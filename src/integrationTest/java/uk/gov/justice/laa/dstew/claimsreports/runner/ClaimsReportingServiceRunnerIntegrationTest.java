package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.justice.laa.dstew.claimsreports.config.TestConfig;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService;

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
@SpringBootTest(classes = {TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
public class ClaimsReportingServiceRunnerIntegrationTest {

  @Value("${S3_REPORT_STORE}")
  private String bucketName;
  private static final String CLAIM_TABLE_NAME = "claim";
  private static final String CLIENT_TABLE_NAME = "client";
  private static final String CLAIM_SUMMARY_FEE_TABLE_NAME = "claim_summary_fee";
  private static final int NUMBER_OF_REPORTS = 3;

  // -------------------- Containers --------------------
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
      .withUsername("postgres")  // default superuser
      .withPassword("password")
      .withInitScript("init_extensions.sql") // <-- preload extensions
      .withExposedPorts(5432);

  @Container
  static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4"))
          .withServices(LocalStackContainer.Service.S3);

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ReplicationHealthCheckService replicationHealthCheckService;

  @Autowired
  private ClaimsReportingServiceRunner serviceRunner;

  @Autowired
  private List<AbstractReportService> reportServices;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private Clock staticClock;

  static {
    // Ensure both containers are fully started before Spring initializes the context
    postgres.start();
    localstack.start();
    //Following can be used for checking the database contents if required (after setting a debug breakpoint).
    log.info("JDBC URL: {}, Username: {}, Password: {}", postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()
    );
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // Postgres
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);

    // LocalStack (S3)
    registry.add("aws.region", localstack::getRegion);
    registry.add("aws.accessKeyId", localstack::getAccessKey);
    registry.add("aws.secretAccessKey", localstack::getSecretKey);
    registry.add("aws.s3.endpoint",
        () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
  }

  @BeforeEach
  void setup() {
    jdbcTemplate.update("DELETE FROM claims.replication_summary");
  }

  // ------------------------------------------------------------
  // Replication Health Tests
  // ------------------------------------------------------------

  @Test
  void shouldReportHealthyReplicationWhenCountsMatch() {
    LocalDate yesterday = LocalDate.now(staticClock).minusDays(1);
    OffsetDateTime now = OffsetDateTime.now(staticClock);

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        CLAIM_TABLE_NAME, Pair.of(2, 1),
        CLIENT_TABLE_NAME, Pair.of(2, 1),
        CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(2, 2)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);

    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    assertThat(report).isNotNull();
    assertThat(report.isHealthy()).isTrue();
  }

  @Test
  void shouldReportUnhealthyReplicationWhenCountsDiffer() {
    LocalDate yesterday = LocalDate.now(staticClock).minusDays(1);
    OffsetDateTime now = OffsetDateTime.now(staticClock);

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        CLAIM_TABLE_NAME, Pair.of(3, 1),
        CLIENT_TABLE_NAME, Pair.of(2, 2),
        CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(1, 2)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);

    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    assertThat(report).isNotNull();
    assertThat(report.isHealthy()).isFalse();
    Map<String, String> expectedFailures = Map.of(
        CLAIM_TABLE_NAME, "Count mismatch — expected (3/1), actual (2/1)",
        CLIENT_TABLE_NAME, "Count mismatch — expected (2/2), actual (2/1)",
        CLAIM_SUMMARY_FEE_TABLE_NAME, "Count mismatch — expected (1/2), actual (2/2)"
    );

    assertThat(report.getFailedChecks()).isEqualTo(expectedFailures);
  }

  // ------------------------------------------------------------
  // Report Generation Tests (with real S3 uploads)
  // ------------------------------------------------------------

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

    // Make replication healthy
    insertHealthyReplicationData();

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

  // ------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------

  private void insertHealthyReplicationData() {
    createReplicationSummaryTestData(
        LocalDate.now(staticClock).minusDays(1),
        OffsetDateTime.now(staticClock),
        Map.of(
            CLAIM_TABLE_NAME, Pair.of(2, 1),
            CLIENT_TABLE_NAME, Pair.of(2, 1),
            CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(2, 2)
        )
    );
  }

  private void createReplicationSummaryTestData(
      LocalDate yesterday,
      OffsetDateTime now,
      Map<String, Pair<Integer, Integer>> tableCounts) {

    for (Map.Entry<String, Pair<Integer, Integer>> entry : tableCounts.entrySet()) {
      String tableName = entry.getKey();
      Integer recordCount = entry.getValue().getLeft();
      Integer updatedCount = entry.getValue().getRight();

      jdbcTemplate.update(
          """
              INSERT INTO claims.replication_summary
              (table_name, summary_date, record_count, updated_count, wal_lsn, created_on)
              VALUES (?, ?, ?, ?, pg_current_wal_lsn(), ?)
              """,
          tableName, yesterday, recordCount, updatedCount, now);
    }
  }
}