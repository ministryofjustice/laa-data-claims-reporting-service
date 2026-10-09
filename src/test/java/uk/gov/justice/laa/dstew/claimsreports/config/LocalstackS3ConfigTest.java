package uk.gov.justice.laa.dstew.claimsreports.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

class LocalstackS3ConfigTest {

  private final LocalstackS3Config localstackS3Config = new LocalstackS3Config();
  private S3Client createdClient;
  private final S3Timeouts s3Timeouts = new S3Timeouts(10, 60, 60, 180);

  @AfterEach
  void cleanupCreatedClients() {
    if (createdClient != null) {
      createdClient.close();
    }
  }

  @Test
  void shouldCreateLocalstackS3ClientWithConfiguredApiTimeouts() {
    createdClient =
        localstackS3Config.localstackS3Client(
            "http://localhost:4566", "eu-west-1", "test", "test", s3Timeouts);

    assertEquals(Region.EU_WEST_1, createdClient.serviceClientConfiguration().region());
    assertEquals(
        Duration.ofSeconds(60),
        createdClient
            .serviceClientConfiguration()
            .overrideConfiguration()
            .apiCallAttemptTimeout()
            .orElseThrow());
    assertEquals(
        Duration.ofSeconds(180),
        createdClient
            .serviceClientConfiguration()
            .overrideConfiguration()
            .apiCallTimeout()
            .orElseThrow());
    assertEquals(
        "http://localhost:4566",
        createdClient.serviceClientConfiguration().endpointOverride().orElseThrow().toString());
  }
}
