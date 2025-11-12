package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Report012Service is responsible for generating and managing report_012.
 * This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report012 data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report012Service extends AbstractReportService {

  public Report012Service(JdbcTemplate jdbcTemplate,
                          S3ClientWrapper s3ClientWrapper, CsvCreationService csvCreationService) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService);
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_012";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_012";
  }

  @Override
  protected String getReportFileName() {
    return "report_012.csv";
  }

  @Override
  protected String getReportName() {
    return "REPORT012";
  }

  @Override
  protected String getOrderByClause() {
    return " \"Provider office account number\","
        + "    to_char(to_date(\"Submission month\", 'MON-YYYY'), 'YYYYMM'),"
        + "    \"Area of law\"";
  }

}