package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Report013Service is responsible for generating and managing report_013.
 * This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report013 data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report013Service extends AbstractReportService {

  public Report013Service(JdbcTemplate jdbcTemplate,
                          S3ClientWrapper s3ClientWrapper, CsvCreationService csvCreationService) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService);
  }

  @Override
  protected String getDataSourceName() {
    return "claims.report_013";
  }

  /**
   * This is a complex report which needs a function (with local arrays and procedural elements) to
   * refresh the underlying report data, rather than a materialised view with a single SQL statement
   */
  @Override
  protected String getRefreshCommand() {
    return "SELECT claims.refresh_report013()"; //The "SELECT" statement actually runs the stored function
  }

  @Override
  protected String getReportFileName() {
    return "report_013.csv";
  }

  @Override
  protected String getReportName() {
    return "REPORT013";
  }

}