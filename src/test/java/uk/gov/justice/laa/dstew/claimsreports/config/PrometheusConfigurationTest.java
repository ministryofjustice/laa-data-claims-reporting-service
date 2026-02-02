package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrometheusConfigurationTest {

  private PrometheusConfiguration prometheusConfiguration;

  @Mock
  private PrometheusMeterRegistry prometheusMeterRegistry;

  @Test
  void shouldCreateCustomMetricsClass() {
    prometheusConfiguration = new PrometheusConfiguration();
    when(prometheusMeterRegistry.getPrometheusRegistry()).thenReturn(mock(PrometheusRegistry.class));
    var customMetrics = prometheusConfiguration.createReportGauges(prometheusMeterRegistry);

    assertNotNull(customMetrics.dataRefreshTimeMs());
    assertNotNull(customMetrics.encodingCheckTimeMs());
    assertNotNull(customMetrics.generatedTimeMs());
    assertNotNull(customMetrics.reportFileSize());
    assertNotNull(customMetrics.reportSuccessful());
    assertNotNull(customMetrics.rowsWritten());
    assertNotNull(customMetrics.uploadTimeMs());
  }

  @Test
  void shouldResetMetricsWhenResetCalled() {
    prometheusConfiguration = new PrometheusConfiguration();
    when(prometheusMeterRegistry.getPrometheusRegistry()).thenReturn(mock(PrometheusRegistry.class));
    var customMetrics = prometheusConfiguration.createReportGauges(prometheusMeterRegistry);

    // Set some test metrics
    customMetrics.dataRefreshTimeMs().set(1);
    customMetrics.encodingCheckTimeMs().set(7);
    customMetrics.generatedTimeMs().set(2);
    customMetrics.reportFileSize().set(3);
    customMetrics.reportSuccessful().set(4);
    customMetrics.rowsWritten().set(5);
    customMetrics.uploadTimeMs().set(6);

    customMetrics.reset();

    assertEquals(0, customMetrics.dataRefreshTimeMs().get());
    assertEquals(0, customMetrics.encodingCheckTimeMs().get());
    assertEquals(0, customMetrics.generatedTimeMs().get());
    assertEquals(0, customMetrics.reportFileSize().get());
    assertEquals(0, customMetrics.reportSuccessful().get());
    assertEquals(0, customMetrics.rowsWritten().get());
    assertEquals(0, customMetrics.uploadTimeMs().get());
  }

}