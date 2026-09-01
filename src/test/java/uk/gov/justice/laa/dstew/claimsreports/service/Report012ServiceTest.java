package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Unit tests for {@link Report012Service}.
 */
class Report012ServiceTest {

  private static final String TEMP_REPORT_PREFIX = "report_012_2025-12-22_";

  private Report012Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private ExcelCreationService excelCreationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;
  private AppConfig appConfig;

  @BeforeEach
  void setUpReport012Service() {
    deleteTempXlsxFiles();
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    excelCreationService = mock(ExcelCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    metricsHandler = mock(MetricsHandler.class);
    appConfig = mock(AppConfig.class);

    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new Report012Service(
        jdbcTemplate,
        s3ClientWrapper,
        creationService,
        metricsHandler,
        fixedClock,
        appConfig,
        excelCreationService
    );
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    service.refreshDataSource();
    verify(jdbcTemplate, times(1)).execute("REFRESH MATERIALIZED VIEW claims.mvw_report_012");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldUseCsvPathWhenFeatureFlagDisabled() {
    when(appConfig.isEnableRep012Xlsx()).thenReturn(false);

    service.generateReport();

    verify(creationService).buildCsvFromData(
        eq("SELECT * FROM claims.mvw_report_012 "
            + "ORDER BY  \"Provider office account number\","
            + "    to_char(to_date(\"Submission month\", 'MON-YYYY'), 'YYYYMM'),"
            + "    \"Area of law\""),
        any(BufferedWriter.class),
        any()
    );
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/daily/report_012_2025-12-22.csv"), eq(List.of(
        "Provider office account number", "Submission month", "Area of law",
        "Original submission value", "Date submission was uploaded")), isNull());
  }

  @Test
  void generateReport_shouldUseXlsxPathWhenFeatureFlagEnabled() {
    when(appConfig.isEnableRep012Xlsx()).thenReturn(true);

    service.generateReport();

    verify(excelCreationService).buildExcelFromData(
        eq("SELECT * FROM claims.mvw_report_012 "
            + "ORDER BY  \"Provider office account number\","
            + "    to_char(to_date(\"Submission month\", 'MON-YYYY'), 'YYYYMM'),"
            + "    \"Area of law\""),
        any(File.class),
        eq("REPORT012")
    );
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/daily/report_012_2025-12-22.xlsx"),
        eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Test
  void generateReport_shouldDeleteTempXlsxWhenExcelCreationFails() {
    when(appConfig.isEnableRep012Xlsx()).thenReturn(true);
    doThrow(new CsvCreationException("Simulated excel failure"))
        .when(excelCreationService)
        .buildExcelFromData(any(), any(), any());

    assertThrows(CsvCreationException.class, () -> service.generateReport());

    assertNoTempXlsxFiles();
  }

  private void assertNoTempXlsxFiles() {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (var stream = Files.list(tempDir)) {
      assertFalse(stream.anyMatch(file -> {
        var fileName = file.getFileName();
        return fileName != null
            && fileName.toString().startsWith(TEMP_REPORT_PREFIX)
            && fileName.toString().endsWith(".xlsx");
      }));
    } catch (IOException e) {
      throw new RuntimeException("Unable to inspect temp files", e);
    }
  }

  private void deleteTempXlsxFiles() {
    Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
    try (var stream = Files.list(tempDir)) {
      stream
          .filter(file -> {
            var fileName = file.getFileName();
            return fileName != null
                && fileName.toString().startsWith(TEMP_REPORT_PREFIX)
                && fileName.toString().endsWith(".xlsx");
          })
          .forEach(file -> {
            try {
              Files.deleteIfExists(file);
            } catch (IOException e) {
              throw new RuntimeException("Failed to delete temp report file: " + file, e);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException("Unable to clean temp files", e);
    }
  }
}
