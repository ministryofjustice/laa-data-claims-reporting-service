package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusRenameFilter;
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

  /*
    This updates the help message associated with micrometers messages, so that we don't get 3 infos a second clogging up the logs
   */
  @Bean
  MeterFilter fixInfoLogsAboutStartTimeMessage() {
    return new PrometheusRenameFilter();
  }

}
