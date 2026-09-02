package uk.gov.justice.laa.dstew.claimsreports.service;

import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Report014Service is responsible for generating and managing report_014. This service extends the
 * AbstractReportService and provides an implementation for the report generation process.
 * Responsibilities: - Implements report generation logic for Report014 data. - Utilizes the
 * inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report014Service extends AbstractReportService {

  public Report014Service(
      JdbcTemplate jdbcTemplate,
      S3ClientWrapper s3ClientWrapper,
      CsvCreationService csvCreationService,
      MetricsHandler metricsHandler,
      Clock clock) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_014";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_014";
  }

  @Override
  protected String getReportFileName() {
    return "report_014";
  }

  @Override
  public String getReportName() {
    return "REPORT014";
  }

  @Override
  protected String getReportFolder() {
    return "daily";
  }

  @Override
  protected String getOrderByClause() {
    return "\"Claim ID\","
        + " to_date(\"Amendment Date\", 'DD/MM/YYYY'),"
        + " to_timestamp(\"Amendment Time\", 'HH24:MI:SS')::time,"
        + " \"Assessment ID\"";
  }

  @Override
  protected List<String> getExpectedCsvHeaders() {
    return List.of(
        "Office Account Number",
        "Submission Period",
        "Area of Law",
        "Client Forename",
        "Client Surname",
        "Unique Client Number",
        "Unique File Number",
        "Amendment Date",
        "Amendment Time",
        "Value before Amendment",
        "Value after Amendment",
        "Difference",
        "Assessment Type",
        "Assessment Reason",
        "Submission ID",
        "Claim ID",
        "Assessment ID");
  }

  // Daily report
  @Override
  protected boolean runToday() {
    return true;
  }
}
