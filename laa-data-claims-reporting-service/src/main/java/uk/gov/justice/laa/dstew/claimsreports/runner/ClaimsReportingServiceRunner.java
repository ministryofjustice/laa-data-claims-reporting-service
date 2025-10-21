package uk.gov.justice.laa.dstew.claimsreports.runner;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.claimsreports.service.AbstractReportService;



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
 * - The `run` method is invoked at application startup and calls the internal `generateReports` method.
 * - The `generateReports` method iterates through the provided list of report services, ensuring each one
 *   performs the refresh of its associated materialized view and generates the corresponding report.
 *
 * <p>
 * Usage:
 * - This component is used as part of the Spring Boot application lifecycle to ensure reports are
 *   consistently generated during the application execution phase.
 * - The logic for refreshing materialized views and generating reports is delegated to the individual
 *   AbstractReportService implementations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimsReportingServiceRunner  implements ApplicationRunner {

  //Spring will auto-inject all services that implement the AbstractReportService
  private final List<AbstractReportService<?, ?>> reportServices;

  @Override
  public void run(ApplicationArguments args) {
    generateReports();
  }

  /**
   * Generates reports by iterating through a list of report services, performing the following tasks:
   * - Refreshing the associated materialized view for each report service.
   * - Generating the report through the report service logic.
   *
   * <p>
   * This method ensures that errors during the generation process for one service do not interfere
   * with the other services. If an exception occurs during the execution of a specific report service,
   * it logs an error message containing the name of the service and the details of the exception.
   *
   * <p>
   * The implementation assumes that the report services extend from the AbstractReportService base class,
   * which provides the necessary methods for refreshing materialized views and generating reports.
   */
  private void generateReports() {
    for (AbstractReportService<?, ?> service : reportServices) {
      try {
        service.refreshMaterializedView();
        service.generateReport();
      } catch (Exception e) {
        log.error("Report generation failed for {}: {}",
            service.getClass().getSimpleName(), e.getMessage(), e);
      }
    }
  }

}
