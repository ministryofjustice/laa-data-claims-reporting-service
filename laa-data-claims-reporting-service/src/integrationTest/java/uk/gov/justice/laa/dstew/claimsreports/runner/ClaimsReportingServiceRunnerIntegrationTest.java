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

/**
 * Integration tests for the ClaimsReportingServiceRunner component, verifying its functionality
 * in a full application context using test-specific settings and PostgreSQL Testcontainers.
 *
 * <p>
 * This test ensures that:
 * - The materialized view associated with `Report000Entity` is refreshed correctly.
 * - Data is successfully retrieved from the database.
 * - The generated reports or corresponding data output meet expected conditions.
 *
 * <p>
 * An embedded PostgreSQL container is used for the test database to simulate the production-like
 * environment. The `@SpringBootTest` annotation is used to load the application context, and
 * dependencies such as `Report000Service` and `Report000Repository` are autowired for testing.
 *
 * <p>
 * An active Spring profile, "test", is set to ensure specific test configurations are applied.
 * e.g. the sql scripts in the testdata folder which insert test data are run only for the test profile.
 */
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