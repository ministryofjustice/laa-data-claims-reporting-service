package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;

import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;
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

  private ClaimsReportingServiceRunner runner;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Inject a list of mocked report services
    runner = new ClaimsReportingServiceRunner(replicationHealthCheckService, List.of(reportService1, reportService2), metricsHandler);
    // Default: replication is healthy
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

    verify(metricsHandler, times(2)).resetCustomMetrics();
    verify(metricsHandler).pushReportMetrics("report1");
    verify(metricsHandler).pushReportMetrics("report2");
  }

  @Test
  void shouldHandleEmptyServiceList() {
    // Create runner with empty list
    ClaimsReportingServiceRunner emptyRunner = new ClaimsReportingServiceRunner(replicationHealthCheckService, List.of(), metricsHandler);

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

    // First service was called
    verify(reportService1).refreshDataSource();
    verify(reportService1, never()).generateReport(); // generateReport skipped because refresh failed

    // Second service should still run
    verify(reportService2).refreshDataSource();
    verify(reportService2).generateReport();
  }

  @Test
  void shouldAbortWhenReplicationIsUnhealthy() {
    // Arrange
    ReplicationHealthReport unhealthy = new ReplicationHealthReport(LocalDate.now());
    unhealthy.setHealthy(false);
    unhealthy.addFailure("claim", "Count mismatch");
    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthy);

    // Verify that reports were NOT generated
    verifyNoInteractions(reportService1);
    verifyNoInteractions(reportService2);
  }

  @Test
  void shouldAlwaysCheckReplicationHealthBeforeGeneratingReports() {
    runner.run(applicationArguments);

    verify(replicationHealthCheckService, times(1)).checkReplicationHealth();
  }

  @Test
  void shouldContinueWhenIgnoreMismatchTrueAndWalLsnOK() {
    // given
    ReplicationHealthReport unhealthyButSafe = new ReplicationHealthReport(LocalDate.now());
    unhealthyButSafe.setHealthy(false);
    unhealthyButSafe.setWalLsnOk(true);

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthyButSafe);

    runner = new ClaimsReportingServiceRunner(
        replicationHealthCheckService,
        List.of(reportService1, reportService2),
        metricsHandler
    );
    // use reflection to set the private @Value field
    ReflectionTestUtils.setField(runner, "ignoreRowCountMismatch", true);

    // when
    runner.run(applicationArguments);

    // then - reports should still be generated
    verify(reportService1).refreshDataSource();
    verify(reportService1).generateReport();
    verify(reportService2).refreshDataSource();
    verify(reportService2).generateReport();
  }

  @Test
  void shouldAbortWhenIgnoreMismatchTrueButWalLsnNotOK() {
    // given
    ReplicationHealthReport unhealthy = new ReplicationHealthReport(LocalDate.now());
    unhealthy.setHealthy(false);
    unhealthy.setWalLsnOk(false);

    when(replicationHealthCheckService.checkReplicationHealth()).thenReturn(unhealthy);

    runner = new ClaimsReportingServiceRunner(
        replicationHealthCheckService,
        List.of(reportService1, reportService2),
        metricsHandler
    );
    ReflectionTestUtils.setField(runner, "ignoreRowCountMismatch", true);

    // when / then
    verifyNoInteractions(reportService1, reportService2);
  }
}