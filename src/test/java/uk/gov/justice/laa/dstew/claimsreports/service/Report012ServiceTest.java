package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.BufferedWriter;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Unit tests for {@link Report012Service}.
 */
class Report012ServiceTest {

  private Report012Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;

  @BeforeEach
  void setUpReport012Service() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    metricsHandler = mock(MetricsHandler.class);

    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new Report012Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock);
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    // when
    service.refreshDataSource();
    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_012");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues(){

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

}