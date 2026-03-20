package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;

@Slf4j
class ReplicationHealthCheckServiceIntegrationTest extends IntegrationTestBase {

  @Autowired
  private ReplicationHealthCheckService replicationHealthCheckService;

  @Test
  void shouldReportHealthyReplicationWhenCountsMatch() {
    LocalDate yesterday = LocalDate.now(staticClock).minusDays(1);
    OffsetDateTime now = OffsetDateTime.now(staticClock);

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        CLAIM_TABLE_NAME, Pair.of(5, 2),
        CLIENT_TABLE_NAME, Pair.of(4, 2),
        CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(5, 3)
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
        CLAIM_TABLE_NAME, Pair.of(5, 2),
        CLIENT_TABLE_NAME, Pair.of(2, 2),
        CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(1, 2)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);

    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    assertThat(report).isNotNull();
    assertThat(report.isHealthy()).isFalse();
    Map<String, String> expectedFailures = Map.of(
        CLIENT_TABLE_NAME, "Count mismatch — expected (2/2), actual (4/2)",
        CLAIM_SUMMARY_FEE_TABLE_NAME, "Count mismatch — expected (1/2), actual (5/3)"
    );

    assertThat(report.getFailedChecks()).isEqualTo(expectedFailures);
  }

  @Test
  void healthCheckFailsWhenPublishedTablesNotFound() {
    // Given
    LocalDate yesterday = LocalDate.now(staticClock).minusDays(1);
    OffsetDateTime now = OffsetDateTime.now(staticClock);

    Map<String, Pair<Integer, Integer>> tableCounts = Map.of(
        CLAIM_TABLE_NAME, Pair.of(3, 1)
    );

    createReplicationSummaryTestData(yesterday, now, tableCounts);

    // When
    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isFalse();
    assertThat(report.isTableSummaryOk()).isFalse();

    assertThat(report.summary())
        .contains("claims.client: Missing replication summary for table");
  }

  @Test
  void healthCheckFailsWhenYesterdaySummaryIsMissing() {
    // Given
    LocalDate yesterday = LocalDate.now(staticClock).minusDays(1);

    // Ensure table is empty (or only has other dates)
    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);

    // When
    ReplicationHealthReport report =
        replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isFalse();
    assertThat(report.isTableSummaryOk()).isFalse();

    assertThat(report.summary())
        .contains("No replication summary found")
        .contains(yesterday.toString());
  }

  @Test
  void healthCheckFailsWhenReplicationLagDetected() {
    // Given

    OffsetDateTime staleTime =
        OffsetDateTime.now(staticClock).minusMinutes(10);

    jdbcTemplate.update("""
      UPDATE mock_pg_catalog.pg_stat_subscription
      SET received_lsn = '2CE/0000FFF0',
          latest_end_lsn = '2CE/00000010',
          latest_end_time = ?
      WHERE subname = 'claims_reporting_service_sub'
      """, staleTime);

    // When
    ReplicationHealthReport report =
        replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isFalse();
    assertThat(report.isWalLsnOk()).isFalse();

    assertThat(report.summary())
        .contains("Replication lag detected");
  }

  @Test
  void healthCheckFailsWhenLatestEndTimeIsTooOld() {
    // Given
    OffsetDateTime staleTime =
        OffsetDateTime.now(staticClock).minusHours(2);

    jdbcTemplate.update("""
      UPDATE mock_pg_catalog.pg_stat_subscription
      SET received_lsn = '2CE/0000FFF0',
          latest_end_lsn = '2CE/0000FFF0',
          latest_end_time = ?
      WHERE subname = 'claims_reporting_service_sub'
      """, staleTime);

    // When
    ReplicationHealthReport report =
        replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isFalse();
    assertThat(report.isWalLsnOk()).isFalse();

    assertThat(report.summary())
        .contains("Replication apply has not progressed");
  }

  @Test
  void healthCheckPassesWhenSubscriptionIsUpToDate() {
    // Given
    OffsetDateTime recentTime =
        OffsetDateTime.now(staticClock).minusSeconds(10);

    jdbcTemplate.update("""
      UPDATE mock_pg_catalog.pg_stat_subscription
      SET received_lsn = '2CE/0000FFF0',
          latest_end_lsn = '2CE/0000FFF0',
          latest_end_time = ?
      WHERE subname = 'claims_reporting_service_sub'
      """, recentTime);

    // When
    ReplicationHealthReport report =
        replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isTrue();
  }

  @Test
  void healthCheckFailsWhenOnlyOlderSummaryExists() {
    // Given
    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);

    LocalDate twoDaysAgo = LocalDate.now(staticClock).minusDays(2);

    jdbcTemplate.update("""
      INSERT INTO claims.replication_summary
      (table_name, summary_date, record_count, updated_count, wal_lsn, created_on)
      VALUES ('claims.claim', ?, 10, 2, '2CE/0000FFF0'::pg_lsn, now())
      """, twoDaysAgo);

    // When
    ReplicationHealthReport report =
        replicationHealthCheckService.checkReplicationHealth();

    // Then
    assertThat(report.isHealthy()).isFalse();
    assertThat(report.isTableSummaryOk()).isFalse();
  }

}
