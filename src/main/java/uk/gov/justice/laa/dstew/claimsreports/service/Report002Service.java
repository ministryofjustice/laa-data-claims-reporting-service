package uk.gov.justice.laa.dstew.claimsreports.service;

import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Report002Service is responsible for generating and managing report_002.
 */
@Slf4j
@Service
public class Report002Service extends AbstractReportService {

  private static final int MONTHLY_REPORT_DATE = 21;

  public Report002Service(JdbcTemplate jdbcTemplate,
                          S3ClientWrapper s3ClientWrapper,
                          CsvCreationService csvCreationService,
                          MetricsHandler metricsHandler,
                          Clock clock) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_002";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_002";
  }

  @Override
  protected String getReportFileName() {
    return "report_002";
  }

  @Override
  public String getReportName() {
    return "REPORT002";
  }

  @Override
  protected String getReportFolder() {
    return "monthly";
  }

  @Override
  protected String getOrderByClause() {
    return " to_char(to_date(\"Submission for date\", 'MON-YYYY'), 'YYYYMM') NULLS LAST,"
        + "    \"Office code\","
        + "    \"Category code\","
        + "    \"Procurement area code\","
        + "    \"Access point code\"";
  }

  @Override
  protected boolean runToday() {
    return LocalDate.now(clock).getDayOfMonth() == MONTHLY_REPORT_DATE;
  }
}
