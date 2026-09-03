package uk.gov.justice.laa.dstew.claimsreports.dto;

/**
 * Represents a summary of replication metrics for a specific database table. This record captures
 * key information that helps in assessing the replication process, including the table name, the
 * number of records present, and updates occurring during previous day.
 *
 * @param tableName The name of the database table involved in replication.
 * @param recordCount The total number of records processed for the table during replication.
 * @param updatedCount The number of records updated for the table during replication.
 * @param walLsn The Log Sequence Number (LSN) associated with the table's Write Ahead Log (WAL),
 *     providing a marker for replication progress.
 */
public record ReplicationSummary(
    String tableName, long recordCount, long updatedCount, String walLsn) {}
