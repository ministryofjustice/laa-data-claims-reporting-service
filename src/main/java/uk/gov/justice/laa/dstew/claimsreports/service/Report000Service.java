package uk.gov.justice.laa.dstew.claimsreports.service;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

/**
 * Report000Service is responsible for generating and managing report_000.
 * This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report000 data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report000Service extends AbstractReportService {

  public Report000Service(JdbcTemplate jdbcTemplate,
                          FileUploader fileUploader, CsvCreationService csvCreationService) {
    super(jdbcTemplate, fileUploader, csvCreationService);
  }

  @Override
  protected String getMaterializedViewName() {
    return "claims.mvw_report_000";
  }

  @Override
  protected String getReportFileName() {
    return "report_000.csv";
  }

  @Override
  protected String getReportName() {
    return "REPORT000";
  }
}