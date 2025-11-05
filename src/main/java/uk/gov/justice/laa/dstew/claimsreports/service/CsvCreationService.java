package uk.gov.justice.laa.dstew.claimsreports.service;

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
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * Data access object class to provide interface between application and database layer.
 */
@Service
@Slf4j
@AllArgsConstructor
public class CsvCreationService {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;
  protected AppConfig appConfig;
  private final CsvMapper csvMapper;

  /**
   * Builds CSV from data retrieved from SQL query
   * Returns data in chunks, size defined in application config, to ensure
   * good performance for large datasets.
   *
   * @param sqlQuery query for retrieving dataset
   * @param writer writes string buffer into csv file
   */
  public void buildCsvFromData(String sqlQuery, BufferedWriter writer) {
    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
      throw new CsvCreationException("SQL query is not provided");
    }

    if (writer == null) {
      throw new CsvCreationException("BufferedWriter is null");
    }

    try (writer) {
      Map<String, String> row = new LinkedHashMap<>();

      jdbcTemplate.query(
          (Connection con) -> {
            return buildPreparedStatement(sqlQuery, con, appConfig.getDataChunkSize());
          },
          new CsvRowCallbackHandler(writer, row, appConfig.getBufferFlushFrequency(), csvMapper)
      );

      writer.flush();
      log.info("CSV creation completed");

    } catch (IOException ex) {
      throw new CsvCreationException("Failure to write to file: " + ex.getMessage());
    } catch (CsvCreationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new CsvCreationException("Failure during CSV creation " + ex.getMessage());
    }
  }

  /**
   * Creates a prepared statement that fetches data from the database in defined chunks,
   * specifically to provide performance improvements for large data sets.
   * Setting autocommit to false, in combination with fetch size,
   * ensures data is retrieved in chunks.
   *
   * @param sqlQuery SELECT statement for report data
   * @param con database connection
   *
   * @return {PreparedStatement}
   */
  private PreparedStatement buildPreparedStatement(
      String sqlQuery, Connection con, int dataChunkSize) {
    try {
      con.setAutoCommit(false);
      PreparedStatement statement = con.prepareStatement(sqlQuery,
          ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      statement.setFetchSize(dataChunkSize);
      return statement;
    } catch (SQLException ex) {
      throw new CsvCreationException("Failed on creation of prepared statement "
          + ex.getNextException());
    }
  }

}