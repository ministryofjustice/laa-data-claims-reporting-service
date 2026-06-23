package uk.gov.justice.laa.dstew.claimsreports.runner;

import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId.REPLICATION_HEALTH_CHECK_STATUS;
import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_FAILED;
import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;
import uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService;


/**
 * The ClaimsReportingServiceRunner class is responsible for orchestrating the report generation process
 * for a collection of report services that extend the AbstractReportService.
 *
 * <p>
 * This class implements the ApplicationRunner interface, ensuring that the report generation is triggered
 * once the application context is fully initialized.
 *
 * <p>
 * Dependencies:
 * - A list of AbstractReportService implementations, where each implementation is tasked with handling
 *   the specific logic for refreshing materialized views and generating reports.
 *
 * <p>
 * Workflow:
 * - The `run` method is invoked at application startup and calls the internal `ensureReplicationHealthy` method
 * - This checks if the database replication is healthy and all the data is in sync with the source DB.
 * - If so, it calls the `generateReports` method, if not, it logs an error, throws an exception and terminates the process.
 * - The `generateReports` method iterates through the provided list of report services, ensuring each one
 *   performs the refresh of its associated materialized view and generates the corresponding report.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimsReportingServiceRunner implements ApplicationRunner {

  @Value("${feature.ignore-replication-rowcount-mismatch:false}")
  private boolean ignoreRowCountMismatch;
  private final ReplicationHealthCheckService replicationHealthCheckService;
  //Spring will auto-inject all services that implement the AbstractReportService
  private final List<AbstractReportService> reportServices;

  private final MetricsHandler metricsHandler;
  private final DatabaseStatisticService databaseStatisticService;

  @Override
  public void run(ApplicationArguments args) {
    if (ensureReplicationHealthy()) {
      generateReports();
    } else {
      log.error("Replication health check failed, reports not generated.");
      markAllReportsFailedDueToReplication();
    }

    databaseStatisticService.setDatabaseMetrics();
    metricsHandler.pushDatabaseHealthMetrics();

  }

  /**
   * Ensures the health of the replication system before proceeding with report generation.
   *
   * <p>This method performs a replication health check by utilizing the
   * {@code replicationHealthCheckService}. If the replication is determined to be
   * unhealthy, an {@code IllegalStateException} is thrown to abort the operation.
   *
   * <p>Logging is performed to indicate the status of the health check, including
   * detailed error information if the check fails.
   *
   * <p>@throws IllegalStateException if the replication health check fails
   */
  private static final long REPLICATION_HEALTH_CHECK_FAILED = 0;
  private static final long REPLICATION_HEALTH_CHECK_PASSED = 1;

  private boolean ensureReplicationHealthy() {
    log.info("Checking replication health before generating reports...");

    ReplicationHealthReport report = replicationHealthCheckService.checkReplicationHealth();
    boolean replicationHealthy = true;

    if (!report.isHealthy()) {
      log.error("Replication health check failed:\n{}", report.summary());

      // Even if the overall replication is unhealthy, we want to continue if
      // ignoreRowCountMismatch is set and basic WAL check passed.
      if (ignoreRowCountMismatch && report.isWalLsnOk()) {
        log.info("Ignoring Row Count Mismatch because ignoreRowCountMismatch is set to true and WAL LSN check has passed");
      } else {
        replicationHealthy = false;
      }
    }

    metricsHandler.setCustomMetric(
            REPLICATION_HEALTH_CHECK_STATUS,
            replicationHealthy ? REPLICATION_HEALTH_CHECK_PASSED : REPLICATION_HEALTH_CHECK_FAILED
    );
    metricsHandler.pushReplicationHealthMetric();

    if (!replicationHealthy) {
      return false;
    }

    log.info("Replication health confirmed — proceeding with report generation.");
    return true;
  }

  /**
   * Generates reports by iterating through a list of report services, performing the following tasks:
   * - Refreshing the associated materialized view for each report service.
   * - Generating the report through the report service logic.
   *
   * <p>This method ensures that errors during the generation process for one service do not interfere
   * with the other services. If an exception occurs during the execution of a specific report service,
   * it logs an error message containing the name of the service and the details of the exception.
   *
   * <p>The implementation assumes that the report services extend from the AbstractReportService base class,
   * which provides the necessary methods for refreshing materialized views and generating reports.
   */
  @SuppressFBWarnings(value = "SECCRLFLOG", justification = "Both arguments sanitised via sanitise() to strip CRLF before logging")
  private void generateReports() {
    log.info("Generating {} reports...", reportServices.size());
    for (AbstractReportService service : reportServices) {
      metricsHandler.resetCustomMetrics();
      var startTime = System.currentTimeMillis();
      try {
        service.refreshDataSource();
        service.generateReport();
      } catch (Exception e) {
        String safeService = sanitise(service.getClass().getSimpleName());
        String safeMessage = sanitise(e.getMessage());
        log.error("Report generation failed for {}: {}", safeService, safeMessage, e);
        metricsHandler.setCustomMetric(CustomMetricId.REPORT_SUCCESSFUL, REPORT_FAILED);
      } finally {
        var reportDuration = System.currentTimeMillis() - startTime;
        metricsHandler.setCustomMetric(CustomMetricId.REPORT_TOTAL_TIME_MS, reportDuration);
        log.info("Report generation for report {} took {} ms ({} s)", sanitise(service.getReportName()), reportDuration, reportDuration / 1000);
        metricsHandler.pushReportMetrics(service.getReportName());
      }
    }
  }

  private void markAllReportsFailedDueToReplication() {
    log.info("Marking all report metrics as failed due to replication health check failure.");
    for (AbstractReportService service : reportServices) {
      String reportName = service.getReportName();
      metricsHandler.resetCustomMetrics();
      metricsHandler.setCustomMetric(CustomMetricId.REPORT_SUCCESSFUL, REPORT_FAILED);
      metricsHandler.pushReportMetrics(reportName);
    }
  }

}