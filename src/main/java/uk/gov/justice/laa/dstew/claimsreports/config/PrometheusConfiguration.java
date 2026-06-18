package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusRenameFilter;
import io.prometheus.metrics.core.metrics.Gauge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus Registry allowing for integration with pushgateway.
 */
@Configuration
public class PrometheusConfiguration {

  /**
   * This registry is used for the report-level metric push, and so also contains report specific things like success status.
   *
   * @return registry for report metrics
   */
  @Bean
  public PrometheusMeterRegistry reportPrometheusMeterRegistry() {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    registry.config().meterFilter(MeterFilter.deny(id ->
            id.getName().equals("replication_health_check_status")));
    return registry;
  }

  /**
   * This registry is used for the job-level metric push at the end of the process.
   *
   * @return registry for job-level metrics
   */
  @Bean
  public PrometheusMeterRegistry jobPrometheusMeterRegistry() {
    PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    registry.config().meterFilter(MeterFilter.deny(id ->
            id.getName().equals("replication_health_check_status")));
    return registry;
  }

  /**
   * This registry is used to isolate replication health metric pushes at the end of the process instead of bundling
   * with job registry.
   *
   * @return registry for (replication health)-level metrics
   */
  @Bean
  public PrometheusMeterRegistry replicationHealthPrometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  /*
    This updates the help message associated with micrometers messages, so that we don't get 3 infos a second clogging up the logs
   */
  @Bean
  MeterFilter fixInfoLogsAboutStartTimeMessage() {
    return new PrometheusRenameFilter();
  }

  /**
   * Create a gauge for the replication health check status, registered on the replication health registry.
   * Kept separate from report and job gauges as it is not a per-report metric and we don't want duplicate
   * job registry metric updates.
   *
   * @param replicationHealthPrometheusMeterRegistry replication health registry
   * @return replication health check gauge
   */
  @Bean
  public ReplicationHealthGauge replicationHealthGauge(
          PrometheusMeterRegistry replicationHealthPrometheusMeterRegistry) {
    var replicationHealthCheck = Gauge.builder()
            .withoutExemplars()
            .name("replication_health_check_status")
            .help("1 when replication health check passes, 0 when it fails")
            .register(replicationHealthPrometheusMeterRegistry.getPrometheusRegistry());
    return new ReplicationHealthGauge(replicationHealthCheck);
  }

  /**
   * Identifier for a custom metric.
   */
  public enum CustomMetricId {
    REPORT_SUCCESSFUL,
    REPORT_TOTAL_TIME_MS,
    DATA_REFRESH_TIME_MS,
    GENERATED_TIME_MS,
    ROWS_WRITTEN,
    REPORT_FILE_SIZE,
    UPLOAD_TIME_MS,
    ENCODING_CHECK_TIME_MS,
    REPLICATION_HEALTH_CHECK_STATUS,
    DB_CONNECTIONS_TOTAL,
    DB_CONNECTIONS_ACTIVE,
    DB_CONNECTIONS_IDLE,
    DB_CONNECTIONS_MAX_CONNECTIONS,
    DB_CONNECTIONS_UTILISATION_ACTIVE,
    DB_CONNECTIONS_UTILISATION_TOTAL
  }

  /**
   * This class defines the replication health metric we push to Prometheus.
   */
  public record ReplicationHealthGauge(Gauge replicationHealthCheck) {}

