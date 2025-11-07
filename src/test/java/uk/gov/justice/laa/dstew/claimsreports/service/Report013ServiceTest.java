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
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Unit tests for {@link Report013Service}.
 */
class Report013ServiceTest {

  private Report013Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    service = new Report013Service(jdbcTemplate, s3ClientWrapper, creationService);
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    // when
    service.refreshDataSource();
    // then
    verify(jdbcTemplate, times(1))
        .execute("SELECT claims.refresh_report013()");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues(){

    service.generateReport();

    verify(creationService).buildCsvFromData(eq("SELECT * FROM claims.report_013"), any(BufferedWriter.class));
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("report_013.csv"));
  }

}