{{/*
  Define environment variables that can be "included" in cronjob.yaml, deployment.yaml etc
*/}}
{{- define "data-claims-reporting.dbConnectionDetails" }}
{{/*
Extract DB environment variables from rds-postgresql-instance-output secret
*/}}
- name: DB_NAME
  valueFrom:
    secretKeyRef:
      name: rds-postgresql-instance-output
      key: database_name
- name: SPRING_DATASOURCE_USERNAME
  valueFrom:
    secretKeyRef:
      name: rds-postgresql-instance-output
      key: database_username
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: rds-postgresql-instance-output
      key: database_password
- name: DB_HOST
  valueFrom:
    secretKeyRef:
      name: rds-postgresql-instance-output
      key: rds_instance_address
- name: SPRING_DATASOURCE_URL
  value: "jdbc:postgresql://$(DB_HOST):5432/$(DB_NAME)"
{{/*
Extract other environment variables from laa-data-claims-reporting-service-secrets secret
*/}}
- name: SPRING_FLYWAY_PLACEHOLDERS_REPORTING_USERNAME
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: reporting-username
- name: SPRING_FLYWAY_PLACEHOLDERS_REPORTING_PASSWORD
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: reporting-password
- name: SPRING_FLYWAY_PLACEHOLDERS_REPLICATION_SOURCE_DB_URL
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: replication-source-db-url
- name: SPRING_FLYWAY_PLACEHOLDERS_REPLICATION_SOURCE_DB_NAME
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: replication-source-db-name
- name: FEATURE_IGNORE_REPLICATION_ROWCOUNT_MISMATCH
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: feature-ignore-replication-rowcount-mismatch
{{- end }}