  /**
   * This registry is used to isolate replication health metric pushes at the end of the process instead of bundling
   * with job registry.
   *
   * @return registry for (replication health)-level metrics
   */
  @Bean
  public PrometheusMeterRegistry databaseHealthPrometheusMeterRegistry() {
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  /**
   * Create a gauge for the database health checks, registered on the database health registry.
   * These are collected at the end of the process
   *
   * @param databaseHealthPrometheusMeterRegistry database health registry
   * @return database health check gauge
   */
  @Bean
  public DatabaseHealthGauge databaseHealthGauge(
      PrometheusMeterRegistry databaseHealthPrometheusMeterRegistry) {
    var totalConnections = Gauge.builder()
        .withoutExemplars()
        .name("database_connectons_total_open")
        .help("Total open connections to the database at the end of the cronjob")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    var activeConnections = Gauge.builder()
        .withoutExemplars()
        .name("database_connections_active")
        .help("Active connections at the end of the cronjob")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    var idleConnections = Gauge.builder()
        .withoutExemplars()
        .name("database_connections_idle")
        .help("Idle connections at the end of the cronjob")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    var maxConnections = Gauge.builder()
        .withoutExemplars()
        .name("database_connections_max")
        .help("Max connections")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    var activeUtilisation = Gauge.builder()
        .withoutExemplars()
        .name("database_connections_active_utilisation")
        .help("Active utilisation rate (active/max)")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    var totalUtilisation = Gauge.builder()
        .withoutExemplars()
        .name("database_connections_total_utilisation")
        .help("Total utilisation rate (total/max)")
        .register(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry());

    return new DatabaseHealthGauge(totalConnections, activeConnections, idleConnections, maxConnections,
        activeUtilisation, totalUtilisation);
  }

  /**
   * This class defines the replication health metric we push to Prometheus.
   */
  public record DatabaseHealthGauge(Gauge totalConnections, Gauge activeConnections, Gauge idleConnections,
                                    Gauge maxConnections, Gauge activeUtilisation, Gauge totalUtilisation) {}

  /**
   * Create a set of custom gauges for measuring report stats.
   *
   * @param reportPrometheusMeterRegistry report registry
   * @return custom gauge objects
   */
  @Bean
  public CustomReportGauges createReportGauges(PrometheusMeterRegistry reportPrometheusMeterRegistry) {
    var registry = reportPrometheusMeterRegistry.getPrometheusRegistry();

    var reportSuccess = Gauge.builder()
        .withoutExemplars()
        .name("report_success")
        .help("1 on success, -1 on failure, 0 if skipped")
        .register(registry);

    var reportTotalTimeMs = Gauge.builder()
        .withoutExemplars()
        .name("report_total_time_ms")
        .help("How long the report generation took in total")
        .register(registry);

    var dataRefreshTimeMs = Gauge.builder()
        .withoutExemplars()
        .name("report_data_refresh_duration_ms")
        .help("How long the data source refresh took in milliseconds")
        .register(registry);

    var generatedTimeMs = Gauge.builder()
        .withoutExemplars()
        .name("report_generation_duration_ms")
        .help("How long the report generation took in milliseconds")
        .register(registry);

    var rowsWritten = Gauge.builder()
        .withoutExemplars()
        .name("report_rows_written")
        .help("How many rows were written into the report")
        .register(registry);

    var reportFileSize = Gauge.builder()
        .withoutExemplars()
        .name("report_file_size_mib")
        .help("Size of report uploaded to S3 in MiB")
        .register(registry);

    var uploadTimeMs = Gauge.builder()
        .withoutExemplars()
        .name("report_upload_time_ms")
        .help("How long report upload to S3 took in milliseconds")
        .register(registry);

    var encodingCheckTimeMs = Gauge.builder()
        .withoutExemplars()
        .name("report_encoding_check_time_ms")
        .help("Time taken to verify that the generated file is UTF-8 encoded (ms)")
        .register(registry);

    return new CustomReportGauges(reportSuccess, reportTotalTimeMs, dataRefreshTimeMs, generatedTimeMs, rowsWritten, reportFileSize,
        uploadTimeMs, encodingCheckTimeMs);
  }

  /**
   * This class defines some custom metrics we push to Prometheus.
   */
  public record CustomReportGauges(Gauge reportSuccessful, Gauge reportTotalTime, Gauge dataRefreshTimeMs, Gauge generatedTimeMs,
                                   Gauge rowsWritten, Gauge reportFileSize, Gauge uploadTimeMs, Gauge encodingCheckTimeMs) {

    public static int REPORT_FAILED = -1;
    public static int REPORT_SUCCESSFUL = 1;
    public static int REPORT_SKIPPED = 0;

    /**
     * Reset the metrics.
     */
    public void reset() {
      reportSuccessful.set(0);
      reportTotalTime.set(0);
      dataRefreshTimeMs.set(0);
      generatedTimeMs.set(0);
      rowsWritten.set(0);
      reportFileSize.set(0);
      uploadTimeMs.set(0);
      encodingCheckTimeMs.set(0);
    }
  }
}
