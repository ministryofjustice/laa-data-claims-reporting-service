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
    return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
  }

  /**
   * This registry is used for the job-level metric push at the end of the process.
   *
   * @return registry for job-level metrics
   */
  @Bean
  public PrometheusMeterRegistry jobPrometheusMeterRegistry() {
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
        .help("1 on success, 0 on failure, -1 on skipped")
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

    return new CustomReportGauges(reportSuccess, dataRefreshTimeMs, generatedTimeMs, rowsWritten, reportFileSize, uploadTimeMs);
  }

  /**
   * This class defines some custom metrics we push to Prometheus.
   */
  public record CustomReportGauges(Gauge reportSuccessful, Gauge dataRefreshTimeMs, Gauge generatedTimeMs,
                                   Gauge rowsWritten, Gauge reportFileSize, Gauge uploadTimeMs) {

    public static int REPORT_FAILED = 0;
    public static int REPORT_SUCCESSFUL = 1;
    public static int REPORT_SKIPPED = -1;

    /**
     * Identifier for a custom metric.
     */
    public enum CustomReportMetric {
      REPORT_SUCCESSFUL,
      DATA_REFRESH_TIME_MS,
      GENERATED_TIME_MS,
      ROWS_WRITTEN,
      REPORT_FILE_SIZE,
      UPLOAD_TIME_MS
    }

    /**
     * Reset the metrics.
     */
    public void reset() {
      reportSuccessful.set(0);
      dataRefreshTimeMs.set(0);
      generatedTimeMs.set(0);
      rowsWritten.set(0);
      reportFileSize.set(0);
      uploadTimeMs.set(0);
    }
  }
}
