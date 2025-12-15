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
- The Helm version must be bumped to trigger a redeploy, because Helm uses that version to determine whether a release has changed.

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

If you need to run REP000 on a date that is not the 21st of the month, you should create a cronjob with the `FORCE_RUN_REP000` flag set to true.