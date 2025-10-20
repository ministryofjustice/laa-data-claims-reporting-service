package uk.gov.justice.laa.dstew.claimsreports.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Configuration class for application-level beans and settings.
 *
 * <p>
 * This class defines beans that are essential for database connectivity
 * </p>
 */
@Configuration
public class AppConfig {
  /**
   * Configures a {@link DataSource} in the application's configuration file.
   *
   * @return a configured {@link DataSource} for read-only operations.
   */
  @Bean
  @ConfigurationProperties(prefix = "spring.datasource")
  DataSource dataSource() {
    return new DriverManagerDataSource();
  }

  /**
   * Configures a {@link JdbcTemplate} for database operations.
   *
   * @param dataSource the {@link DataSource} to be used by the {@link JdbcTemplate}.
   * @return a configured {@link JdbcTemplate} for database operations.
   */
  @Bean
  JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
