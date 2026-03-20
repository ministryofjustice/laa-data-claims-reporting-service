package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;

/**
 * Integration tests for the ClaimsReportingServiceRunner.
 *
 * <p>This test verifies the following:
 * - The health status of database replication, ensuring data consistency between replication tables.
 * - The correctness of report generation and upload of CSV files to an S3-compatible storage.
 *
 * <p>Key Features:
 * - Utilizes Testcontainers to set up PostgreSQL and LocalStack (with S3) containers for isolated testing.
 * - Dynamically registers environment properties required for testing, such as database connection details and S3 configurations.
 * - Tests replication health by inserting mock data into the database and validating the results using a service.
 * - Verifies that report files are generated, uploaded to the S3 bucket, and match expected content.
 */
@Slf4j
class ClaimsReportingServiceRunnerIntegrationTest extends IntegrationTestBase {

  @Value("${S3_REPORT_STORE}")
  private String bucketName;
  private static final int NUMBER_OF_REPORTS = 4;

  @Autowired
  private ClaimsReportingServiceRunner serviceRunner;

  @Autowired
  private List<AbstractReportService> reportServices;

  @Autowired
  private S3Client s3Client;


  @BeforeEach
  void setup() {
    // Reset replication summary
    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);
    insertHealthyReplicationData();
  }

  @AfterEach
  void emptyS3Bucket() {

    log.info("Emptying bucket");

    ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build());

    if (listResponse.hasContents()) {
      List<ObjectIdentifier> objects = listResponse.contents().stream()
          .map(o -> ObjectIdentifier.builder().key(o.key()).build())
          .toList();

      s3Client.deleteObjects(DeleteObjectsRequest.builder()
          .bucket(bucketName)
          .delete(d -> d.objects(objects))
          .build());
    }
  }

  @Test
  void shouldGenerateAllReportsAndUploadCSVsToS3() throws Exception {
    log.info("Detected report service implementations: {}",
        reportServices.stream()
            .map(s -> s.getClass().getSimpleName())
            .collect(Collectors.joining(", "))
    );

    //Assert that expected number of reportServices were autowired
    assertThat(reportServices)
        .isNotEmpty()
        .hasSize(NUMBER_OF_REPORTS);

    // Run report generation end-to-end
    serviceRunner.run(null);
    // Check uploads
    ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build());

    List<String> uploadedFiles = listResponse.contents().stream()
        .map(S3Object::key)
        .toList();

    log.info("Uploaded report files: {}", uploadedFiles);

    //Assert that expected number of reports were generated
    assertThat(uploadedFiles)
        .isNotEmpty()
        .hasSize(NUMBER_OF_REPORTS);

    // Compare each uploaded file to the expected CSV in resources
    for (String uploadedKey : uploadedFiles) {
      // Create a temp file for the uploaded S3 object
      Path tempFile = Files.createTempFile("uploaded-", ".csv");
      try (InputStream s3is = s3Client.getObject(GetObjectRequest.builder()
          .bucket(bucketName)
          .key(uploadedKey)
          .build())) {
        Files.copy(s3is, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }

      // Locate the expected file in resources
      Path expectedFile = Paths.get("src/integrationTest/resources/expected_csv_files", uploadedKey);
      assertThat(tempFile.toFile())
          .as("CSV file comparison for " + uploadedKey)
          .hasSameTextualContentAs(expectedFile.toFile());

      log.info("CSV file '{}' matches expected content.", uploadedKey);
    }

  }

  @Test
  void shouldNotGenerateReportsAndUploadCSVsToS3WhenReplicationNotHealthy() {

    // Make replication unhealthy
    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);

    // Run report generation end-to-end
    serviceRunner.run(null);

    // Check uploads
    ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build());

    List<String> uploadedFiles = listResponse.contents().stream()
        .map(S3Object::key)
        .toList();

    log.info("Uploaded report files: {}", uploadedFiles);

    //Assert that no reports were generated
    assertThat(uploadedFiles)
        .isEmpty();
  }

  // ------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------

  private void insertHealthyReplicationData() {
    createReplicationSummaryTestData(
        LocalDate.now(staticClock).minusDays(1),
        OffsetDateTime.now(staticClock),
        Map.of(
            CLAIM_TABLE_NAME, Pair.of(5, 2),
            CLIENT_TABLE_NAME, Pair.of(4, 2),
            CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(5, 3)
        )
    );
  }

}