package uk.gov.justice.laa.dstew.claimsreports.service;

import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_SKIPPED;
import static uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomReportGauges.REPORT_SUCCESSFUL;
import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;
import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitiseForFilename;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * AbstractReportService serves as a base class for implementing report generation services with
 * support for operations on materialized views. This class provides a framework for refreshing
 * materialized views and generating reports. Subclasses should provide specific implementations for
 * the abstract methods as needed.
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

  // Abstract methods (implemented by subclasses) to provide relevant details for individual reports

  protected abstract String getDataSourceName();

  protected abstract String getRefreshCommand();

  protected abstract String getOrderByClause();

  /** Refreshes the associated materialized view. */
  @Transactional
  @SuppressFBWarnings(
      value = "SQL_INJECTION_SPRING_JDBC",
      justification = "Refresh SQL is supplied by report service constants, not user input.")
  public void refreshDataSource() {
    String refreshCommand = getRefreshCommand();
    log.info("Refreshing data for {}", sanitise(getReportName()));
    long startTime = System.currentTimeMillis();

    jdbcTemplate.execute(refreshCommand);

    long endTime = System.currentTimeMillis();
    long durationMilliseconds = endTime - startTime;
    log.info("Refresh complete for {} in {} ms", sanitise(getReportName()), durationMilliseconds);
    metricsHandler.setCustomMetric(CustomMetricId.DATA_REFRESH_TIME_MS, durationMilliseconds);
  }

  /**
   * Gets the intended name of the report. This method is intended to be implemented by subclasses
   * to define the name expected
   *
   * @return the report's expected name
   */
  public abstract String getReportName();

  /**
   * Gets the intended file name of the report. This method is intended to be implemented by
   * subclasses to define the file name expected It is used to build the full file name which
   * includes date and extension.
   *
   * @return the report's expected file name
   */
  protected abstract String getReportFileName();

  /**
   * Gets the "folder" of the report. S3 doesn't really have folders, but it's easier to think of
   * the path as containing folders and that is how it is often visualised in the Console. This will
   * be appended onto the "reports/" folder.
   *
   * @return the report's expected path in S3
   */
  protected abstract String getReportFolder();

  protected List<String> getExpectedCsvHeaders() {
    return List.of();
  }

  protected Pattern getAdditionalCsvHeaderPattern() {
    return null;
  }

  /**
   * Gets the intended file extension of the report. At present every report is a csv
   *
   * @return the report's expected file extension
   */
  protected String getReportFileExtension() {
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

  /** Generates a CSV report. */
  public void generateReport() {
    if (!runToday()) {
      log.atInfo()
          .addKeyValue("event.action", "report.schedule.skip")
          .addKeyValue("event.type", "batch")
          .addKeyValue("report.name", getReportName())
          .addKeyValue("event.outcome", "skipped")
          .log("Report {} is not scheduled to run today", sanitise(getReportName()));
      metricsHandler.setCustomMetric(CustomMetricId.REPORT_SUCCESSFUL, REPORT_SKIPPED);
      return;
    }

    log.atInfo()
        .addKeyValue("event.action", "report.generate")
        .addKeyValue("event.type", "batch")
        .addKeyValue("report.name", getReportName())
        .log("Generating report from {}", getClass().getSimpleName());
    File tempFile = createTempReportFile();
    long startTime = System.currentTimeMillis();

    try {
      final String sql = buildReportSql(getDataSourceName(), getOrderByClause());
      writeReportToTempFile(sql, tempFile);
      long endTime = System.currentTimeMillis();
      long durationMilliseconds = endTime - startTime;
      log.atInfo()
          .addKeyValue("event.action", "report.generated")
          .addKeyValue("event.type", "batch")
          .addKeyValue("report.name", getReportName())
          .addKeyValue("event.outcome", "success")
          .log(
              "Created {} file with filename {} in {} ms",
              sanitise(getReportName()),
              sanitise(getFullReportFileName()),
              durationMilliseconds);
      metricsHandler.setCustomMetric(CustomMetricId.GENERATED_TIME_MS, durationMilliseconds);
      uploadReportFile(tempFile, generateS3FileKey());
      metricsHandler.setCustomMetric(CustomMetricId.REPORT_SUCCESSFUL, REPORT_SUCCESSFUL);

    } catch (IOException | RuntimeException e) {
      log.atError()
          .addKeyValue("event.action", "report.generate")
          .addKeyValue("event.type", "batch")
          .addKeyValue("report.name", getReportName())
          .addKeyValue("event.outcome", "failure")
          .setCause(e)
          .log("Failed to generate {}", sanitise(getReportName()));
      throw new CsvCreationException("Failure to create " + getReportName(), e);
    } finally {
      deleteTempFile(tempFile);
    }
  }

  @SuppressFBWarnings(
      value = "PATH_TRAVERSAL_IN",
      justification =
          "Prefix/suffix sanitised to alphanumeric/dot/dash/underscore only via sanitiseForFilename before use")
  private File createTempReportFile() {
    try {
      String prefix = sanitiseForFilename(getReportFileName()) + "_" + LocalDate.now(clock) + "_";
      String suffix = sanitiseForFilename(getReportFileExtension());
      Path tempFile = Files.createTempFile(prefix, suffix);
      return tempFile.toFile();
    } catch (IOException e) {
      throw new CsvCreationException("Failure to create temp file for " + getReportName(), e);
    }
  }

  private String buildReportSql(String dataSourceName, String orderByClause) {
    return String.format("SELECT * FROM %s ORDER BY %s", dataSourceName, orderByClause);
  }

  protected void writeReportToTempFile(String sql, File tempFile) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
      csvCreationService.buildCsvFromData(sql, writer, getReportName());
    }
  }

  protected void uploadReportFile(File tempFile, String s3FileKey) {
    var expectedHeaders = getExpectedCsvHeaders();
    s3ClientWrapper.uploadFile(
        tempFile, s3FileKey, expectedHeaders, getAdditionalCsvHeaderPattern());
  }

  private void deleteTempFile(File tempFile) {
    if (tempFile.exists()) {
      try {
        Files.delete(tempFile.toPath());
        log.info("Deleted temp file {}", sanitise(tempFile.getPath()));
      } catch (IOException e) {
        log.warn(
            "Failed to delete temp file {}: {}",
            sanitise(tempFile.getPath()),
            sanitise(e.getMessage()));
      }
    }
  }

  protected abstract boolean runToday();
}
