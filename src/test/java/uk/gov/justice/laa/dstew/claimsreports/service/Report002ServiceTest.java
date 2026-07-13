package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.springframework.jdbc.core.JdbcTemplate;

import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Unit tests for {@link Report002Service}.
 */
class Report002ServiceTest {

  private Report002Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    metricsHandler = mock(MetricsHandler.class);

    Instant fixedNow = Instant.parse("2025-12-21T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new Report002Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock);
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    service.refreshDataSource();

    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_002");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues() {
    service.generateReport();

    verify(creationService).buildCsvFromData(
        eq("SELECT * FROM claims.mvw_report_002 "
        + "ORDER BY  to_char(to_date(\"Submission for date\", 'MON-YYYY'), 'YYYYMM') NULLS LAST,"
        + "    \"Office code\","
        + "    \"Category code\","
        + "    \"Procurement area code\","
        + "    \"Access point code\""),
        any(BufferedWriter.class),
        any()
    );
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/monthly/report_002_2025-12-21.csv"));
  }

  @Test
  void returnsTrueOnThe21stOfMonth() {
    Assertions.assertTrue(service.runToday());
  }

  @Test
  void returnsFalseOnThe22ndOfMonth() {
    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    Report002Service serviceOn22nd = new Report002Service(
        jdbcTemplate,
        s3ClientWrapper,
        creationService,
        metricsHandler,
        fixedClock
    );

    Assertions.assertFalse(serviceOn22nd.runToday());
  }
}
