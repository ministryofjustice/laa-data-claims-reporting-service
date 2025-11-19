package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus Registry allowing for integration with pushgateway.
 */
@Configuration
public class PrometheusConfiguration {

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(key -> null);
  }
}
