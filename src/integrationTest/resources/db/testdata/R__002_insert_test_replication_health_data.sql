--Set mock pg_catalog and mask the real pg_catalog so that the replication health check queries think that the pg_stat_subscription shows a healthy state

CREATE SCHEMA IF NOT EXISTS mock_pg_catalog;

CREATE TABLE IF NOT EXISTS mock_pg_catalog.pg_stat_subscription (
                                                                    subname text,
                                                                    received_lsn text,
                                                                    latest_end_lsn text,
                                                                    latest_end_time timestamptz
);

INSERT INTO mock_pg_catalog.pg_stat_subscription(subname, received_lsn, latest_end_lsn, latest_end_time)
VALUES ('claims_reporting_service_sub', pg_current_wal_lsn()::text, pg_current_wal_lsn()::text, CURRENT_DATE);

-- pg_namespace
CREATE TABLE mock_pg_catalog.pg_namespace (
                                              oid          BIGINT PRIMARY KEY,
                                              nspname      TEXT NOT NULL
);

-- pg_class
CREATE TABLE mock_pg_catalog.pg_class (
                                          oid            BIGINT PRIMARY KEY,
                                          relname        TEXT NOT NULL,
                                          relnamespace   BIGINT NOT NULL REFERENCES mock_pg_catalog.pg_namespace(oid)
);

-- pg_subscription
CREATE TABLE mock_pg_catalog.pg_subscription (
                                                 oid        BIGINT PRIMARY KEY,
                                                 subname    TEXT NOT NULL
);

-- pg_subscription_rel
CREATE TABLE mock_pg_catalog.pg_subscription_rel (
                                                     srsubid    BIGINT NOT NULL REFERENCES mock_pg_catalog.pg_subscription(oid),
                                                     srrelid    BIGINT NOT NULL REFERENCES mock_pg_catalog.pg_class(oid)
);

CREATE TABLE IF NOT EXISTS mock_pg_catalog.pg_publication_tables (
                                                                     pubname     text NOT NULL,
                                                                     schemaname  text NOT NULL,
                                                                     tablename   text NOT NULL
);

-- pg_namespace
INSERT INTO mock_pg_catalog.pg_namespace (oid, nspname) VALUES
    (10, 'claims');

-- pg_class (tables)
INSERT INTO mock_pg_catalog.pg_class (oid, relname, relnamespace) VALUES
                                                                      (100, 'client', 10),
                                                                      (101, 'claim', 10),
                                                                      (102, 'replication_summary', 10);

-- pg_subscription
INSERT INTO mock_pg_catalog.pg_subscription (oid, subname) VALUES
    (1, 'claims_reporting_service_sub');

-- pg_subscription_rel (subscription → table links)
INSERT INTO mock_pg_catalog.pg_subscription_rel (srsubid, srrelid) VALUES
                                                                       (1, 100),
                                                                       (1, 101),
                                                                       (1, 102);
ALTER DATABASE test
SET search_path = mock_pg_catalog, public, pg_catalog;