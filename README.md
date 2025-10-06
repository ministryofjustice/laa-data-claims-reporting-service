# laa-data-claims-reporting-service
[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/laa-data-claims-reporting-service/badge)](https://github-community.service.justice.gov.uk/repository-standards/laa-data-claims-reporting-service)

This is a Java based Spring Boot application hosted on [MOJ Cloud Platform](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/concepts/what-is-the-cloud-platform.html).

## Overview

Java Spring Boot based application that generates reports from the claims database and stores them in GLAD S3 Bucket.

The project was created from [this Github Template](https://github.com/ministryofjustice/laa-spring-boot-microservice-template)

### Project Structure
Includes the following subprojects:

- `laa-data-claims-reporting-service/laa-data-claims-reporting-service` - Generates reports from replica of claims database

## Add GitHub Token
1.	Generate a Github PAT (Personal Access Token) to access the required plugin, via https://github.com/settings/tokens
2.	Specify the Note field, e.g. “Token to allow access to LAA Gradle plugin”
3.  If you haven’t got a gradle.properties file create one under `~/.gradle/gradle.properties`
4.  Add the following properties to `~/.gradle/gradle.properties` and replace the placeholder values as follows:
  - `project.ext.gitPackageUser` = YOUR_GITHUB_USERNAME
  - `project.ext.gitPackageKey` = PAT_CREATED_ABOVE

5.	Go back to Github to authorize MOJ for SSO

## Build And Run Application

### Build application
`./gradlew clean build`

### Run integration tests

`./gradlew integrationTest`


## Additional Information
### Helm
- Updates to helm template must include a change to the `Chart.yaml` version number.
- The Helm version must be bumped to trigger a redeploy, because Helm uses that version to determine whether a release has changed.

### CronJob
- The application runs via a CronJob. This job is currently scheduled to run once per day, at 9am.