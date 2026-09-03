package uk.gov.justice.laa.dstew.claimsreports.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationSummary;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;

/**
 * Interface for managing replication metadata, providing operations to query metadata related to
 * database replication. It defines methods to retrieve a list of published tables and to obtain the
 * status of the Write Ahead Log (WAL) for a specified subscription.
 *
 * <p>Implementations of this interface are responsible for providing appropriate functionality to
 * handle replication metadata retrieval. This may include interacting with databases or other
 * storage mechanisms. Example implementations could include a local mock repository for testing or
 * a PostgreSQL-based repository for production use.
 */
public interface ReplicationMetadataRepository {

  List<String> getPublishedTables();

  SubscriptionWalStatus getSubscriptionWalStatus(String subscriptionName);

  Map<String, ReplicationSummary> getReplicationSummaries(LocalDate summaryDate);
}
