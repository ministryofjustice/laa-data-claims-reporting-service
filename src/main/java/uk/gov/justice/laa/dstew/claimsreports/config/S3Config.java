package uk.gov.justice.laa.dstew.claimsreports.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3FileUploader;

/**
 * Configuration class for S3-enabled systems
 * Long-term we'd want to mock S3 locally and so this could all go in the main AppConfig
 * Separate class as AWS_REGION and S3_REPORT_STORE are env variables not setup locally.
 */
@Configuration
@ConditionalOnProperty(name = "s3.active", havingValue = "true")
public class S3Config {

  @Bean
  public FileUploader createFileUploader(@Value("${AWS_REGION}") String awsRegion, @Value("${S3_REPORT_STORE}") String bucketName) {
    return new S3FileUploader(new S3ClientWrapper(awsRegion, bucketName));
  }

}
