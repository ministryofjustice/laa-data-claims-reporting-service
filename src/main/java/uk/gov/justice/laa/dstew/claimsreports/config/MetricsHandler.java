package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.CustomReportMetric;

/**
 * Pushes prometheus metrics from ephemeral job to pushgateway.
 */
@Component
public class MetricsHandler {

  @Value("${GATEWAY_ADDRESS}")
  private String gatewayAddress;

  private final PrometheusMeterRegistry reportPrometheusMeterRegistry;
  private final PrometheusMeterRegistry jobPrometheusMeterRegistry;
  private final PrometheusConfiguration.CustomReportGauges customReportGauges;

  /**
   * Creates a Metrics Handler.
   *
   * @param reportPrometheusMeterRegistry registry for report metrics
   * @param jobPrometheusMeterRegistry registry for job metrics
   * @param customReportGauges custom metric gauges for reports
   */
  public MetricsHandler(PrometheusMeterRegistry reportPrometheusMeterRegistry,
                        PrometheusMeterRegistry jobPrometheusMeterRegistry,
                        PrometheusConfiguration.CustomReportGauges customReportGauges) {
    this.reportPrometheusMeterRegistry = reportPrometheusMeterRegistry;
    this.jobPrometheusMeterRegistry = jobPrometheusMeterRegistry;
    this.customReportGauges = customReportGauges;
  }

  public void resetCustomMetrics() {
    customReportGauges.reset();
  }

  /**
   * Set a custom metrics value. This helps isolate metric setting from the actual code calling it.
   *
   * @param metric metric to set from the {@link CustomReportMetric} enum
   * @param value value to set metric to
   */
  public void setCustomMetric(CustomReportMetric metric, long value) {
    switch (metric) {
      case ROWS_WRITTEN -> customReportGauges.rowsWritten().set(value);
      case REPORT_FILE_SIZE -> customReportGauges.reportFileSize().set(value);
      case REPORT_SUCCESSFUL -> customReportGauges.reportSuccessful().set(value);
      case DATA_REFRESH_TIME_MS -> customReportGauges.dataRefreshTimeMs().set(value);
      case UPLOAD_TIME_MS -> customReportGauges.uploadTimeMs().set(value);
      case GENERATED_TIME_MS -> customReportGauges.generatedTimeMs().set(value);
      default -> throw new EnumConstantNotPresentException(CustomReportMetric.class, metric.name());
    }
  }

  /**
   * Allow dynamic pushing of metrics.
   */
  public void pushReportMetrics(String reportName) {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job(reportName)
          .registry(reportPrometheusMeterRegistry.getPrometheusRegistry()).build()
          .push();
    } catch (Exception e) {
      System.err.println("Failed to push metrics: " + e.getMessage());
    }
  }

  /**
   * When the job is complete, send metrics to pushgateway.
   */
  @PreDestroy
  public void pushEndOfJobMetrics() {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job("jobEnd")
          .registry(jobPrometheusMeterRegistry.getPrometheusRegistry()).build()
          .push();
    } catch (Exception e) {
      System.err.println("Failed to push metrics: " + e.getMessage());
    }
  }
}
