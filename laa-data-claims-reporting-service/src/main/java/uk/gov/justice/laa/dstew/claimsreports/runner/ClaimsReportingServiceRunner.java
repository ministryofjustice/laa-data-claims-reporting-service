package uk.gov.justice.laa.dstew.claimsreports.runner;

import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClaimsReportingServiceRunner  implements ApplicationRunner {
  private final EntityManager entityManager;
  private final ConfigurableApplicationContext context;

  @Override
  public void run(ApplicationArguments args) {
    generateReports();
    context.close(); // Graceful shutdown
  }

  /** TODO: replace test code with actual report generation code */
  private void generateReports() {
    try {
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

    } catch (Exception e) {
      log.error("Database connectivity or query failed: {}", e.getMessage());
      e.printStackTrace();
    } finally {
      context.close(); // graceful shutdown
    }
  }
}
