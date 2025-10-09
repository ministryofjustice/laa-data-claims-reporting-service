CREATE USER ${reporting_username} WITH PASSWORD '${reporting_password}';

CREATE SUBSCRIPTION my_subscription
CONNECTION 'host=${replication_source_db_url} port=5432 dbname=${replication_source_db_name} user=${reporting_username} password=${reporting_password}'
PUBLICATION claims_reporting_service_pub
WITH (copy_data = true);