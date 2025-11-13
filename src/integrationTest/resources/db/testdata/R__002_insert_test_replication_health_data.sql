--Set mock pg_catalog and mask the real pg_catalog so that the replication health check queries think that the pg_stat_subscription shows a healthy state

CREATE SCHEMA IF NOT EXISTS mock_pg_catalog;

CREATE TABLE IF NOT EXISTS mock_pg_catalog.pg_stat_subscription (
                                                                    subname text,
                                                                    latest_end_lsn text
);

INSERT INTO mock_pg_catalog.pg_stat_subscription(subname, latest_end_lsn)
VALUES ('claims_reporting_service_sub', pg_current_wal_lsn()::text);

ALTER DATABASE test
SET search_path = mock_pg_catalog, public, pg_catalog;