package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

/**
 * AbstractReportService serves as a base class for implementing report generation services
 * with support for operations on materialized views. This class provides a framework for
 * refreshing materialized views and generating reports. Subclasses should provide specific
 * implementations for the abstract methods as needed.
 *
 */
@Slf4j
@Transactional
@AllArgsConstructor
public abstract class AbstractReportService {

  protected final JdbcTemplate jdbcTemplate;
  protected final FileUploader fileUploader;
  protected final CsvCreationService csvCreationService;

  /**
   * Gets the name of the materialized view associated with the current service.
   * This method is intended to be implemented by subclasses to define the specific
   * materialized view they are operating on.
   *
   * @return the name of the materialized view as a String
   */
  protected abstract String getMaterializedViewName();

  /**
   * Refreshes the associated materialized view.
   */
  @Transactional
  public void refreshMaterializedView() {
    String viewName = getMaterializedViewName();
    log.info("Refreshing materialized view {}", viewName);

 //   jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + viewName);

    log.info("Refresh complete for {}", viewName);
  }

  /**
   * Gets the intended name of the report.
   * This method is intended to be implemented by subclasses to define the name expected
   *
   * @return the report's expected name
   */
  protected abstract String getReportName();

  /**
   * Gets the intended file name of the report.
   * This method is intended to be implemented by subclasses to define the file name expected
   *
   * @return the report's expected file name
   */
  protected abstract String getReportFileName();

  /**
   * Generates a CSV report.
   */
  public void generateReport() {
    log.info("Generating report from {}", getClass().getSimpleName());

    try {
      File tempFile = new File("/tmp/" + getReportFileName());
      log.info("Created temp file {}", tempFile.getPath());

      try {
        var sql = "SELECT * FROM " + getMaterializedViewName();
        csvCreationService.buildCsvFromData(sql, new BufferedWriter(new FileWriter(tempFile)));
        fileUploader.uploadFile(tempFile, getReportFileName());
      } catch (CsvCreationException e) {
        log.info("Failure to create {}", getReportName());
        throw e;
      } finally {
        if (tempFile.exists()) {
          // Remove this if you want to test things locally and want to see the output file
//          boolean isFileDeleted = tempFile.delete();
//          if (isFileDeleted) {
//            log.info("Deleted temp file {}", tempFile.getPath());
//          } else {
//            log.warn("Failed to clean up file {}", tempFile.getPath());
//          }
        }
      }

    } catch (IOException e) {
      throw new CsvCreationException("Failure to create " + getReportName() + ": " + e.getMessage());
    }
  }

}
