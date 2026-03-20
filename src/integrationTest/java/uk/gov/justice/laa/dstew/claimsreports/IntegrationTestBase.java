package uk.gov.justice.laa.dstew.claimsreports;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.TimeZone;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.justice.laa.dstew.claimsreports.config.TestConfig;

@Slf4j
@SpringBootTest(classes = {TestConfig.class})
@ActiveProfiles("test")
@Testcontainers
public class IntegrationTestBase {

  @BeforeAll
  static void forceUtc() {
    // Resolves issue whereby some local machines would convert date to BST in report_014, and some didn't, causing failures
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
  }

  // -------------------- Containers --------------------
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
      .withUsername("postgres")  // default superuser
      .withPassword("password")
      .withInitScript("init_extensions.sql") // <-- preload extensions
      .withExposedPorts(5432);

  static {
    POSTGRES.start();
  }

  @Container
  static final LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.4"))
          .withServices(LocalStackContainer.Service.S3);

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Autowired
  protected Clock staticClock;


  protected static final String CLAIM_TABLE_NAME = "claims.claim";
  protected static final String CLIENT_TABLE_NAME = "claims.client";
  protected static final String CLAIM_SUMMARY_FEE_TABLE_NAME= "claims.claim_summary_fee";
  protected static final String DELETE_FROM_REPLICATION_SUMMARY = "DELETE FROM claims.replication_summary";

  @BeforeAll
  static void logContainerDetails() {
    //Following can be used for checking the database contents if required (after setting a debug breakpoint).
    log.info("JDBC URL: {}, Username: {}, Password: {}", POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
    );
  }

  @DynamicPropertySource
  static void registerProperties(DynamicPropertyRegistry registry) {
    // Postgres
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);

    // LocalStack (S3)
    registry.add("aws.region", localstack::getRegion);
    registry.add("aws.accessKeyId", localstack::getAccessKey);
    registry.add("aws.secretAccessKey", localstack::getSecretKey);
    registry.add("aws.s3.endpoint",
        () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
  }

  protected void createReplicationSummaryTestData(
      LocalDate yesterday,
      OffsetDateTime now,
      Map<String, Pair<Integer, Integer>> tableCounts) {

    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);

    jdbcTemplate.update("DELETE FROM mock_pg_catalog.pg_stat_subscription");
    jdbcTemplate.update("""
           INSERT INTO mock_pg_catalog.pg_stat_subscription(subname, received_lsn, latest_end_lsn, latest_end_time)
           VALUES ('claims_reporting_service_sub', pg_current_wal_lsn()::text, pg_current_wal_lsn()::text, CURRENT_DATE);
        """);

    for (Map.Entry<String, Pair<Integer, Integer>> entry : tableCounts.entrySet()) {
      String tableName = entry.getKey();
      Integer recordCount = entry.getValue().getLeft();
      Integer updatedCount = entry.getValue().getRight();

      //latest_end_lsn (Log Sequence Number) is a marker which indicates how far the subsription has reached.
      //We set up the test summary data so that it looks like the subscription has reached the LSN recorded in the summary
      String mockLsn = jdbcTemplate.queryForObject(
          "SELECT latest_end_lsn FROM pg_stat_subscription WHERE subname = 'claims_reporting_service_sub'",
          String.class);

      jdbcTemplate.update(
          """
              INSERT INTO claims.replication_summary
              (table_name, summary_date, record_count, updated_count, wal_lsn, created_on)
              VALUES (?, ?, ?, ?, ?::pg_lsn, ?)
              """,
          tableName, yesterday, recordCount, updatedCount, mockLsn, now);
    }
  }

    /*
      Removes data written into tables by the 'integration-test' user, which is data for specific test cases only.
     */
    protected void cleanUpDataFromTests(){
      jdbcTemplate.update("""
        DELETE FROM claims.assessment
        WHERE created_by_user_id = 'integration_test_user'
        """);
      jdbcTemplate.update("""
        DELETE FROM claims.claim_case
        WHERE created_by_user_id = 'integration_test_user'
    """);
      jdbcTemplate.update("""
        DELETE FROM claims.calculated_fee_detail
         WHERE created_by_user_id = 'integration_test_user'
    """);
      jdbcTemplate.update("""
        DELETE FROM claims.claim_summary_fee
        WHERE created_by_user_id = 'integration_test_user'
    """);
      jdbcTemplate.update("""
        DELETE FROM claims.claim
        WHERE created_by_user_id = 'integration_test_user'
        """);
      jdbcTemplate.update("""
        DELETE FROM claims.submission
        WHERE created_by_user_id = 'integration_test_user'
        """);
    }

}
