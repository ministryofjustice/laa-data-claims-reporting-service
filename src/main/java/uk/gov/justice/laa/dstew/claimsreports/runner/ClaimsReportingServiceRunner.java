package uk.gov.justice.laa.dstew.claimsreports.runner;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.dstew.claimsreports.service.ReportCreationService;

/**
 * A service runner class that is responsible for generating reports
 * related to claims upon application startup. Implements the
 * {@link ApplicationRunner} interface to execute code after the
 * application context is fully initialized.
 *
 * <p>This class utilizes JPA for database interaction and ensures a graceful
 * shutdown of the application context after executing its logic.</p>
 *
 * <p>Dependencies:
 * - {@link EntityManager}: Used for interacting with the database.
 * - {@link ConfigurableApplicationContext}: Provides application context
 *   management and lifecycle handling.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimsReportingServiceRunner  implements ApplicationRunner {
  private final EntityManager entityManager;
  private final ConfigurableApplicationContext context;
  private final ReportCreationService reportCreationService;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    generateReports();
  }

  /** TODO: replace test code with actual report generation code. */
  private void generateReports() throws Exception {
    try {
      Thread.sleep(10000);
      List<String> tables = entityManager.createNativeQuery("""
          SELECT table_name
          FROM information_schema.tables
          WHERE table_schema = 'claims'
          ORDER BY table_name
          """).getResultList();

      log.info("Connected via JPA! Tables found in schema 'claims':");
      if (tables.isEmpty()) {
        log.info("  (no tables found)");
      } else {
        tables.forEach(t -> log.info("  - {}", t));
      }

      //    reportCreationService.saveCsv();
      reportCreationService.saveCsvWithStream();
      Thread.sleep(10000);

    } catch (Exception e) {
      log.error("Database connectivity or query failed: {}", e.getMessage());
      e.printStackTrace();





    } finally {
      context.close(); // graceful shutdown
    }
  }
}
