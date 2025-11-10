package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbstractReportService
 */
class AbstractReportServiceTest {

  // Define a concrete subclass for testing purposes
  static class TestReportService extends AbstractReportService {
    public TestReportService(JdbcTemplate template, S3ClientWrapper s3ClientWrapper, CsvCreationService csvCreationService) {
      super(template, s3ClientWrapper, csvCreationService);
    }

    @Override
    protected String getDataSourceName() {
      return "claims.mvw_report_000";
    }

    @Override
    protected String getRefreshCommand() {
      return "REFRESH MATERIALIZED VIEW claims.mvw_report_000";
    }

    @Override
    protected String getReportName() {
      return "testReport";
    }

    @Override
    protected String getReportFileName() {
      return "test_report.csv";
    }

  }

  private TestReportService service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService csvCreationService;
  private S3ClientWrapper s3ClientWrapper;


  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    csvCreationService = mock(CsvCreationService.class);
    service = new TestReportService(jdbcTemplate, s3ClientWrapper, csvCreationService);
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

  @Test
  void willThrowCsvExceptionWhenCsvServiceThrows() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(csvCreationService)
        .buildCsvFromData(any(), any(), any());
    Assertions.assertThrows(CsvCreationException.class, () -> service.generateReport());

    // And ensure it cleans up after itself
    assertFalse(Files.exists(Path.of("/tmp/test_report.csv")));
  }

  @Test
  void generateReport_shouldCallTheRightServices(){

    service.generateReport();

    verify(csvCreationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_000"), any(BufferedWriter.class), any());
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("test_report.csv"));
  }

  @Test
  void generateReport_shouldDeleteTheTempFileWhenFinished(){

    service.generateReport();

    assertFalse(Files.exists(Path.of("/tmp/test_report.csv")));

  }

}