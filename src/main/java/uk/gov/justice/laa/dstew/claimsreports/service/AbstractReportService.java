package uk.gov.justice.laa.dstew.claimsreports.service;

import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_SKIPPED;
import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_SUCCESSFUL;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.CustomReportMetric;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * AbstractReportService serves as a base class for implementing report generation services
 * with support for operations on materialized views. This class provides a framework for
 * refreshing materialized views and generating reports. Subclasses should provide specific
 * implementations for the abstract methods as needed.
 */
@Slf4j
@Transactional
@AllArgsConstructor
public abstract class AbstractReportService {

  protected final JdbcTemplate jdbcTemplate;
  protected final S3ClientWrapper s3ClientWrapper;
  protected final CsvCreationService csvCreationService;
  protected MetricsHandler metricsHandler;
  protected Clock clock;

  //Abstract methods (implemented by subclasses) to provide relevant details for individual reports

  protected abstract String getDataSourceName();

  protected abstract String getRefreshCommand();

  protected abstract String getOrderByClause();

  /**
   * Refreshes the associated materialized view.
   */
  @Transactional
  public void refreshDataSource() {
    String refreshCommand = getRefreshCommand();
    log.info("Refreshing data for {}", getReportName());
    long startTime = System.currentTimeMillis();

    jdbcTemplate.execute(refreshCommand);

    long endTime = System.currentTimeMillis();
    long durationMilliseconds = endTime - startTime;
    log.info("Refresh complete for {} in {} ms", getReportName(), durationMilliseconds);
    metricsHandler.setCustomMetric(CustomReportMetric.DATA_REFRESH_TIME_MS, durationMilliseconds);
  }

  /**
   * Gets the intended name of the report.
   * This method is intended to be implemented by subclasses to define the name expected
   *
   * @return the report's expected name
   */
  public abstract String getReportName();

  /**
   * Gets the intended file name of the report.
   * This method is intended to be implemented by subclasses to define the file name expected
   * It is used to build the full file name which includes date and extension.
   *
   * @return the report's expected file name
   */
  protected abstract String getReportFileName();

  /**
   * Gets the "folder" of the report.
   * S3 doesn't really have folders, but it's easier to think of the path as containing folders and that is how
   * it is often visualised in the Console.
   * This will be appended onto the "reports/" folder.
   *
   * @return the report's expected path in S3
   */
  protected abstract String getReportFolder();

  /**
   * Gets the intended file extension of the report.
   * At present every report is a csv
   *
   * @return the report's expected file extension
   */
  private String getReportFileExtension() {
    return ".csv";
  }

  /**
   * Builds the final filename of the report, including the generation date.
   *
   * @return the report's file name
   */
  private String getFullReportFileName() {
    return getReportFileName() + "_" + LocalDate.now(clock) + getReportFileExtension();
  }

  /**
   * Builds the key to use for file upload to s3 - "folders" as well as filename.
   *
   * @return the report's file name
   */
  private String generateS3FileKey() {
    return "reports/" + getReportFolder() + "/" + getFullReportFileName();
  }

  /**
   * Generates a CSV report.
   */
  public void generateReport() {
    if (!runToday()) {
      log.info("Report {} is not scheduled to run today", getReportName());
      metricsHandler.setCustomMetric(CustomReportMetric.REPORT_SUCCESSFUL, REPORT_SKIPPED);
      return;
    }

    log.info("Generating report from {}", getClass().getSimpleName());
    File tempFile = new File("/tmp/" + getFullReportFileName());
    long startTime = System.currentTimeMillis();

    try {
      var sql = "SELECT * FROM " + getDataSourceName() + " ORDER BY " + getOrderByClause();
      try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
        csvCreationService.buildCsvFromData(sql, writer, getReportName());
      }
      long endTime = System.currentTimeMillis();
      long durationMilliseconds = endTime - startTime;
      log.info("Created {} file with filename {} in {} ms", getReportName(), getFullReportFileName(), durationMilliseconds);
      metricsHandler.setCustomMetric(CustomReportMetric.GENERATED_TIME_MS, durationMilliseconds);
      s3ClientWrapper.uploadFile(tempFile, generateS3FileKey());
      metricsHandler.setCustomMetric(CustomReportMetric.REPORT_SUCCESSFUL, REPORT_SUCCESSFUL);

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

  protected abstract boolean runToday();

}
