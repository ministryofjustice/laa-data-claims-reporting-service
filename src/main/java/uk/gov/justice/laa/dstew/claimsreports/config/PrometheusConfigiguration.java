package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrometheusConfigiguration {

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(key -> null);
  }
}
