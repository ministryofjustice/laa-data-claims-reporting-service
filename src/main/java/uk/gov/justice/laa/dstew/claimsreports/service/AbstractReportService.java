package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

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
  protected final S3ClientWrapper s3ClientWrapper;
  protected final CsvCreationService csvCreationService;

  /**
   * Gets the name of the materialized view or table associated with the current service.
   * This method is intended to be implemented by subclasses to define the specific
   * data source they read from.
   *
   * @return the name of the materialized view or table as a String
   */
  protected abstract String getDataSourceName();

  protected abstract String getRefreshCommand();

  /**
   * Refreshes the associated materialized view.
   */
  @Transactional
  public void refreshDataSource() {
    String refreshCommand = getRefreshCommand();
    log.info("Refreshing data for {}", getReportName());

    jdbcTemplate.execute(refreshCommand);

    log.info("Refresh complete for {}", getReportName());
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
    File tempFile = new File("/tmp/" + getReportFileName());

    try {
      var sql = "SELECT * FROM " + getDataSourceName();
      try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
        csvCreationService.buildCsvFromData(sql, writer);
      }
      s3ClientWrapper.uploadFile(tempFile, getReportFileName());
    } catch (Exception e) {
      log.error("Failed to generate {}: {}", getReportName(), e.getMessage());
      throw new CsvCreationException("Failure to create " + getReportName(), e);
    } finally {
      deleteTempFile(tempFile);
    }
  }

  private void deleteTempFile(File tempFile) {
    if (tempFile.exists()) {
      try {
        Files.delete(tempFile.toPath());
        log.info("Deleted temp file {}", tempFile.getPath());
      } catch (IOException e) {
        log.warn("Failed to delete temp file {}: {}", tempFile.getPath(), e.getMessage());
      }
    }
  }
}
