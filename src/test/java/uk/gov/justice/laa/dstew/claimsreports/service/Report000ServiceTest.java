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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import javax.sql.DataSource;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

/**
 * Unit tests for {@link Report000Service}.
 */
class Report000ServiceTest {

  private Report000Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private FileUploader fileUploader;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    AppConfig appConfig = mock(AppConfig.class);
    creationService = mock(CsvCreationService.class);
    fileUploader = mock(FileUploader.class);
    service = new Report000Service(jdbcTemplate, dataSource, appConfig, fileUploader, creationService);
  }

  @Test
  void refreshMaterializedView_ShouldCallRepositoryMethod() {
    // when
    service.refreshMaterializedView();
    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void willThrowCsvExceptionWhenCsvServiceThrows() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(creationService)
        .buildCsvFromData(any(), any());
    Assertions.assertThrows(CsvCreationException.class, () -> service.generateReport());

    // And ensure it cleans up after itself
    assertFalse(Files.exists(Path.of("/tmp/report_000.csv")));
  }

  @Test
  void generateReport_shouldCallTheRightServices(){

    service.generateReport();

    verify(creationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_000"), any(BufferedWriter.class));
    verify(fileUploader).uploadFile(any(File.class), eq("report_000.csv"));
  }

  @Test
  void generateReport_shouldDeleteTheTempFileWhenFinished(){

    service.generateReport();

    assertFalse(Files.exists(Path.of("/tmp/report_000.csv")));

  }

}