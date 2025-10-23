package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.repository.RefreshableMaterializedView;

import static org.mockito.Mockito.*;

import javax.sql.DataSource;

/**
 * Unit tests for AbstractReportService
 */
class AbstractReportServiceTest {

  // Define a concrete subclass for testing purposes
  static class TestReportService extends AbstractReportService<String, TestRepository> {
    public TestReportService(TestRepository repository, JdbcTemplate template, DataSource dataSource, AppConfig appConfig) {
      super(repository, template, dataSource, appConfig);
    }

    @Override
    public void generateReport() {
      // No-op for testing
    }
  }

  // Mock repository that extends both JpaRepository and RefreshableMaterializedView
  interface TestRepository extends JpaRepository<String, String>, RefreshableMaterializedView {}

  private TestRepository repository;
  private TestReportService service;
  private JdbcTemplate jdbcTemplate;
  private DataSource dataSource;
  private AppConfig appConfig;

  @BeforeEach
  void setUp() {
    repository = mock(TestRepository.class);
    jdbcTemplate = mock(JdbcTemplate.class);
    dataSource = mock(DataSource.class);
    appConfig = mock(AppConfig.class);
    service = new TestReportService(repository, jdbcTemplate, dataSource, appConfig);
  }

  @Test
  void refreshMaterializedViewShouldInvokeRepositoryAndLog() {
    // when
    service.refreshMaterializedView();

    // then
    verify(repository, times(1)).refreshMaterializedView();
  }

  @Test
  void refreshMaterializedView_ShouldHandleMultipleInvocations() {
    service.refreshMaterializedView();
    service.refreshMaterializedView();
    verify(repository, times(2)).refreshMaterializedView();
  }

}