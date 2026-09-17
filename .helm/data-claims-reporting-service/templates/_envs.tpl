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
- name: FORCE_RUN_REP000
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: feature-force-run-rep000
      optional: true
- name: FEATURE_ENABLE_REP012_XLSX
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: feature-enable-rep012-xlsx
      optional: true
- name: FEATURE_ENABLE_REP012_SHAREPOINT_UPLOAD
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: feature-enable-rep012-sharepoint-upload
      optional: true
- name: SHAREPOINT_TENANT_ID
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-tenant-id
      optional: true
- name: SHAREPOINT_CLIENT_ID
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-client-id
      optional: true
- name: SHAREPOINT_SITE_HOST
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-site-host
      optional: true
- name: SHAREPOINT_SITE_PATH
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-site-path
      optional: true
- name: SHAREPOINT_DRIVE_NAME
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-drive-name
      optional: true
- name: SHAREPOINT_FOLDER_PATH
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-folder-path
      optional: true
- name: SHAREPOINT_UPLOAD_RETRY_COUNT
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-upload-retry-count
      optional: true
- name: SHAREPOINT_CLIENT_SECRET
  valueFrom:
    secretKeyRef:
      name: laa-data-claims-reporting-service-secrets
      key: sharepoint-client-secret
      optional: true
{{- end }}