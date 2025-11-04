package uk.gov.justice.laa.dstew.claimsreports.config;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3FileUploader;

@TestConfiguration
public class TestConfig {
  @Bean
  @Primary
  public S3Client localstackS3Client(
      @Value("${aws.s3.endpoint}") String endpoint,
      @Value("${aws.region}") String region,
      @Value("${aws.accessKeyId}") String accessKey,
      @Value("${aws.secretAccessKey}") String secretKey) {
    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        .build();
  }

  @Bean
  @Primary
  public S3ClientWrapper s3ClientWrapper(S3Client localstackS3Client, @Value("${S3_REPORT_STORE}") String bucketName) {
    try {
      localstackS3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    } catch (BucketAlreadyOwnedByYouException e) {
      // ignore
    }
    return new S3ClientWrapper(localstackS3Client, bucketName);
  }

  @Bean
  @Primary
  public FileUploader createTestFileUploader(S3ClientWrapper s3ClientWrapper) {
    return new S3FileUploader(s3ClientWrapper);
  }

  @Bean
  @Primary
  public Clock fixedClock() {
    return Clock.fixed(
        LocalDate.of(2025, 11, 3)
            .atTime(5, 0)                     // 5:00 AM
            .atZone(ZoneId.systemDefault())   // apply your timezone
            .toInstant(),
        ZoneId.systemDefault()
    );
  }
}
