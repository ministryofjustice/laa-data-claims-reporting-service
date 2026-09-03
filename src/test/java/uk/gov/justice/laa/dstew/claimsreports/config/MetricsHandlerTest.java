package uk.gov.justice.laa.dstew.claimsreports.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.core.metrics.Gauge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;

@ExtendWith(MockitoExtension.class)
class MetricsHandlerTest {

  @Mock private PrometheusMeterRegistry reportPrometheusMeterRegistry;

  @Mock private PrometheusMeterRegistry jobPrometheusMeterRegistry;

  @Mock private PrometheusConfiguration.CustomReportGauges customReportGauges;

  @InjectMocks private MetricsHandler metricsHandler;

  @Test
  void shouldResetMetricsIfResetCalled() {
    metricsHandler.resetCustomMetrics();
    verify(customReportGauges).reset();
  }

  @Test
  void shouldSetMetrics() {
    var mockGauge = mock(Gauge.class);
    when(customReportGauges.reportFileSize()).thenReturn(mockGauge);
    metricsHandler.setCustomMetric(CustomMetricId.REPORT_FILE_SIZE, 1234);
    verify(mockGauge).set(1234);
  }
}
