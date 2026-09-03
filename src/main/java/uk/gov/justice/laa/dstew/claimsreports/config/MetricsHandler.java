package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;

/** Pushes prometheus metrics from ephemeral job to pushgateway. */
@Component
@Slf4j
public class MetricsHandler {

  @Value("${GATEWAY_ADDRESS}")
  private String gatewayAddress;

  private final PrometheusMeterRegistry reportPrometheusMeterRegistry;
  private final PrometheusMeterRegistry jobPrometheusMeterRegistry;
  private final PrometheusMeterRegistry replicationHealthPrometheusMeterRegistry;
  private final PrometheusMeterRegistry databaseHealthPrometheusMeterRegistry;
  private final PrometheusConfiguration.CustomReportGauges customReportGauges;
  private final PrometheusConfiguration.ReplicationHealthGauge replicationHealthCheckGauge;
  private final PrometheusConfiguration.DatabaseHealthGauge databaseHealthGauge;

  /**
   * Creates a Metrics Handler.
   *
   * @param reportPrometheusMeterRegistry registry for report metrics
   * @param jobPrometheusMeterRegistry registry for job metrics
   * @param replicationHealthPrometheusMeterRegistry registry for replication health check metric
   * @param customReportGauges custom metric gauges for reports
   * @param replicationHealthCheckGauge gauge for replication health check status
   */
  public MetricsHandler(
      PrometheusMeterRegistry reportPrometheusMeterRegistry,
      PrometheusMeterRegistry jobPrometheusMeterRegistry,
      PrometheusMeterRegistry replicationHealthPrometheusMeterRegistry,
      PrometheusMeterRegistry databaseHealthPrometheusMeterRegistry,
      PrometheusConfiguration.CustomReportGauges customReportGauges,
      PrometheusConfiguration.ReplicationHealthGauge replicationHealthCheckGauge,
      PrometheusConfiguration.DatabaseHealthGauge databaseHealthGauge) {
    this.reportPrometheusMeterRegistry = reportPrometheusMeterRegistry;
    this.jobPrometheusMeterRegistry = jobPrometheusMeterRegistry;
    this.replicationHealthPrometheusMeterRegistry = replicationHealthPrometheusMeterRegistry;
    this.databaseHealthPrometheusMeterRegistry = databaseHealthPrometheusMeterRegistry;
    this.customReportGauges = customReportGauges;
    this.replicationHealthCheckGauge = replicationHealthCheckGauge;
    this.databaseHealthGauge = databaseHealthGauge;
  }

  public void resetCustomMetrics() {
    customReportGauges.reset();
  }

  /**
   * Set a custom metrics value. This helps isolate metric setting from the actual code calling it.
   *
   * @param metric metric to set from the {@link CustomMetricId} enum
   * @param value value to set metric to
   */
  public void setCustomMetric(CustomMetricId metric, double value) {
    switch (metric) {
      case ROWS_WRITTEN -> customReportGauges.rowsWritten().set(value);
      case REPORT_TOTAL_TIME_MS -> customReportGauges.reportTotalTime().set(value);
      case REPORT_FILE_SIZE -> customReportGauges.reportFileSize().set(value);
      case REPORT_SUCCESSFUL -> customReportGauges.reportSuccessful().set(value);
      case DATA_REFRESH_TIME_MS -> customReportGauges.dataRefreshTimeMs().set(value);
      case UPLOAD_TIME_MS -> customReportGauges.uploadTimeMs().set(value);
      case GENERATED_TIME_MS -> customReportGauges.generatedTimeMs().set(value);
      case ENCODING_CHECK_TIME_MS -> customReportGauges.encodingCheckTimeMs().set(value);
      case REPLICATION_HEALTH_CHECK_STATUS ->
          replicationHealthCheckGauge.replicationHealthCheck().set(value);
      case DB_CONNECTIONS_TOTAL -> databaseHealthGauge.totalConnections().set(value);
      case DB_CONNECTIONS_ACTIVE -> databaseHealthGauge.activeConnections().set(value);
      case DB_CONNECTIONS_IDLE -> databaseHealthGauge.idleConnections().set(value);
      case DB_CONNECTIONS_MAX_CONNECTIONS -> databaseHealthGauge.maxConnections().set(value);
      case DB_CONNECTIONS_UTILISATION_ACTIVE -> databaseHealthGauge.activeUtilisation().set(value);
      case DB_CONNECTIONS_UTILISATION_TOTAL -> databaseHealthGauge.totalUtilisation().set(value);
      default -> throw new EnumConstantNotPresentException(CustomMetricId.class, metric.name());
    }
  }

  /** Allow dynamic pushing of metrics. */
  public void pushReportMetrics(String reportName) {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job(reportName)
          .registry(reportPrometheusMeterRegistry.getPrometheusRegistry())
          .build()
          .push();
    } catch (Exception e) {
      log.error("Failed to push report metrics", e);
    }
  }

  /** When the job is complete, send metrics to pushgateway. */
  @PreDestroy
  public void pushEndOfJobMetrics() {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job("jobEnd")
          .registry(jobPrometheusMeterRegistry.getPrometheusRegistry())
          .build()
          .push();
    } catch (Exception e) {
      log.error("Failed to push end of job metrics", e);
    }
  }

  /** Push replication health metrics. */
  public void pushReplicationHealthMetric() {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job("replicationHealth")
          .registry(replicationHealthPrometheusMeterRegistry.getPrometheusRegistry())
          .build()
          .push();
    } catch (Exception e) {
      log.error("Failed to push replication health metrics", e);
    }
  }

  /** Push replication health metrics. */
  public void pushDatabaseHealthMetrics() {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job("databaseHealth")
          .registry(databaseHealthPrometheusMeterRegistry.getPrometheusRegistry())
          .build()
          .push();
    } catch (Exception e) {
      log.error("Failed to push database metrics", e);
    }
  }
}
