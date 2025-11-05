package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.*;

import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Unit tests for {@link Report000Service}.
 */
class Report000ServiceTest {

  private Report000Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    service = new Report000Service(jdbcTemplate, s3ClientWrapper, creationService);
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
  void generateReport_shouldCallTheRightServicesWithTheRightValues(){

    service.generateReport();

    verify(creationService).buildCsvFromData(eq("SELECT * FROM claims.mvw_report_000"), any(BufferedWriter.class));
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("report_000.csv"));
  }

}