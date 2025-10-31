package uk.gov.justice.laa.dstew.claimsreports.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3FileUploader;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestConfig {
  @Bean
  public S3Client mockS3Client() {
    return mock(S3Client.class);
  }

  @Bean
  public S3ClientWrapper createS3ReportClient() {
    return new S3ClientWrapper(mockS3Client(), "test-bucket-reports");
  }

  @Bean
  @Primary
  public FileUploader createTestFileUploader() {
    return new S3FileUploader(createS3ReportClient());
  }

}
