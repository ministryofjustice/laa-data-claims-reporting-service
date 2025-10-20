package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import uk.gov.justice.laa.dstew.claimsreports.entity.Report000Entity;
import uk.gov.justice.laa.dstew.claimsreports.repository.Report000Repository;
import uk.gov.justice.laa.dstew.claimsreports.service.Report000Service;

@SpringBootTest
  @ActiveProfiles("test")
  @Testcontainers
  @ContextConfiguration
  public class ClaimsReportingServiceRunnerIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private Report000Repository repository;

    @Autowired
    private Report000Service report000Service;

    @Test
    void testViewAndReportGeneration() {
      report000Service.refreshMaterializedView();

      List<Report000Entity> rows = repository.findAll();
      assertThat(rows).isNotEmpty();
      assertThat(rows.size()).isEqualTo(2);

      // Validate CSV output or generated file if applicable
    }
  }