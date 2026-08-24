package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbstractReportService
 */
class AbstractReportServiceTest {

  private static final String TEMP_REPORT_PREFIX = "test_report_2025-12-21_";

  private TestReportService service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService csvCreationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;

  @BeforeEach
  @SneakyThrows
  void setUpAbstractReportService() {
    deleteTempReportFiles();
    jdbcTemplate = mock(JdbcTemplate.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    csvCreationService = mock(CsvCreationService.class);
    metricsHandler = mock(MetricsHandler.class);
    Instant fixedNow = Instant.parse("2025-12-21T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new TestReportService(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, true, fixedClock);
  }

  @Test
  void refreshDataSourceShouldInvokeRepositoryAndLog() {
    // when
    service.refreshDataSource();

    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void refreshDataSource_ShouldHandleMultipleInvocations() {
    service.refreshDataSource();
    service.refreshDataSource();
    verify(jdbcTemplate, times(2))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @SneakyThrows
  @Test
  void willThrowCsvExceptionWhenCsvServiceThrows() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(csvCreationService)
        .buildCsvFromData(any(), any(), any());
    Assertions.assertThrows(CsvCreationException.class, () -> service.generateReport());

    // And ensure it cleans up after itself
    assertNoTempReportFiles();
  }

  @Test
  void generateReport_shouldCallTheRightServices(){
    service.generateReport();

    verify(csvCreationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_000 ORDER BY  test_order_by_column"),
        any(BufferedWriter.class), any());
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/daily/test_report_2025-12-21.csv"), eq(List.of()), isNull());
  }

  @SneakyThrows
  @Test
  void generateReport_shouldDeleteTheTempFileWhenFinished(){
    service.generateReport();
    assertNoTempReportFiles();
  }

  @SneakyThrows
  private void assertNoTempReportFiles() {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (var stream = Files.list(tempDir)) {
      assertFalse(stream.anyMatch(file -> {
        var fileName = file.getFileName();
        return fileName != null && fileName.toString().startsWith(TEMP_REPORT_PREFIX) && fileName.toString().endsWith(".csv");
      }));
    }
  }

  @SneakyThrows
  private void deleteTempReportFiles() {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (var stream = Files.list(tempDir)) {
      stream
          .filter(file -> {
            var fileName = file.getFileName();
            return fileName != null && fileName.toString().startsWith(TEMP_REPORT_PREFIX) && fileName.toString().endsWith(".csv");
          })
          .forEach(file -> file.toFile().delete());
    }
  }

  @Test
  void willNotGenerateReportIfReportNotScheduledToRun() {
    TestReportService service = new TestReportService(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, false, fixedClock);

    service.generateReport();
    verify(csvCreationService, times(0)).buildCsvFromData(any(), any(), any());
  }

}