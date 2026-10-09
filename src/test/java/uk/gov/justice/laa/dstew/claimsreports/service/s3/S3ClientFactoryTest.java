package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import uk.gov.justice.laa.dstew.claimsreports.config.S3Timeouts;

class S3ClientFactoryTest {
  private final S3Timeouts s3Timeouts = new S3Timeouts(1, 2, 3, 4);
  private final S3ClientFactory s3ClientFactory = new S3ClientFactory(s3Timeouts);
  private S3Client createdClient;

  @AfterEach
  void cleanupCreatedClient() {
    if (createdClient != null) {
      createdClient.close();
    }
  }

  @Test
  void shouldCreateS3ClientWithPassedInRegion() {

    createdClient = s3ClientFactory.createS3Client("eu-west-1");

    assertEquals(Region.EU_WEST_1, createdClient.serviceClientConfiguration().region());
  }

  @Test
  void shouldCreateS3ClientWithApiTimeoutsSet() {

    createdClient = s3ClientFactory.createS3Client("eu-west-1");
    assertEquals(
        Optional.of(Duration.ofSeconds(4)),
        createdClient.serviceClientConfiguration().overrideConfiguration().apiCallTimeout());
    assertEquals(
        Optional.of(Duration.ofSeconds(3)),
        createdClient.serviceClientConfiguration().overrideConfiguration().apiCallAttemptTimeout());
  }
}
