package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;


/**
 * Calculate and set metrics pertaining to database performance.
 */
@Service
@RequiredArgsConstructor
public class DatabaseStatisticService {

  protected static final String QUERY_TOTAL_CONNECTIONS = "SELECT count(*) FROM pg_stat_activity";
  public static final String QUERY_ACTIVE_CONNECTIONS = "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'";
  public static final String QUERY_IDLE_CONNECTIONS = "SELECT count(*) FROM pg_stat_activity WHERE state = 'idle'";
  public static final String QUERY_MAX_CONNECTIONS = "SHOW max_connections;";
  private final JdbcTemplate jdbcTemplate;
  private final MetricsHandler metricsHandler;

  /**
   * Get database statistics and set them as metrics.
   */
  public void setDatabaseMetrics() {
    var totalConnections = jdbcTemplate.queryForObject(QUERY_TOTAL_CONNECTIONS, Double.class);
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_TOTAL, totalConnections != null ? totalConnections : 0);

    var activeConnections = jdbcTemplate.queryForObject(QUERY_ACTIVE_CONNECTIONS, Double.class);
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_ACTIVE, activeConnections != null ? activeConnections : 0);

    var idleConnections = jdbcTemplate.queryForObject(QUERY_IDLE_CONNECTIONS, Double.class);
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_IDLE, idleConnections != null ? idleConnections : 0);

    var maxConnections = jdbcTemplate.queryForObject(QUERY_MAX_CONNECTIONS, Double.class);
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_MAX_CONNECTIONS, maxConnections != null ? maxConnections : 0);

    var activeUtilisation = (maxConnections == null || activeConnections == null || maxConnections == 0) ? 0 : activeConnections / maxConnections;
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_ACTIVE, activeUtilisation);

    var totalUtilisation = (maxConnections == null || totalConnections == null || maxConnections == 0) ? 0 : totalConnections / maxConnections;
    metricsHandler.setCustomMetric(CustomMetricId.DB_CONNECTIONS_UTILISATION_TOTAL, totalUtilisation);

  }
}
