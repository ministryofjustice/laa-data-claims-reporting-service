# laa-data-claims-reporting-service
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-data-claims-reporting-service/badge?v=2)](https://github-community.service.justice.gov.uk/repository-standards/laa-data-claims-reporting-service)

This is a Java based Spring Boot application hosted on [MOJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/concepts/what-is-the-cloud-platform.html).

## Overview

Java Spring Boot based application that generates reports from the claims database and stores them in GLAD S3 Bucket.

The project was created from [this GitHub Template](https://github.com/ministryofjustice/laa-spring-boot-microservice-template)

### Project Structure
Includes the following subprojects:

- `laa-data-claims-reporting-service/laa-data-claims-reporting-service` - Generates reports from replica of claims database

## Add GitHub Token

1. Generate a GitHub PAT (Personal Access Token) to access the required plugin via <https://github.com/settings/tokens>.
2. Specify the Note field, for example: "Token to allow access to LAA Gradle plugin".
3. If you don't already have a `gradle.properties` file, create one at `~/.gradle/gradle.properties`.
4. Add the following properties to `~/.gradle/gradle.properties`, replacing the placeholder values:

    - `project.ext.gitPackageUser=YOUR_GITHUB_USERNAME`
    - `project.ext.gitPackageKey=PAT_CREATED_ABOVE`

5. Return to GitHub and authorise MOJ for SSO.

## Build And Run Application

### Build application
`./gradlew clean build`

### Run integration tests

`./gradlew integrationTest`

## Logging

This application uses **ECS (Elastic Common Schema) structured logging** for production environments and console logging for local development.

### Structured Logging (Default/Production)

By default, the application outputs logs in ECS JSON format with distributed tracing support:

```json
{
  "@timestamp": "2026-05-11T16:25:18.992904Z",
  "ecs": {
    "version": "8.11"
  },
  "log": {
    "level": "INFO",
    "logger": "uk.gov.justice.laa.dstew.claimsreports.runner.ClaimsReportingServiceRunner"
  },
  "message": "Generated REPXXX report with 1234 rows",
  "process": {
    "pid": 1,
    "thread": {
      "name": "main"
    }
  },
  "service": {
    "environment": "staging",
    "name": "LAA Claims Data Reporting Application",
    "node": {
      "name": "laa-data-claims-reporting-service-xxxxx"
    },
    "version": "1.0.0"
  },
  "spanId": "fe4586c5fd5f7021",
  "traceId": "69aaffee8d19869cfe4586c5fd5f7021"
}
```

**Key fields:**
- `@timestamp`: ISO 8601 timestamp
- `log.level`: Log level (INFO, DEBUG, WARN, ERROR)
- `message`: Log message
- `service.name`: Application name from spring.application.name
- `service.version`: Application version from gradle.properties
- `service.environment`: Active Spring profile
- `service.node.name`: Hostname (pod name in Kubernetes)
- `traceId` / `spanId`: Distributed tracing correlation IDs

### Local Development Logging

When running with the `local` profile, logs use a human-readable console format:

```
2026-05-11T16:25:18.992+01:00 [main] [69aaffee8d19869cfe4586c5fd5f7021/fe4586c5fd5f7021] INFO  u.g.j.l.d.c.runner.ClaimsReportingServiceRunner - Starting claims report generation
```

This format includes trace/span IDs for correlation while remaining easy to read during development.

### Run locally using Minikube
```
brew install minikube
minikube start
docker build -t my-app:latest .
minikube image load my-app:latest
kubectl get all
kubectl logs <pod-name>
```

### Run locally using docker-compose
This will spin up an instance of postgres and localstack to enable fuller testing
```
docker-compose up
```

This will spin down the service and delete volumes if you e.g. need to rebuild the database
```
docker compose down -v
```

### To view the files uploaded to the localstack S3 bucket you can use commands such as the follows:
```
aws --endpoint-url=http://localhost:4566 s3 ls s3://test-bucket/reports/
```

If it asks you to run `aws configure`, just set the config keys to any value, localstack will ignore them. They just have to be non-empty.

### Updating Helm
When making updates to Helm, it is possible to lint your changes to ensure no errors
```
helm lint .helm/data-claims-reporting-service/Chart.yaml
helm install my-app ./.helm/data-claims-reporting-service -f .helm/data-claims-reporting-service/values/local.yaml
```

## Additional Information
### Helm
- Updates to helm template must include a change to the `Chart.yaml` version number.
- The Helm version must be bumped to trigger a redeployment, because Helm uses that version to determine whether a release has changed.

### CronJob
- The application runs via a CronJob. This job is currently scheduled to run once per day, at 5am.
- Not all reports run every day.
- To manually run a CronJob at another time - if you have permission to access kubernetes - run
```
kubectl -n {namespace} create job {give a job name here} --from=cronjob/{cronjob to copy}
```
where `namespace` is the Kubernetes namespace, `give a job name here` is some memorable unique name, 
and `cronjob to copy` is one of the entries from the list of cronjobs - on `uat` there should be one for each active pull request branch. 
On other systems it should just be `laa-data-claims-reporting-service`

#### Forcing monthly reports to run
##### Long-term basis
If you need to run REP000 on a date that is not the 21st of the month for many days or on production, you can force it by following these steps:
- Edit the AWS secret laa-data-claims-reporting-service-secrets: add a key called feature-force-run-rep000 - if not already there - and set it to true.
- Delete the existing K8s secret via kubectl so that the new value is picked up from the updated AWS secret.
- The next time the CronJob runs, REP000 will run regardless of the date.
- To reset this, delete the key from the AWS secret or set it to false and delete the K8s secret again.

##### One-off basis
If on dev or staging you just need to run REP000 as a one-off, you can create a temporary Kubernetes job 
```
kubectl -n {namespace} create job {give a job name here} --from=cronjob/{cronjob to copy} --dry-run=client -o yaml \
| kubectl -n {namespace} set env --local -f - FORCE_RUN_REP000="true" -o yaml \
| kubectl -n {namespace} apply -f -
```

# Releases

This project uses [release-please](https://github.com/googleapis/release-please)
to automate versioning, changelog generation, and GitHub releases.

### How It Works

When commits are merged into `main`, release-please:

- determines the next semantic version
- updates `pom.xml`
- updates `CHANGELOG.md`
- creates a release pull request
- creates a GitHub release after the PR is merged

---

When the Release PR is merged:
- a Git tag is created
- a GitHub Release is published
- the new version becomes available

---

### Commit Message Format

All commits should follow this format:

```text
<type>: <short summary>
```

Examples:

```text
feat: add webhook retry support
fix: prevent duplicate event processing
docs: update installation instructions
```

---

### Commit Types

| Type       | Description                         | Version Impact |
|------------|-------------------------------------|----------------|
| `feat`     | Introduces a new feature            | Minor          |
| `fix`      | Fixes a bug                         | Patch          |
| `feat!`    | Breaking feature change             | Major          |
| `docs`     | Documentation updates only          | None           |
| `refactor` | Internal code restructuring         | None           |
| `test`     | Adding or updating tests            | None           |
| `chore`    | Maintenance tasks                   | None           |
| `ci`       | CI/CD pipeline changes              | None           |
| `build`    | Build tooling or dependency changes | None           |
| `perf`     | Performance improvements            | Patch          |
| `revert`   | Reverts a previous commit           | Depends        |

---
## Semantic Versioning Examples

### Patch Release

```text
fix: handle null response from API
```

Results in:

```text
1.4.0 → 1.4.1
```

---

### Minor Release

```text
feat: add OAuth authentication
```

Results in:

```text
1.4.0 → 1.5.0
```

---

### Major Release

```text
feat!: remove deprecated REST endpoints
```

or:

```text
feat: remove deprecated REST endpoints

BREAKING CHANGE: legacy REST API removed
```

Results in:

```text
1.4.0 → 2.0.0
```
---

## Debugging feature flags
You can create the cronjob with the `FEATURE_UPLOAD-UTF-8-FAILURES-TO-S3="true"` if you are debugging an issue with UTF-8 validation and need to see the invalid document.
When enabled it will attempt to upload the report to the `reports/errors` folder.
You should not turn this on permanently it is intended as just a debug helper.  

## Pre commit hooks
- Pre commit hooks have been set up on this repository to ensure no accidental commits of secrets, keys etc. Provided by DevSecOps https://github.com/ministryofjustice/devsecops-hooks

Install the hook with the following command:
```text
pre-commit install
```