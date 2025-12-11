package uk.gov.justice.laa.dstew.claimsreports.service;

import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

public class TestReportService extends AbstractReportService {

    private boolean runToday;

    public TestReportService(JdbcTemplate template, S3ClientWrapper s3ClientWrapper,
                             CsvCreationService csvCreationService, MetricsHandler metricsHandler, boolean runToday, Clock clock) {
      super(template, s3ClientWrapper, csvCreationService, metricsHandler, clock);
      this.runToday = runToday;
    }

    @Override
    protected String getDataSourceName() {
      return "claims.mvw_report_000";
    }

    @Override
    protected String getRefreshCommand() {
      return "REFRESH MATERIALIZED VIEW claims.mvw_report_000";
    }

    @Override
    protected String getReportName() {
      return "testReport";
    }

    @Override
    protected String getReportFileName() {
      return "test_report";
    }

  @Override
    protected String getOrderByClause() {
      return " test_order_by_column";
    }

    @Override
    protected boolean runToday() { return runToday;}
}