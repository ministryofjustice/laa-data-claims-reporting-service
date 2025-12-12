package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
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
 * Unit tests for {@link Report000Service}.
 */
@ExtendWith(MockitoExtension.class)
class Report000ServiceTest {

  private Report000Service service;
  private JdbcTemplate jdbcTemplate;
  private CsvCreationService creationService;
  private S3ClientWrapper s3ClientWrapper;
  private MetricsHandler metricsHandler;
  private Clock fixedClock;
  private AppConfig appConfig;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    creationService = mock(CsvCreationService.class);
    s3ClientWrapper = mock(S3ClientWrapper.class);
    metricsHandler = mock(MetricsHandler.class);
    appConfig = mock(AppConfig.class);

    Instant fixedNow = Instant.parse("2025-12-21T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    service = new Report000Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock, appConfig);
  }

  @Test
  void refreshDataSource_ShouldCallRepositoryMethod() {
    // when
    service.refreshDataSource();
    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void generateReport_shouldCallTheRightServicesWithTheRightValues() {
    when(appConfig.isForceRunReport000()).thenReturn(false);

    service.generateReport();

    verify(creationService).buildCsvFromData(
        eq("SELECT * FROM claims.mvw_report_000 "
            + "ORDER BY  to_char(to_date(\"Submission Period\", 'MON-YYYY'), 'YYYYMM') NULLS LAST,"
            + "    \"Office Account Number\","
            + "    \"Line Number\""),
        any(BufferedWriter.class),
        any()
    );
    verify(s3ClientWrapper).uploadFile(any(File.class), eq("reports/monthly/report_000_2025-12-21.csv"));
  }

  @Test
  void returnsTrueOnThe21stOfMonth() {
    when(appConfig.isForceRunReport000()).thenReturn(false);

    Assertions.assertTrue(service.runToday());
  }

  @Test
  void returnsFalseOnThe22ndOfMonth() {
    when(appConfig.isForceRunReport000()).thenReturn(false);

    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);
    Report000Service service = new Report000Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock, appConfig);

    Assertions.assertFalse(service.runToday());
  }

  @Test
  void returnsTrueOnThe22ndOfMonthIfOverrideSet() {

    when(appConfig.isForceRunReport000()).thenReturn(true);

    Instant fixedNow = Instant.parse("2025-12-22T10:00:00Z");
    fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    Report000Service service = new Report000Service(jdbcTemplate, s3ClientWrapper, creationService, metricsHandler, fixedClock, appConfig);

    Assertions.assertTrue(service.runToday());
  }

}