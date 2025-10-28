package uk.gov.justice.laa.dstew.claimsreports.service;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;

/**
 * The {@code ReplicationHealthCheckService} class provides functionality to evaluate
 * the health of replication processes within a database environment. This service
 * checks multiple parameters and reports on the system's replication status,
 * including checks for missing tables, write-ahead log (WAL) progression discrepancies,
 * and differences in expected vs actual data counts.
 *
 * <p>The service is responsible for:
 * - Fetching relevant database state (e.g., published tables, replication summaries).
 * - Running detailed health checks for replication issues.
 * - Generating a comprehensive report capturing the replication's health status
 *   and failures, if any.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReplicationHealthCheckService {

  private final JdbcTemplate jdbcTemplate;

  /**
   * Checks the replication health for a specific date, typically the previous day.
   * This method evaluates various metrics and conditions such as missing tables,
   * write-ahead log (WAL) progress, and data counts to determine the overall health
   * of the system's replication processes.
   *
   * <p>@return A {@code ReplicationHealthReport} object containing the health status,
   *         any detected issues, and a summary of the replication's health for the checked date.
   */
  public ReplicationHealthReport checkReplicationHealth() {
    final LocalDate summaryDate = LocalDate.now().minusDays(1);
    final Timestamp startOfDay = Timestamp.valueOf(summaryDate.atStartOfDay());
    final Timestamp endOfDay = Timestamp.valueOf(summaryDate.plusDays(1).atStartOfDay());

    log.info("Checking replication health for {}", summaryDate);

    List<String> publicationTables = getPublishedTables();
    Map<String, ReplicationSummary> summaries = getReplicationSummaries(summaryDate);

    ReplicationHealthReport report = new ReplicationHealthReport(summaryDate);

    checkMissingTables(publicationTables, summaries, report);
    checkWalProgress(summaries, report);
    checkCounts(summaries, startOfDay, endOfDay, report);

    report.setHealthy(report.getFailedChecks().isEmpty());

    if (report.isHealthy()) {
      log.info("Replication looks healthy for {}", summaryDate);
    } else {
      log.warn("Replication health check failed for {}:\n{}", summaryDate, report.summary());
    }

    return report;
  }

  // --- Private helpers ---

  private List<String> getPublishedTables() {
    String sql = """
            SELECT schemaname || '.' || tablename AS full_table_name
            FROM pg_publication_tables
            WHERE pubname = 'claims_reporting_service_pub'
              AND tablename != 'replication_summary'
            """;
    return jdbcTemplate.queryForList(sql, String.class);
  }

  private Map<String, ReplicationSummary> getReplicationSummaries(LocalDate summaryDate) {
    String sql = """
            SELECT table_name, record_count, updated_count, wal_lsn
            FROM claims.replication_summary
            WHERE summary_date = ?
            """;
    return jdbcTemplate.query(sql, rs -> {
      Map<String, ReplicationSummary> map = new HashMap<>();
      while (rs.next()) {
        map.put(rs.getString("table_name"),
            new ReplicationSummary(
                rs.getString("table_name"),
                rs.getLong("record_count"),
                rs.getLong("updated_count"),
                rs.getString("wal_lsn")));
      }
      return map;
    }, summaryDate);
  }

  private void checkMissingTables(List<String> tables, Map<String, ReplicationSummary> summaries,
      ReplicationHealthReport report) {
    for (String table : tables) {
      if (!summaries.containsKey(table)) {
        report.addFailure(table, "Missing replication summary for table");
      }
    }
  }

  private void checkWalProgress(Map<String, ReplicationSummary> summaries,
      ReplicationHealthReport report) {
    String currentWal = jdbcTemplate.queryForObject("SELECT pg_current_wal_lsn()", String.class);
    for (ReplicationSummary summary : summaries.values()) {
      if (compareWal(summary.walLsn(), currentWal) > 0) {
        report.addFailure(summary.tableName(),
            String.format("WAL LSN in summary (%s) is ahead of current WAL (%s)",
                summary.walLsn(), currentWal));
      }
    }
  }

  private int compareWal(String wal1, String wal2) {
    return new BigInteger(wal1.replace("/", ""), 16)
        .compareTo(new BigInteger(wal2.replace("/", ""), 16));
  }

  private void checkCounts(Map<String, ReplicationSummary> summaries,
      Timestamp start, Timestamp end,
      ReplicationHealthReport report) {
    for (ReplicationSummary summary : summaries.values()) {
      String countSql = String.format("SELECT count(*) FROM claims.%s WHERE created_on < ?", summary.tableName());
      String updatedSql = String.format("SELECT count(*) FROM claims.%s WHERE updated_on BETWEEN ? AND ?", summary.tableName());

      Long actualRecordCount = jdbcTemplate.queryForObject(countSql, Long.class, end);
      Long actualUpdatedCount = jdbcTemplate.queryForObject(updatedSql, Long.class, start, end);

      if (!Objects.equals(actualRecordCount, summary.recordCount())
          || !Objects.equals(actualUpdatedCount, summary.updatedCount())) {
        report.addFailure(summary.tableName(),
            String.format("Count mismatch — expected (%d/%d), actual (%d/%d)",
                summary.recordCount(), summary.updatedCount(),
                actualRecordCount, actualUpdatedCount));
      }
    }
  }

  // --- DTOs ---

  record ReplicationSummary(String tableName, long recordCount, long updatedCount, String walLsn) {}
}