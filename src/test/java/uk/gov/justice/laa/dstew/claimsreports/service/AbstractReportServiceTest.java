package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;

import static org.mockito.Mockito.*;

import javax.sql.DataSource;

/**
 * Unit tests for AbstractReportService
 */
class AbstractReportServiceTest {

  // Define a concrete subclass for testing purposes
  static class TestReportService extends AbstractReportService {
    public TestReportService(JdbcTemplate template, DataSource dataSource, AppConfig appConfig) {
      super(template, dataSource, appConfig);
    }

    @Override
    protected String getMaterializedViewName() {
      return "claims.mvw_report_000";
    }

    @Override
    public void generateReport() {
      // No-op for testing
    }
  }

  private TestReportService service;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    jdbcTemplate = mock(JdbcTemplate.class);
    DataSource dataSource = mock(DataSource.class);
    AppConfig appConfig = mock(AppConfig.class);
    service = new TestReportService(jdbcTemplate, dataSource, appConfig);
  }

  @Test
  void refreshMaterializedViewShouldInvokeRepositoryAndLog() {
    // when
    service.refreshMaterializedView();

    // then
    verify(jdbcTemplate, times(1))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

  @Test
  void refreshMaterializedView_ShouldHandleMultipleInvocations() {
    service.refreshMaterializedView();
    service.refreshMaterializedView();
    verify(jdbcTemplate, times(2))
        .execute("REFRESH MATERIALIZED VIEW claims.mvw_report_000");
    verifyNoMoreInteractions(jdbcTemplate);
  }

}