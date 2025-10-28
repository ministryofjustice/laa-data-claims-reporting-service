package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.time.Duration;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Create an S3 Client with our chosen settings.
 */
public class S3ClientFactory {

  // These are just best-guesses at this point, no clue how long say a 3GB file would actually take to upload.
  // They should be adjusted if actual usage turns out to not be suitable.
  public static final int TIMEOUT_FOR_EACH_ATTEMPT = 60;
  public static final int TIMEOUT_FOR_TOTAL_ATTEMPT = TIMEOUT_FOR_EACH_ATTEMPT * 3;

  /**
   * Creates the S3 client.
   *
   * @param awsRegion - region the bucket it in
   * @return created client
   */
  public S3Client createS3Client(String awsRegion) {
    // By default, AWS does not time out API calls. Set some to avoid any risk of calls hanging
    var config = ClientOverrideConfiguration.builder()
        .apiCallAttemptTimeout(Duration.ofSeconds(TIMEOUT_FOR_EACH_ATTEMPT))
        .apiCallTimeout(Duration.ofSeconds(TIMEOUT_FOR_TOTAL_ATTEMPT))
        .build();

    return S3Client.builder()
        .region(Region.of(awsRegion))
        .overrideConfiguration(config)
        .build();
  }

}
