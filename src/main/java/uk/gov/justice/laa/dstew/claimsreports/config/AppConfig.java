package uk.gov.justice.laa.dstew.claimsreports.config;

import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
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

  /**
   * Defines how frequently the file buffer will be flushed for performant file creation.
   */
  @Getter
  @Value("${csv-creation.buffer-flush-freq:1000}")
  private int bufferFlushFrequency;

  /**
   * Defines how data chunks retrieved from DB will be for performant file creation.
   * Default ensures this value is never 0, which would cause an arithmetic error when used in creation of
   * CSV files.
   */
  @Getter
  @Value("${csv-creation.buffer-flush-freq:1000}")
  private int dataChunkSize;

}