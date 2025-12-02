package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pushes prometheus metrics from ephemeral job to pushgateway.
 */
@Component

public class MetricsHandler {

  @Value("${GATEWAY_ADDRESS}")
  private String gatewayAddress;

  @Autowired
  private PrometheusMeterRegistry prometheusMeterRegistry;

  /**
   * Allow dynamic pushing of metrics.
   */
  public void pushMetrics(String reportName) {
    try {
      PushGateway.builder()
          .address(gatewayAddress)
          .job(reportName)
          .registry(prometheusMeterRegistry.getPrometheusRegistry()).build()
          .push();
      System.out.println("**************** " + gatewayAddress);
    } catch (Exception e) {
      System.err.println("Failed to push metrics: " + e.getMessage());
    }
  }

  /**
   * When the job is complete, send metrics to pushgateway.
   */
  @PreDestroy
  public void pushEndOfJobMetrics() {
    pushMetrics("JobEnd");
  }
}
