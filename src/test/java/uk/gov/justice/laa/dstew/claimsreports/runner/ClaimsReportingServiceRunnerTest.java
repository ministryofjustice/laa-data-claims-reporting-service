package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_FAILED;
import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId.REPLICATION_HEALTH_CHECK_STATUS;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.test.util.ReflectionTestUtils;

import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;
import uk.gov.justice.laa.dstew.claimsreports.service.DatabaseStatisticService;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService;

class ClaimsReportingServiceRunnerTest {

  @Mock
  private AbstractReportService reportService1;

  @Mock
  private AbstractReportService reportService2;

  @Mock
  private ReplicationHealthCheckService replicationHealthCheckService;

  @Mock
  private ApplicationArguments applicationArguments;

  @Mock
  private MetricsHandler metricsHandler;

  @Mock
  private DatabaseStatisticService databaseStatisticService;

  private ClaimsReportingServiceRunner runner;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Inject a list of mocked report services
    runner = new ClaimsReportingServiceRunner(
            replicationHealthCheckService,
            List.of(reportService1, reportService2),
            metricsHandler,
            databaseStatisticService
    );

    // Default behaviour: replication is healthy
    ReplicationHealthReport healthyReport = new ReplicationHealthReport(LocalDate.now());
    healthyReport.setHealthy(true);
    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(healthyReport);
  }

  @Test
  void shouldInvokeGenerateReportsOnAllServices() {
    // Call the run method
    runner.run(applicationArguments);

    // Verify that refreshDataSource and generateReport were called on each service
    verify(reportService1).refreshDataSource();
    verify(reportService1).generateReport();

    verify(reportService2).refreshDataSource();
    verify(reportService2).generateReport();
  }

  @Test
  void shouldResetAndPushMetricsForEachReport() {

    when(reportService1.getReportName()).thenReturn("report1");
    when(reportService2.getReportName()).thenReturn("report2");

    runner.run(applicationArguments);

    // Each report should reset its metrics
    verify(metricsHandler, times(2)).resetCustomMetrics();

    // And push metrics using the report name
    verify(metricsHandler).pushReportMetrics("report1");
    verify(metricsHandler).pushReportMetrics("report2");
  }

  @Test
  void shouldPushHealthyReplicationMetricWhenReplicationIsHealthy() {
    runner.run(applicationArguments);

    // When replication is healthy we push metric value = 1
    verify(metricsHandler).setCustomMetric(REPLICATION_HEALTH_CHECK_STATUS, 1);
    verify(metricsHandler).pushReplicationHealthMetric();
  }

  @Test
  void shouldHandleEmptyServiceList() {
    // Create runner with empty list of report services
    ClaimsReportingServiceRunner emptyRunner =
            new ClaimsReportingServiceRunner(replicationHealthCheckService, List.of(), metricsHandler, databaseStatisticService);

    // Should not throw any exceptions
    assertThatCode(() -> emptyRunner.run(applicationArguments))
            .doesNotThrowAnyException();
  }

  @Test
  void shouldContinueWhenOneServiceFails() {
    // Make the first service throw an exception when refreshing
    doThrow(new RuntimeException("Refresh failed")).when(reportService1).refreshDataSource();

    // Call run (should continue to second service)
    runner.run(applicationArguments);

    // First service was called but generateReport should not run
    verify(reportService1).refreshDataSource();
    verify(reportService1, never()).generateReport();

    // Second service should still run normally
    verify(reportService2).refreshDataSource();
    verify(reportService2).generateReport();

    // Failure metric should be recorded
    verify(metricsHandler).setCustomMetric(CustomMetricId.REPORT_SUCCESSFUL, REPORT_FAILED);
  }

  @Test
  void shouldAbortWhenReplicationIsUnhealthy() {
    // Arrange: replication health check fails
    ReplicationHealthReport unhealthy = new ReplicationHealthReport(LocalDate.now());
    unhealthy.setHealthy(false);
    unhealthy.setWalLsnOk(false);
    unhealthy.addFailure("claim", "Count mismatch");

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthy);
    when(reportService1.getReportName()).thenReturn("report1");
    when(reportService2.getReportName()).thenReturn("report2");

    // Run
    runner.run(applicationArguments);

    // Verify that reports were NOT generated
    verify(reportService1, never()).refreshDataSource();
    verify(reportService1, never()).generateReport();
    verify(reportService2, never()).refreshDataSource();
    verify(reportService2, never()).generateReport();
  }

  @Test
  void shouldMarkAllReportsFailedWhenReplicationIsUnhealthy() {
    // Arrange: replication fails
    ReplicationHealthReport unhealthy = new ReplicationHealthReport(LocalDate.now());
    unhealthy.setHealthy(false);
    unhealthy.setWalLsnOk(false);

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthy);
    when(reportService1.getReportName()).thenReturn("report1");
    when(reportService2.getReportName()).thenReturn("report2");

    // Run
    runner.run(applicationArguments);

    // Replication metric should be set to failure
    verify(metricsHandler).setCustomMetric(REPLICATION_HEALTH_CHECK_STATUS, 0);
    verify(metricsHandler).pushReplicationHealthMetric();

    // Each report should be marked as failed
    verify(metricsHandler, times(2)).resetCustomMetrics();
    verify(metricsHandler, times(2))
            .setCustomMetric(eq(CustomMetricId.REPORT_SUCCESSFUL), eq((double) REPORT_FAILED));

    // Metrics pushed per report
    verify(metricsHandler).pushReportMetrics("report1");
    verify(metricsHandler).pushReportMetrics("report2");

    // Ensure reports were not executed
    verify(reportService1, never()).refreshDataSource();
    verify(reportService1, never()).generateReport();
    verify(reportService2, never()).refreshDataSource();
    verify(reportService2, never()).generateReport();
  }

  @Test
  void shouldAlwaysCheckReplicationHealthBeforeGeneratingReports() {
    runner.run(applicationArguments);

    // Replication health must always be checked first
    verify(replicationHealthCheckService, times(1)).checkReplicationHealth();
  }

  @Test
  void shouldContinueWhenIgnoreMismatchTrueAndWalLsnOK() {
    // given replication is unhealthy but WAL is OK
    ReplicationHealthReport unhealthyButSafe = new ReplicationHealthReport(LocalDate.now());
    unhealthyButSafe.setHealthy(false);
    unhealthyButSafe.setWalLsnOk(true);

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthyButSafe);

    runner = new ClaimsReportingServiceRunner(
            replicationHealthCheckService,
            List.of(reportService1, reportService2),
            metricsHandler,
            databaseStatisticService
    );

    // enable ignoreRowCountMismatch feature flag
    ReflectionTestUtils.setField(runner, "ignoreRowCountMismatch", true);

    // when
    runner.run(applicationArguments);

    // replication metric should still be treated as healthy
    verify(metricsHandler).setCustomMetric(REPLICATION_HEALTH_CHECK_STATUS, 1);
    verify(metricsHandler).pushReplicationHealthMetric();

    // reports should still run
    verify(reportService1).refreshDataSource();
    verify(reportService1).generateReport();
    verify(reportService2).refreshDataSource();
    verify(reportService2).generateReport();
  }

  @Test
  void shouldAbortWhenIgnoreMismatchTrueButWalLsnNotOK() {
    // replication unhealthy AND WAL check failed
    ReplicationHealthReport unhealthy = new ReplicationHealthReport(LocalDate.now());
    unhealthy.setHealthy(false);
    unhealthy.setWalLsnOk(false);

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthy);
    when(reportService1.getReportName()).thenReturn("report1");
    when(reportService2.getReportName()).thenReturn("report2");

    runner = new ClaimsReportingServiceRunner(
            replicationHealthCheckService,
            List.of(reportService1, reportService2),
            metricsHandler,
            databaseStatisticService
    );

    // enable ignoreRowCountMismatch
    ReflectionTestUtils.setField(runner, "ignoreRowCountMismatch", true);

    runner.run(applicationArguments);

    // replication failure metric should be pushed
    verify(metricsHandler).setCustomMetric(REPLICATION_HEALTH_CHECK_STATUS, 0);
    verify(metricsHandler).pushReplicationHealthMetric();

    // all reports should be marked as failed
    verify(metricsHandler, times(2))
            .setCustomMetric(eq(CustomMetricId.REPORT_SUCCESSFUL), eq((double) REPORT_FAILED));

    verify(metricsHandler).pushReportMetrics("report1");
    verify(metricsHandler).pushReportMetrics("report2");

    // reports themselves should not run
    verify(reportService1, never()).refreshDataSource();
    verify(reportService1, never()).generateReport();
    verify(reportService2, never()).refreshDataSource();
    verify(reportService2, never()).generateReport();
  }

  @Test
  void shouldPublishDbHealthStats() {
    runner.run(applicationArguments);

    verify(databaseStatisticService).setDatabaseMetrics();
    verify(metricsHandler).pushDatabaseHealthMetrics();
  }
}