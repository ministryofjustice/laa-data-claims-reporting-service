# Copilot Instructions

This document helps Copilot work effectively in the LAA Claims Data Reporting Service repository.

## Build & Test Commands

See [README.md](../README.md#build-and-run-application) for setup and local development (docker-compose, Minikube, Helm).

### Quick Commands
```bash
./gradlew clean build          # Full build
./gradlew test                 # Unit tests
./gradlew test --tests ClassName  # Single test (e.g., Report000ServiceTest)
./gradlew integrationTest      # Integration tests
./gradlew check                # All checks (tests, spotbugs, checkstyle, jacoco)
./gradlew spotbugsMain         # Static analysis
./gradlew jacocoTestReport     # Coverage (output: build/reports/jacoco/test/html/)
./gradlew bootRun              # Run locally with 'local' profile
```

## Architecture

### High-Level Overview
This is a **Spring Boot batch application** that generates claims reports from a PostgreSQL database replica and stores them in AWS S3. It runs as a **Kubernetes CronJob** (currently scheduled for 5 AM daily) and does not expose a web interface (WebApplicationType.NONE).

### Key Components

**Entry Point**
- `ClaimsReportingServiceApplication` - Spring Boot entry point (non-web application)
- `ClaimsReportingServiceRunner` - Orchestrates report generation workflow via ApplicationRunner

**Report Generation Flow**
1. Check database replication health via `ReplicationHealthCheckService`
2. For each report service (Report000Service, Report002Service, etc.):
   - Refresh materialised view for the report
   - Generate CSV via `CsvCreationService`
   - Validate CSV via `CsvFileValidator`
   - Upload to S3 via `S3ClientWrapper`
3. Track metrics and logging throughout

**Service Layer**
- `AbstractReportService` - Base class for all report generators; handles materialised view refresh, CSV generation, validation, and S3 upload
- `ReportXXXService` (Report000Service, Report002Service, etc.) - Concrete implementations for specific reports
- `CsvCreationService` - Generates CSV files using Spring's JdbcTemplate with streaming/buffering for large datasets
- `CsvFileValidator` - Validates CSV files before S3 upload
- `ReplicationHealthCheckService` - Verifies database replication is synced with source DB
- `DatabaseStatisticService` - Retrieves database statistics
- `S3ClientWrapper` - Wraps AWS SDK S3 operations; supports both real AWS and localstack

**Configuration & Observability**
- `PrometheusConfiguration` - Defines custom Prometheus metrics (REPORT_SUCCESSFUL, REPORT_FAILED, REPORT_SKIPPED, REPLICATION_HEALTH_CHECK_STATUS, etc.)
- `MetricsHandler` - Records metrics for report execution
- `LocalstackS3Config` - Configures S3 client for localstack in local/dev environments
- `LogSanitiser` - Sanitizes logs to prevent PII exposure

**Database**
- Flyway migrations in `src/main/resources/db/migration/`
- PostgreSQL schemas: `claims` (with replication views)
- Replication user credentials injected via environment variables

### Test Structure
- `src/test/java` - Unit tests for services, config, utilities
- `src/integrationTest/java` - Integration tests using TestContainers (PostgreSQL + Localstack)
- Test data files in `src/integrationTest/resources/db/testdata/` and `sql/`
- Expected CSV files for assertions in `src/integrationTest/resources/expected_csv_files/reports/`

## Key Conventions

### Package Structure
- `service/` - Business logic; AbstractReportService base class with Report*Service implementations
- `repository/` - Database access layer; ReplicationMetadataRepository with Postgres and Local implementations
- `config/` - Spring configuration, metrics, AWS clients
- `runner/` - ApplicationRunner for batch orchestration
- `dto/` - Data transfer objects (ReplicationHealthReport, ReplicationSummary, etc.)
- `utils/` - Utilities (LogSanitiser, etc.)
- `exception/` - Custom exceptions (CsvCreationException, etc.)

### Logging
- **Production (default)**: ECS structured logging in JSON format with distributed tracing (trace ID, span ID)
- **Local Development**: Human-readable console format with trace IDs (activated by `local` profile)
- Use `@Slf4j` from Lombok; logs are automatically structured
- Use `LogSanitiser.sanitise()` to redact sensitive data before logging
- MDC is populated with trace ID for correlation

### Testing Patterns
- Use `@SpringBootTest` with TestContainers for integration tests
- Mock external dependencies (S3, databases) using TestContainers
- Use fixture data from `src/integrationTest/resources/` for consistent test data
- Unit tests use mocks; integration tests use real Spring context

### Report Generation
- Each report is a subclass of `AbstractReportService` (Report000Service, Report002Service, etc.)
- Reports are generated as CSV files with UTF-8 encoding
- Materialized views are refreshed before report generation
- Reports are uploaded to S3 with timestamped filenames
- Feature flags control whether certain reports run (FORCE_RUN_REP000, FORCE_RUN_REP002, etc.)

### S3 Naming Conventions
- Production: AWS S3 bucket managed by Cloud Platform
- Local/Dev: Localstack (http://localhost:4566)
- Report files: `reports/{report_code}/{date}_{time}.csv`
- Error uploads (when FEATURE_UPLOAD-UTF-8-FAILURES-TO-S3=true): `reports/errors/`

### Dependency Injection
- Use `@RequiredArgsConstructor` (Lombok) for constructor injection
- All dependencies are final fields
- Services are annotated with `@Service`, `@Component`, or `@Configuration`

### Database Patterns
- Use `JdbcTemplate` for database access (not JPA/Hibernate)
- Transactions managed with `@Transactional` on service methods
- Materialized views refreshed via SQL: `REFRESH MATERIALIZED VIEW view_name`
- Use parameterized queries to prevent SQL injection

### Lombok
- `@Slf4j` for logging (creates `log` field)
- `@RequiredArgsConstructor` for constructor injection
- `@AllArgsConstructor` for test fixtures
- `@Value` for immutable DTOs

### Helm & Deployment
See [README.md](../README.md#helm) for Helm setup, versioning, and deployment details.

### Release Management
See [README.md](../README.md#releases) for release-please configuration and commit message format.

### Code Quality
- SpotBugs (static analysis) via Gradle plugin; config in `spotbugs-exclude.xml`
- Checkstyle linting via LAA Spring Boot Gradle plugin
- Jacoco code coverage verification; main application class is excluded
- Pre-commit hooks enabled to prevent accidental secret commits (from DevSecOps) — see [README.md](../README.md#pre-commit-hooks)

### Language
- **Use British English spelling and terminology** in all code comments, documentation, commit messages, and variable names (e.g., `colour` not `color`, `localise` not `localize`, `organise` not `organize`)

### GitHub Token Setup
See [README.md](../README.md#add-github-token) for GitHub PAT setup required by the LAA Gradle plugin.

## Environment Variables

Key configuration via environment variables (or AWS Secrets in production):

- `DB_HOST` - Database host (default: localhost)
- `ROOT_LOGGING_LEVEL`, `SPRING_LOGGING_LEVEL`, `APP_LOGGING_LEVEL` - Log levels
- `FEATURE_IGNORE_REPLICATION_ROWCOUNT_MISMATCH` - Skip rowcount verification on replication
- `FORCE_RUN_REP000`, `FORCE_RUN_REP002`, etc. - Force specific reports to run
- `FEATURE_UPLOAD-UTF-8-FAILURES-TO-S3` - Debug helper to upload invalid documents to S3
- `HOSTNAME` - Node name in logs (auto-populated in Kubernetes)
