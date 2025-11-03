package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.BufferedWriter;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

/**
 * Unit tests for {@link Report012Service}.
 */
class Report012ServiceTest {

  private Report012Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private FileUploader fileUploader;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    fileUploader = mock(FileUploader.class);
    service = new Report012Service(jdbcTemplate, fileUploader, creationService);
  }

  @Test
  void refreshMaterializedView_ShouldCallRepositoryMethod() {
    // when
    service.refreshMaterializedView();
    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_012");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues(){

    service.generateReport();

    verify(creationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_012"), any(BufferedWriter.class));
    verify(fileUploader).uploadFile(any(File.class), eq("report_012.csv"));
  }

}