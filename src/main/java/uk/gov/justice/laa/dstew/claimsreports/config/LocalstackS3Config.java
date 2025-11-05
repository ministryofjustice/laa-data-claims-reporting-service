package uk.gov.justice.laa.dstew.claimsreports.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Configuration class for setting up S3-related beans for use in a local development environment
 * leveraging Localstack.
 *
 * <p>This class is only active when the 'local' Spring profile is active.
 * It configures the necessary beans to connect to a Localstack S3 service, provide file upload
 * functionality, and create the required S3 bucket if it does not already exist.
 */
@Configuration
@Profile("local") // Only active for the 'local' profile
public class LocalstackS3Config {

  /**
   * Creates and configures an instance of {@link S3Client} for integration with Localstack's S3 service.
   * The client is set up with static credentials, a custom endpoint, and path-style access enabled.
   *
   * @param endpoint the custom endpoint URL of the Localstack S3 service.
   * @param region the AWS region to be used by the S3 client.
   * @param accessKey the access key ID to use for authentication.
   * @param secretKey the secret access key to use for authentication.
   * @return a configured {@link S3Client} for interacting with the Localstack S3 service.
   */
  @Bean
  public S3Client localstackS3Client(
      @Value("${aws.s3.endpoint}") String endpoint,
      @Value("${AWS_REGION}") String region,
      @Value("${aws.accessKeyId}") String accessKey,
      @Value("${aws.secretAccessKey}") String secretKey) {

    return S3Client.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build()
        )
        .build();
  }

  /**
   * Creates and initializes an instance of {@link S3ClientWrapper}, which wraps an {@link S3Client}
   * and provides additional functionalities specific to the application's requirements.
   * This method also ensures that the specified S3 bucket exists, creating it if necessary.
   *
   * @param localstackS3Client the {@link S3Client} instance configured to interact with the S3 service,
   *                           typically pointed at a Localstack environment for local development.
   * @param bucketName the name of the S3 bucket that the {@link S3ClientWrapper} will interact with.
   * @return a configured {@link S3ClientWrapper} instance.
   */
  @Bean
  public S3ClientWrapper s3ClientWrapper(
      S3Client localstackS3Client,
      @Value("${S3_REPORT_STORE}") String bucketName) {

    // create bucket if it doesn't exist
    try {
      localstackS3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    } catch (BucketAlreadyOwnedByYouException e) {
      // ignore
    }

    return new S3ClientWrapper(localstackS3Client, bucketName);
  }

}
