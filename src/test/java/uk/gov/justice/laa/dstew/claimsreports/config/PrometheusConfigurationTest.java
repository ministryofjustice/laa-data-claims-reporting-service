package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = PrometheusConfiguration.class)
class PrometheusConfigurationTest {

  @Autowired
  PrometheusConfiguration prometheusConfiguration;

  @Test
  void shouldLoadSpringBeans() {
    assertNotNull(prometheusConfiguration);
  }

  @Test
  void shouldCreateReportGauges() {
    PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    var gauges = prometheusConfiguration.createReportGauges(registry);

    assertAll(
            () -> assertNotNull(gauges.reportSuccessful()),
            () -> assertNotNull(gauges.reportTotalTime()),
            () -> assertNotNull(gauges.dataRefreshTimeMs()),
            () -> assertNotNull(gauges.generatedTimeMs()),
            () -> assertNotNull(gauges.rowsWritten()),
            () -> assertNotNull(gauges.reportFileSize()),
            () -> assertNotNull(gauges.uploadTimeMs()),
            () -> assertNotNull(gauges.encodingCheckTimeMs())
    );
  }

  @Test
  void shouldResetMetricsToZero() {
    PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    var gauges = prometheusConfiguration.createReportGauges(registry);

    gauges.reportSuccessful().set(1);
    gauges.reportTotalTime().set(100);
    gauges.dataRefreshTimeMs().set(2);
    gauges.generatedTimeMs().set(3);
    gauges.rowsWritten().set(4);
    gauges.reportFileSize().set(5);
    gauges.uploadTimeMs().set(6);
    gauges.encodingCheckTimeMs().set(7);

    gauges.reset();

    assertAll(
            () -> assertEquals(0, gauges.reportSuccessful().get()),
            () -> assertEquals(0, gauges.reportTotalTime().get()),
            () -> assertEquals(0, gauges.dataRefreshTimeMs().get()),
            () -> assertEquals(0, gauges.generatedTimeMs().get()),
            () -> assertEquals(0, gauges.rowsWritten().get()),
            () -> assertEquals(0, gauges.reportFileSize().get()),
            () -> assertEquals(0, gauges.uploadTimeMs().get()),
            () -> assertEquals(0, gauges.encodingCheckTimeMs().get())
    );
  }

  @Test
  void shouldCreateReplicationHealthGauge() {
    PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    var gauge = prometheusConfiguration
            .replicationHealthGauge(registry);

    assertNotNull(gauge);
    assertNotNull(gauge.replicationHealthCheck());

    gauge.replicationHealthCheck().set(1);
    assertEquals(1, gauge.replicationHealthCheck().get());
  }

  @Test
  void shouldDenyReplicationHealthCheckMetricInReportRegistry() {
    PrometheusMeterRegistry registry =
            prometheusConfiguration.reportPrometheusMeterRegistry();

    // try to register metric that should be denied
    registry.gauge("replication_health_check_status", 1);

    String output = registry.scrape();

    assertFalse(
            output.contains("replication_health_check_status"),
            "Metric should be filtered out by MeterFilter"
    );
  }

  @Test
  void shouldAllowOtherMetricsInReportRegistry() {
    PrometheusMeterRegistry registry =
            prometheusConfiguration.reportPrometheusMeterRegistry();

    var gauges = prometheusConfiguration.createReportGauges(registry);

    gauges.reportSuccessful().set(1);

    String output = registry.scrape();

    assertTrue(output.contains("report_success"));
  }

  @Test
  void shouldCreateJobRegistry() {
    var registry = prometheusConfiguration.jobPrometheusMeterRegistry();
    assertNotNull(registry);
  }

  @Test
  void shouldCreateReplicationRegistry() {
    var registry = prometheusConfiguration.replicationHealthPrometheusMeterRegistry();
    assertNotNull(registry);
  }

  @Test
  void shouldContainExpectedMetrics() {
    var values = PrometheusConfiguration.CustomReportGauges.CustomReportMetric.values();

    assertTrue(
            java.util.Arrays.asList(values)
                    .contains(PrometheusConfiguration.CustomReportGauges.CustomReportMetric.REPORT_SUCCESSFUL)
    );
  }

  @Test
  void resetShouldBeIdempotent() {
    PrometheusMeterRegistry registry =
            new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    var gauges = prometheusConfiguration.createReportGauges(registry);

    gauges.reportTotalTime().set(50);

    gauges.reset();
    gauges.reset();

    assertEquals(0, gauges.reportTotalTime().get());
  }
}