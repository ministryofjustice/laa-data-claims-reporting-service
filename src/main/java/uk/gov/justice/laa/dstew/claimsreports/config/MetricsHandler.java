package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MetricsHandler {

  @Autowired
  private PrometheusMeterRegistry prometheusMeterRegistry;

  @PreDestroy
  public void pushMetrics() {
    try {
      PushGateway.builder()
          .address("laa-data-claims-reporting-service-uat-pushgateway-prometheus-pu:9091")
          .job("report generation")
          .registry(prometheusMeterRegistry.getPrometheusRegistry()).build()
          .push();
      System.out.println("Metrics pushed successfully");
    } catch (Exception e) {
      System.err.println("Failed to push metrics: " + e.getMessage());
    }
  }
}
