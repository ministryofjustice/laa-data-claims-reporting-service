package uk.gov.justice.laa.dstew.claimsreports.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.*;

@Getter
@Setter
@ToString
public class ReplicationHealthReport {

  private final LocalDate summaryDate;
  private final Map<String, String> failedChecks = new LinkedHashMap<>();
  private boolean healthy;

  public ReplicationHealthReport(LocalDate summaryDate) {
    this.summaryDate = summaryDate;
  }

  public void addFailure(String table, String reason) {
    failedChecks.put(table, reason);
  }

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