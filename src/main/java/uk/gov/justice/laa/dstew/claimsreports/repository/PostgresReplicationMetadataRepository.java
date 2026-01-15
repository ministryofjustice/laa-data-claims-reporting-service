package uk.gov.justice.laa.dstew.claimsreports.repository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;

/**
 * A PostgreSQL-specific implementation of the {@link ReplicationMetadataRepository} interface.
 * This repository is designed to interact with a PostgreSQL database to retrieve metadata
 * related to replication processes, such as the list of published tables and the status
 * of Write Ahead Log (WAL) for a given subscription.
 *
 * <p>
 * This implementation is active unless the "local" Spring profile is enabled, ensuring that
 * it is used in production or non-local environments.
 *
 *  <p>
 * Key operations include:
 * - Retrieving the list of published tables associated with a specific publication.
 * - Fetching the WAL status for a given subscription to monitor replication progress and state.
 */
@Repository
@Profile("!local")
@RequiredArgsConstructor
public class PostgresReplicationMetadataRepository
    implements ReplicationMetadataRepository {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<String> getPublishedTables() {
    return jdbcTemplate.queryForList("""
        SELECT schemaname || '.' || tablename
        FROM pg_publication_tables
        WHERE pubname = 'claims_reporting_service_pub'
          AND tablename != 'replication_summary'
        """, String.class);
  }

  @Override
  public SubscriptionWalStatus getSubscriptionWalStatus(String subscriptionName) {
    return jdbcTemplate.queryForObject("""
      SELECT received_lsn, latest_end_lsn, latest_end_time
      FROM pg_stat_subscription
      WHERE subname = ?
      """,
        (rs, rowNum) -> new SubscriptionWalStatus(
            rs.getString("received_lsn"),
            rs.getString("latest_end_lsn"),
            rs.getTimestamp("latest_end_time")
        ),
        subscriptionName
    );
  }

}