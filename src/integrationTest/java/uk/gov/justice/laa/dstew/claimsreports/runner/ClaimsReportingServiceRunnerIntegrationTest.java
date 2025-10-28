package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.entity.Report000Entity;
import uk.gov.justice.laa.dstew.claimsreports.repository.Report000Repository;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService;
import uk.gov.justice.laa.dstew.claimsreports.service.Report000Service;

/**
 * Integration tests for the ClaimsReportingServiceRunner component, verifying its functionality
 * in a full application context using test-specific settings and PostgreSQL Testcontainers.
 *
 * <p>
 * This test ensures that:
 * - The materialized view associated with `Report000Entity` is refreshed correctly.
 * - Data is successfully retrieved from the database.
 * - The generated reports or corresponding data output meet expected conditions.
 *
 * <p>
 * An embedded PostgreSQL container is used for the test database to simulate the production-like
 * environment. The `@SpringBootTest` annotation is used to load the application context, and
 * dependencies such as `Report000Service` and `Report000Repository` are autowired for testing.
 *
 * <p>
 * An active Spring profile, "test", is set to ensure specific test configurations are applied.
 * e.g. the sql scripts in the testdata folder which insert test data are run only for the test profile.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public class ClaimsReportingServiceRunnerIntegrationTest {

  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

  @Autowired
  private Report000Repository repository;

  @Autowired
  private Report000Service report000Service;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ReplicationHealthCheckService replicationHealthCheckService;

  static {
    postgres.start();
    System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
    System.setProperty("spring.datasource.username", postgres.getUsername());
    System.setProperty("spring.datasource.password", postgres.getPassword());
  }

  @BeforeEach
  void cleanReplicationSummaryTable() {
    jdbcTemplate.update("DELETE FROM claims.replication_summary");
  }

  @Test
  void testViewAndReportGeneration() {
    report000Service.refreshMaterializedView();

    List<Report000Entity> rows = repository.findAll();
    assertThat(rows).isNotEmpty();
    assertThat(rows.size()).isEqualTo(2);

    // Validate CSV output or generated file if applicable
  }

  @Test
  void testEnsureReplicationHealthy() {
    // Arrange
    LocalDate yesterday = LocalDate.now().minusDays(1);
    OffsetDateTime now = OffsetDateTime.now();

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        "claim", Pair.of(2, 1),
        "client", Pair.of(2, 1),
        "claim_summary_fee", Pair.of(2, 2)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);
    // Act
    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    // Assert
    assertThat(report).isNotNull();
    assertThat(report.isHealthy()).isTrue();
  }

  @Test
  void testEnsureReplicationUnhealthy() {
    // Arrange
    LocalDate yesterday = LocalDate.now().minusDays(1);
    OffsetDateTime now = OffsetDateTime.now();

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        "claim", Pair.of(3, 1),
        "client", Pair.of(2, 2),
        "claim_summary_fee", Pair.of(1, 2)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);
    // Act
    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    // Assert
    assertThat(report).isNotNull();
    assertThat(report.isHealthy()).isFalse();
    Map<String, String> expectedFailures = Map.of(
        "claim", "Count mismatch — expected (3/1), actual (2/1)",
        "client", "Count mismatch — expected (2/2), actual (2/1)",
        "claim_summary_fee", "Count mismatch — expected (1/2), actual (2/2)"
    );

    assertThat(report.getFailedChecks()).isEqualTo(expectedFailures);  }


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