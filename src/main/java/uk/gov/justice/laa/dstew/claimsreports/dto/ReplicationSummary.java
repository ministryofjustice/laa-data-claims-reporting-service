package uk.gov.justice.laa.dstew.claimsreports.dto;

public record ReplicationSummary(String tableName, long recordCount, long updatedCount, String walLsn) {

}
