CREATE TABLE replication_summary (
                                     table_name text NOT NULL,
                                     summary_date date NOT NULL,
                                     record_count bigint NOT NULL,
                                     updated_count bigint NOT NULL,
                                     wal_lsn pg_lsn NOT NULL,
                                     created_on timestamptz NOT NULL,
                                     CONSTRAINT pk_replication_summary PRIMARY KEY (table_name, summary_date)
);