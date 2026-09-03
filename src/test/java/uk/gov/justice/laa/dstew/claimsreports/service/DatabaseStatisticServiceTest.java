package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService.QUERY_ACTIVE_CONNECTIONS;
import static uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService.QUERY_IDLE_CONNECTIONS;
import static uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService.QUERY_MAX_CONNECTIONS;
import static uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService.QUERY_TOTAL_CONNECTIONS;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;

@ExtendWith(MockitoExtension.class)
class DatabaseStatisticServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private MetricsHandler metricsHandler;

  @InjectMocks private DatabaseStatisticService databaseStatisticService;

  @BeforeEach
  @SuppressFBWarnings(
      value = "SECSQLISPRJDBC",
      justification =
          "Mockito stubs match SQL constants passed by production code; no SQL is executed in this unit test.")
  void setUpDatabaseStatisticService() {
    reset(jdbcTemplate, metricsHandler);
    when(jdbcTemplate.queryForObject(anyString(), eq(Double.class))).thenReturn(null);
  }

  @Test
  void should_set_totalConnections_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class)).thenReturn(12.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_TOTAL, 12);
  }

  @Test
  void should_set_idleConnections_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_IDLE_CONNECTIONS, Double.class)).thenReturn(13.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_IDLE, 13);
  }

  @Test
  void should_set_activeConnections_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class)).thenReturn(14.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_ACTIVE, 14);
  }

  @Test
  void should_set_maxConnections_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(15.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_MAX_CONNECTIONS, 15);
  }

  @Test
  void should_set_activeUtilisation_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class)).thenReturn(1.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(100.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_ACTIVE, 0.01);
  }

  @Test
  void should_set_totalUtilisation_when_not_null() {
    when(jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class)).thenReturn(5.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(100.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_TOTAL, 0.05);
  }

  @Test
  void should_zero_totalConnections_when_null() {
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_TOTAL, 0);
  }

  @Test
  void should_zero_idleConnections_when_null() {
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_IDLE, 0);
  }

  @Test
  void should_zero_activeConnections_when_null() {
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_ACTIVE, 0);
  }

  @Test
  void should_zero_maxConnections_when_null() {
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_MAX_CONNECTIONS, 0);
  }

  @Test
  void should_zero_activeUtilisation_when_activeConnections_null() {
    when(jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class)).thenReturn(null);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(100.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_ACTIVE, 0);
  }

  @Test
  void should_zero_activeUtilisation_when_maxConnections_null() {
    when(jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class)).thenReturn(1.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(null);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_ACTIVE, 0);
  }

  @Test
  void should_zero_activeUtilisation_when_maxConnections_zero() {
    when(jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class)).thenReturn(1.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(0.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_ACTIVE, 0);
  }

  @Test
  void should_zero_totalUtilisation_when_totalConnections_null() {
    when(jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class)).thenReturn(null);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(100.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_TOTAL, 0);
  }

  @Test
  void should_zero_totalUtilisation_when_maxConnections_null() {
    when(jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class)).thenReturn(1.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(null);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_TOTAL, 0);
  }

  @Test
  void should_zero_totalUtilisation_when_maxConnections_zero() {
    when(jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class)).thenReturn(1.0);
    when(jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class)).thenReturn(0.0);
    databaseStatisticService.setDatabaseMetrics();
    verify(metricsHandler).setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_TOTAL, 0);
  }
}
