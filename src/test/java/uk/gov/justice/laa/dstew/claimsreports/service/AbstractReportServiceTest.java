package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import javax.sql.DataSource;

/**
 * Unit tests for AbstractReportService
 */
class AbstractReportServiceTest {

  // Define a concrete subclass for testing purposes
  static class TestReportService extends AbstractReportService {
    public TestReportService(JdbcTemplate template, FileUploader fileUploader, CsvCreationService csvCreationService) {
      super(template, fileUploader, csvCreationService);
    }

    @Override
    protected String getMaterializedViewName() {
      return "claims.mvw_report_000";
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
  private FileUploader fileUploader;


  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    fileUploader = mock(FileUploader.class);
    csvCreationService = mock(CsvCreationService.class);
    service = new TestReportService(jdbcTemplate, fileUploader, csvCreationService);
  }

  @Test
  void refreshMaterializedViewShouldInvokeRepositoryAndLog() {
    // when
    service.refreshMaterializedView();

    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void refreshMaterializedView_ShouldHandleMultipleInvocations() {
    service.refreshMaterializedView();
    service.refreshMaterializedView();
    verify(jdbcTemplate, times(2))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void willThrowCsvExceptionWhenCsvServiceThrows() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(csvCreationService)
        .buildCsvFromData(any(), any());
    Assertions.assertThrows(CsvCreationException.class, () -> service.generateReport());

    // And ensure it cleans up after itself
    assertFalse(Files.exists(Path.of("/tmp/test_report.csv")));
  }

  @Test
  void generateReport_shouldCallTheRightServices(){

    service.generateReport();

    verify(csvCreationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_000"), any(BufferedWriter.class));
    verify(fileUploader).uploadFile(any(File.class), eq("test_report.csv"));
  }

  @Test
  void generateReport_shouldDeleteTheTempFileWhenFinished(){

    service.generateReport();

    assertFalse(Files.exists(Path.of("/tmp/test_report.csv")));

  }

}