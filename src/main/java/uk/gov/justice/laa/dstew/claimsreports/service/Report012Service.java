package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

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
                          FileUploader fileUploader, CsvCreationService csvCreationService) {
    super(jdbcTemplate, fileUploader, csvCreationService);
  }

  @Override
  protected String getMaterializedViewName() {
    return "claims.mvw_report_012";
  }

  @Override
  protected String getReportFileName() {
    return "report_012.csv";
  }

  @Override
  protected String getReportName() {
    return "REPORT012";
  }

}