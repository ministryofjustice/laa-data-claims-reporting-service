package uk.gov.justice.laa.dstew.claimsreports.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;

/**
 * A local implementation of the {@link ReplicationMetadataRepository} interface
 * for managing replication metadata in non-production, mock environments.
 * It is designed to simulate the behavior of replication metadata repository
 * without relying on an external database or live infrastructure.
 *
 * <p>
 * This implementation provides hardcoded responses for methods dealing with
 * replication metadata. It is primarily intended for use in local development
 * or testing environments where the "local" Spring profile is active.
 */
@Repository
@Profile("local")
@Slf4j
public class LocalReplicationMetadataRepository
    implements ReplicationMetadataRepository {

  /**
   * Retrieves a list of published database tables being replicated.
   * This implementation returns a hardcoded list of table names
   * intended for use in a local mock environment for testing or development purposes.
   *
   * @return a list of table names in the format "schema.table" that are marked as published.
   */
  @Override
  public List<String> getPublishedTables() {
    log.info("Using local mock published tables");
    return List.of(
        "claims.claim",
        "claims.assessment"
    );
  }

  /**
   * Simulates the Write Ahead Log (WAL) status for a given subscription in a local mock environment.
   * This method returns a hardcoded {@link SubscriptionWalStatus} object that represents a healthy
   * and synchronized state for the specified subscription.
   *
   * @param subscriptionName the name of the subscription for which the WAL status is requested.
   *                         This parameter is used for logging purposes.
   * @return a {@link SubscriptionWalStatus} object containing pre-configured values that simulate
   *         a healthy replication state, including received LSN, latest applied LSN, and the
   *         timestamp of the latest applied transaction.
   */
  @Override
  public SubscriptionWalStatus getSubscriptionWalStatus(String subscriptionName) {
    log.info("Using local mock WAL status for {}", subscriptionName);

    // Simulate a healthy, fully-applied replication state
    return new SubscriptionWalStatus(
        "2CE/FFFFFFE0", // received_lsn
        "2CE/FFFFFFE0", // latest_end_lsn (fully caught up)
        Timestamp.from(Instant.now().minusSeconds(30)) // applied recently
    );
  }

}