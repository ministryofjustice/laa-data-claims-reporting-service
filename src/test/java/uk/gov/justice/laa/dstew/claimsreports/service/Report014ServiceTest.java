package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Report014Service}.
 */
class Report014ServiceTest {

  private Report014Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;
  private Environment environment;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    metricsHandler = mock(MetricsHandler.class);
    environment = mock(Environment.class);

    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new Report014Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock, environment);
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    // when
    service.refreshDataSource();
    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_014");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues(){
    when(environment.acceptsProfiles((Profiles) any())).thenReturn(false);

    service.generateReport();

    verify(creationService).buildCsvFromData(
        eq("SELECT * FROM claims.mvw_report_014"
            + " ORDER BY \"Claim ID\", to_date(\"Amendment Date\", 'DD/MM/YYYY'), to_timestamp(\"Amendment Time\", 'HH24:MI:SS')::time, \"Assessment ID\""),
        any(BufferedWriter.class),
        any()
    );
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/daily/report_014_2025-12-22.csv"));
  }

}