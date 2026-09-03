package uk.gov.justice.laa.dstew.claimsreports.repository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationSummary;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;

/**
 * A PostgreSQL-specific implementation of the {@link ReplicationMetadataRepository} interface. This
 * repository is designed to interact with a PostgreSQL database to retrieve metadata related to
 * replication processes, such as the list of published tables and the status of Write Ahead Log
 * (WAL) for a given subscription.
 *
 * <p>This implementation is active unless the "local" Spring profile is enabled, ensuring that it
 * is used in production or non-local environments.
 *
 * <p>Key operations include: - Retrieving the list of published tables associated with a specific
 * publication. - Fetching the WAL status for a given subscription to monitor replication progress
 * and state.
 */
@Repository
@Profile("!local")
@RequiredArgsConstructor
public class PostgresReplicationMetadataRepository implements ReplicationMetadataRepository {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public List<String> getPublishedTables() {
    return jdbcTemplate.queryForList(
        """
        SELECT n.nspname || '.' || c.relname
          FROM pg_subscription_rel sr
          JOIN pg_class c ON sr.srrelid = c.oid
          JOIN pg_namespace n ON c.relnamespace = n.oid
         WHERE sr.srsubid = (SELECT oid FROM pg_subscription WHERE subname = 'claims_reporting_service_sub')
           AND c.relname != 'replication_summary'
        """,
        String.class);
  }

  @Override
  public SubscriptionWalStatus getSubscriptionWalStatus(String subscriptionName) {
    try {
      return jdbcTemplate.queryForObject(
          """
        SELECT received_lsn, latest_end_lsn, latest_end_time
        FROM pg_stat_subscription
        WHERE subname = ?
        """,
          (rs, rowNum) ->
              new SubscriptionWalStatus(
                  rs.getString("received_lsn"),
                  rs.getString("latest_end_lsn"),
                  rs.getTimestamp("latest_end_time").toInstant()),
          subscriptionName);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  @Override
  public Map<String, ReplicationSummary> getReplicationSummaries(LocalDate summaryDate) {
    String sql =
        """
        SELECT table_name, record_count, updated_count, wal_lsn
        FROM claims.replication_summary
        WHERE summary_date = ?
        """;

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<String, ReplicationSummary> map = new HashMap<>();
          while (rs.next()) {
            map.put(
                rs.getString("table_name"),
                new ReplicationSummary(
                    rs.getString("table_name"),
                    rs.getLong("record_count"),
                    rs.getLong("updated_count"),
                    rs.getString("wal_lsn")));
          }
          return map;
        },
        summaryDate);
  }
}
