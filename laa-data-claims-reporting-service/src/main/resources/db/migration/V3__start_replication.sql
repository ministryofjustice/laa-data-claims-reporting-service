GRANT USAGE ON SCHEMA claims TO reporting_user;

GRANT SELECT ON ALL TABLES IN SCHEMA claims TO reporting_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA claims GRANT SELECT ON TABLES TO reporting_user;

CREATE SUBSCRIPTION claims_reporting_service_sub
CONNECTION 'host=${replication_source_db_url} port=5432 dbname=${replication_source_db_name} user=${reporting_username} password=${reporting_password}'
PUBLICATION claims_reporting_service_pub
WITH (copy_data = true);