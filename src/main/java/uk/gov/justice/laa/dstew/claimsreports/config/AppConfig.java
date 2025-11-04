package uk.gov.justice.laa.dstew.claimsreports.config;

import java.time.Clock;
import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3FileUploader;

/**
 * Configuration class for application-level beans and settings.
 *
 * <p>
 * This class defines beans that are essential for database connectivity
 * </p>
 */
@Getter
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
   * Bean definition for a system clock that uses the default time zone of the system.
   * This is done to allow us to override the system clock from tests, to keep them static (see TestConfig under integrationTest).
   *
   * @return an instance of {@link Clock} configured to use the system's default time zone.
   */
  @Bean
  public Clock systemClock() {
    return Clock.systemDefaultZone();
  }

  /**
   * Defines how frequently the file buffer will be flushed for performant file creation.
   */
  @Value("${csv-creation.buffer-flush-freq:1000}")
  private int bufferFlushFrequency;

  /**
   * Defines how data chunks retrieved from DB will be for performant file creation.
   * Default ensures this value is never 0, which would cause an arithmetic error when used in creation of
   * CSV files.
   */
  @Value("${csv-creation.data-chunk-size:1000}")
  private int dataChunkSize;

  @Bean
  public FileUploader createFileUploader(@Value("${AWS_REGION}") String awsRegion, @Value("${S3_REPORT_STORE}") String bucketName) {
    return new S3FileUploader(new S3ClientWrapper(awsRegion, bucketName));
  }

}