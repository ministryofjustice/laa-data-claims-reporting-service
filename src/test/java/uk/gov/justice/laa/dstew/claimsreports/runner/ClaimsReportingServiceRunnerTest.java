package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.ApplicationArguments;

import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService;

class ClaimsReportingServiceRunnerTest {

  @Mock
  private AbstractReportService<?, ?> reportService1;

  @Mock
  private AbstractReportService<?, ?> reportService2;

  @Mock
  private ReplicationHealthCheckService replicationHealthCheckService;

  @Mock
  private ApplicationArguments applicationArguments;

  private ClaimsReportingServiceRunner runner;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Inject a list of mocked report services
    runner = new ClaimsReportingServiceRunner(replicationHealthCheckService, List.of(reportService1, reportService2));
  }

  @Test
  void shouldInvokeGenerateReportsOnAllServices() {
    // Call the run method
    runner.run(applicationArguments);

    // Verify that refreshMaterializedView and generateReport were called on each service
    verify(reportService1).refreshMaterializedView();
    verify(reportService1).generateReport();

    verify(reportService2).refreshMaterializedView();
    verify(reportService2).generateReport();
  }

  @Test
  void shouldHandleEmptyServiceList() {
    // Create runner with empty list
    ClaimsReportingServiceRunner emptyRunner = new ClaimsReportingServiceRunner(null, List.of());

    // Should not throw any exceptions
    assertThatCode(() -> emptyRunner.run(applicationArguments))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldContinueWhenOneServiceFails() {
    // Make the first service throw an exception when refreshing
    doThrow(new RuntimeException("Refresh failed")).when(reportService1).refreshMaterializedView();

    // Call run (should continue to second service)
    runner.run(applicationArguments);

    // First service was called
    verify(reportService1).refreshMaterializedView();
    verify(reportService1, never()).generateReport(); // generateReport skipped because refresh failed

    // Second service should still run
    verify(reportService2).refreshMaterializedView();
    verify(reportService2).generateReport();
  }
}