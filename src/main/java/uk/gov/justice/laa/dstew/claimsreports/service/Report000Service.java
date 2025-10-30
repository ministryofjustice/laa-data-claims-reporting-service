package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
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

  public Report000Service(JdbcTemplate jdbcTemplate, DataSource dataSource, AppConfig appConfig, FileUploader fileUploader) {
    super(jdbcTemplate, dataSource, appConfig, fileUploader);
  }

  @Override
  protected String getMaterializedViewName() {
    return "claims.mvw_report_000";
  }

  @Override
  public void generateReport() {
    log.info("Generating report from {}", getClass().getSimpleName());
    CsvCreationService csvCreationService = new CsvCreationService(jdbcTemplate, dataSource, appConfig);

    try {
      File tempFile = new File("/tmp/report_000.csv");
      log.info("Created temp file {}", tempFile.getPath());

      try {
        csvCreationService.buildCsvFromData("SELECT * FROM claims.mvw_report_000", new BufferedWriter(new FileWriter(tempFile)));
        fileUploader.uploadFile(tempFile, "report_000.csv");
        //TODO time how long takes to upload??
      } catch (CsvCreationException e) {
        log.info("Failure to create Report000");
        throw e;
      } finally {
        if (tempFile.exists()) {
          // Remove this if you want to test things locally without S3 mocked and want to see the output
          boolean isFileDeleted = tempFile.delete();
          if (!isFileDeleted) {
            log.warn("Failed to clean up file {}", tempFile.getPath());
          }
        }
      }

    } catch (IOException e) {
      throw new CsvCreationException("Failure to create Report000: " + e.getMessage());
    }
  }

}