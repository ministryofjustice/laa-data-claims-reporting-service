package uk.gov.justice.laa.dstew.claimsreports.config;

import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusRenameFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Prometheus Registry allowing for integration with pushgateway.
 */
@Slf4j
@Configuration
public class PrometheusConfiguration {

  @Bean
  public PrometheusMeterRegistry prometheusMeterRegistry() {
    return new PrometheusMeterRegistry(key -> null);
  }

  /*
    This is due to micrometer, but without fixing it we get multiple infos a second,
    which is obviously unideal and masks any actual issues.
   */
  @Bean
  MeterFilter fixInfoLogsAboutStartTimeMessage() {
    return new PrometheusRenameFilter();
  }

}
