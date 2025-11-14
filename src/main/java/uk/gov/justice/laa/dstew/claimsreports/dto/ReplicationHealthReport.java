package uk.gov.justice.laa.dstew.claimsreports.dto;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents a report detailing the health of the replication process for a specific date.
 * This class maintains information regarding the overall health status and any issues
 * identified with specific database tables during replication.
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ReplicationHealthReport {

  private final LocalDate summaryDate;
  private final Map<String, String> failedChecks = new LinkedHashMap<>();
  private boolean healthy;
  private boolean walLsnOk;
  private boolean tableSummaryOk;
  private boolean tableCountsOk;

  public void addFailure(String table, String reason) {
    failedChecks.put(table, reason);
  }

  /**
   * Provides a summary of the replication health status. If the replication process is
   * healthy, a message confirming the health is returned. Otherwise, details of the issues
   * are listed, including the affected tables and reasons for the failures.
   *
   * @return A message indicating either that the replication is healthy or a detailed list
   *         of tables with replication issues and the corresponding reasons.
   */
  public String summary() {
    if (healthy) {
      return String.format("Replication is healthy for %s ", summaryDate);
    }
    StringBuilder sb = new StringBuilder("Replication issues found:\n");
    failedChecks.forEach((table, reason) ->
        sb.append(" - ").append(table).append(": ").append(reason).append("\n"));
    return sb.toString();
  }
}