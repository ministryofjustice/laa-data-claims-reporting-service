package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.time.Duration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.justice.laa.dstew.claimsreports.config.S3Timeouts;

/** Create an S3 Client with our chosen settings. */
public class S3ClientFactory {

  private final S3Timeouts s3Timeouts;

  public S3ClientFactory(S3Timeouts s3Timeouts) {
    this.s3Timeouts = s3Timeouts;
  }

  /**
   * Creates the S3 client.
   *
   * @param awsRegion - region the bucket it in
   * @return created client
   */
  public S3Client createS3Client(String awsRegion) {
    // By default, AWS does not time out API calls. Set some to avoid any risk of calls hanging
    var httpClient =
        Apache5HttpClient.builder()
            .connectionTimeout(Duration.ofSeconds(s3Timeouts.connection()))
            .socketTimeout(Duration.ofSeconds(s3Timeouts.socket()))
            .build();

    var config =
        ClientOverrideConfiguration.builder()
            .apiCallAttemptTimeout(Duration.ofSeconds(s3Timeouts.apiCallAttempt()))
            .apiCallTimeout(Duration.ofSeconds(s3Timeouts.totalApiCall()))
            .build();

    return S3Client.builder()
        .httpClient(httpClient)
        .region(Region.of(awsRegion))
        .overrideConfiguration(config)
        .build();
  }
}
