package uk.gov.justice.laa.dstew.claimsreports.service;

import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.dataformat.csv.CsvMapper;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/** Data access object class to provide interface between application and database layer. */
@Service
@Slf4j
@AllArgsConstructor
public class CsvCreationService {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;
  protected AppConfig appConfig;
  private final CsvMapper csvMapper;
  private final MetricsHandler metricsHandler;

  /**
   * Builds CSV from data retrieved from SQL query Returns data in chunks, size defined in
   * application config, to ensure good performance for large datasets.
   *
   * @param sqlQuery query for retrieving dataset
   * @param writer writes string buffer into csv file
   */
  public void buildCsvFromData(String sqlQuery, BufferedWriter writer, String reportName) {
    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
      throw new CsvCreationException("SQL query is not provided");
    }

    if (writer == null) {
      throw new CsvCreationException("BufferedWriter is null");
    }

    try (writer) {
      Map<String, String> row = new LinkedHashMap<>();
      var handler =
          new CsvRowCallbackHandler(writer, row, appConfig.getBufferFlushFrequency(), csvMapper);

      jdbcTemplate.query(
          (Connection con) -> buildPreparedStatement(sqlQuery, con, appConfig.getDataChunkSize()),
          handler);

      writer.flush();
      log.info("CSV creation completed for {}", sanitise(reportName));
      var rowsWritten = handler.getRowCount();
      log.info("Rows written for {}: {}", sanitise(reportName), rowsWritten);
      metricsHandler.setCustomMetric(CustomMetricId.ROWS_WRITTEN, rowsWritten);

    } catch (IOException ex) {
      throw new CsvCreationException("Failure to write to file for " + reportName, ex);
    } catch (CsvCreationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new CsvCreationException("Failure during CSV creation of " + reportName, ex);
    }
  }

  /**
   * Creates a prepared statement that fetches data from the database in defined chunks,
   * specifically to provide performance improvements for large data sets. Setting autocommit to
   * false, in combination with fetch size, ensures data is retrieved in chunks.
   *
   * @param sqlQuery SELECT statement for report data
   * @param con database connection
   * @return {PreparedStatement}
   */
  @SuppressFBWarnings(
      value = "SQL_INJECTION_JDBC",
      justification =
          "Report SQL is assembled from validated service-owned identifiers before reaching this method.")
  private PreparedStatement buildPreparedStatement(
      String sqlQuery, Connection con, int dataChunkSize) {
    try {
      con.setAutoCommit(false);
      PreparedStatement statement =
          con.prepareStatement(sqlQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      statement.setFetchSize(dataChunkSize);
      return statement;
    } catch (SQLException ex) {
      throw new CsvCreationException("Failed on creation of prepared statement", ex);
    }
  }
}
