package uk.gov.justice.laa.dstew.claimsreports.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationSummary;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;
import uk.gov.justice.laa.dstew.claimsreports.repository.ReplicationMetadataRepository;

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

  public static final String REPLICATION = "replication";
  public static final int TOLERABLE_REPLICATION_DELAY_SECONDS = 300;
  private final JdbcTemplate jdbcTemplate;
  private final ReplicationMetadataRepository metadataRepository;
  private final Clock clock; //This is the system clock for normal prod use, overridden by a static one for tests.
  private static final String SUBSCRIPTION_NAME = "claims_reporting_service_sub";
  private static final Pattern SAFE_SQL_IDENTIFIER =
          Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,127}+(?:\\.[A-Za-z_][A-Za-z0-9_]{0,127}+)?+");

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
    final LocalDate summaryDate = LocalDate.now(clock).minusDays(1);
    final Timestamp startOfDay = Timestamp.valueOf(summaryDate.atStartOfDay());
    final Timestamp endOfDay = Timestamp.valueOf(summaryDate.plusDays(1).atStartOfDay());

    log.info("Checking replication health for {}", summaryDate);

    ReplicationHealthReport report = new ReplicationHealthReport(summaryDate);
    List<String> publicationTables = metadataRepository.getPublishedTables();

    if (publicationTables == null || publicationTables.isEmpty()) {
      report.setTableSummaryOk(false);
      report.addFailure(
          "publication",
          "No tables found for publication claims_reporting_service_pub"
      );
    } else {
      report.setTableSummaryOk(true); //OK so far, i.e. at least publication tables exist.
    }

    Map<String, ReplicationSummary> summaries = metadataRepository.getReplicationSummaries(summaryDate);

    if (summaries == null || summaries.isEmpty()) {
      report.setTableSummaryOk(false);
      report.addFailure("replication_summary",
          "No replication summary found for " + summaryDate);
    }

    checkWalProgress(report);
    if (report.isTableSummaryOk()) {
      checkMissingTables(publicationTables, summaries, report);
      checkCounts(summaries, startOfDay, endOfDay, report);
    }

    report.setHealthy(report.isWalLsnOk() && report.isTableSummaryOk() && report.isTableCountsOk());

    if (report.isHealthy()) {
      log.info("Replication looks healthy for {}", summaryDate);
    } else {
      log.warn("Replication health check failed for {}:\n{}", summaryDate, report.summary());
    }

    return report;
  }

  // --- Private helpers ---

  private void checkMissingTables(List<String> publishedTables, Map<String, ReplicationSummary> summaries,
      ReplicationHealthReport report) {
    report.setTableSummaryOk(true);
    for (String table : publishedTables) {
      if (!summaries.containsKey(table)) {
        report.addFailure(table, "Missing replication summary for table");
        report.setTableSummaryOk(false);
      }
    }
  }

  private void checkWalProgress(
      ReplicationHealthReport report
  ) {
    Instant now = clock.instant();
    SubscriptionWalStatus wal = metadataRepository.getSubscriptionWalStatus(SUBSCRIPTION_NAME);

    if (wal == null || wal.latestEndLsn() == null) {
      report.setWalLsnOk(false);
      report.addFailure(REPLICATION, "No WAL progress information available, replication is failing. Please check RDS logs for more details.");
    } else {
      Instant lastApplied = wal.latestEndTime();

      if (lastApplied == null) {
        report.setWalLsnOk(false);
        report.addFailure(REPLICATION, "WAL latest end time is null");
      } else if (lastApplied.isBefore(now.minusSeconds(TOLERABLE_REPLICATION_DELAY_SECONDS))) {
        long lagMinutes = Duration.between(lastApplied, now).toMinutes();
        report.setWalLsnOk(false);
        report.addFailure(
            REPLICATION,
            String.format(
                "Replication apply has not progressed for %d minutes",
                lagMinutes
            )
        );
        if (compareWal(wal.receivedLsn(), wal.latestEndLsn()) > 0) {
          report.setWalLsnOk(false);
          report.addFailure(
              REPLICATION,
              String.format(
                  "Replication lag detected — received WAL %s but only applied %s",
                  wal.receivedLsn(), wal.latestEndLsn()
              )
          );
        }
      } else {
        report.setWalLsnOk(true);
      }
    }
  }

  private int compareWal(String wal1, String wal2) {
    return new BigInteger(wal1.replace("/", ""), 16)
        .compareTo(new BigInteger(wal2.replace("/", ""), 16));
  }

  @SuppressFBWarnings(
      value = "SQL_INJECTION_SPRING_JDBC",
      justification = "Table names are validated as SQL identifiers before count queries are assembled."
  )
  private void checkCounts(Map<String, ReplicationSummary> summaries,
      Timestamp startOfDay, Timestamp endOfDay,
      ReplicationHealthReport report) {
    report.setTableCountsOk(true);
    for (ReplicationSummary summary : summaries.values()) {
      String tableName = validatedTableName(summary.tableName());
      String countSql = String.format("SELECT count(*) FROM %s WHERE created_on < ?", tableName);
      String updatedSql = String.format("SELECT count(*) FROM %s WHERE updated_on BETWEEN ? AND ?", tableName);

      Long actualRecordCount = jdbcTemplate.query(countSql, rs -> {
        if (rs.next()) {
          return rs.getLong(1);
        }
        return 0L;
      }, endOfDay);

      Long actualUpdatedCount = jdbcTemplate.query(updatedSql, rs -> {
        if (rs.next()) {
          return rs.getLong(1);
        }
        return 0L;
      }, startOfDay, endOfDay);

      if (!Objects.equals(actualRecordCount, summary.recordCount())
          || !Objects.equals(actualUpdatedCount, summary.updatedCount())) {
        report.setTableCountsOk(false);
        report.addFailure(summary.tableName(),
            String.format("Count mismatch — expected (%d/%d), actual (%d/%d)",
                summary.recordCount(), summary.updatedCount(),
                actualRecordCount, actualUpdatedCount));
      }
    }
  }

  private String validatedTableName(String tableName) {
    if (tableName == null || !SAFE_SQL_IDENTIFIER.matcher(tableName).matches()) {
      throw new IllegalStateException("Unsafe replication summary table name");
    }
    return tableName;
  }

}