package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * Data access object class to provide interface between application and database layer.
 */
@Service
@Slf4j
public class CsvCreationService {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;
  protected AppConfig appConfig;

  /**
   * Stuff.
   *
   * @param template stuff
   * @param dataSource things
   * @param appConfig other things
   */
  public CsvCreationService(JdbcTemplate template, DataSource dataSource, AppConfig appConfig) {
    this.jdbcTemplate = template;
    this.dataSource = dataSource;
    this.appConfig = appConfig;
  }

  /**
   * Db call with streams.
   *
   * @param sqlQuery the query
   * @param writer buffer for row content to be output into csv file
   */
  public void buildCsvFromData(String sqlQuery, BufferedWriter writer) {
    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
      throw new CsvCreationException("SQL query is not provided");
    }

    if (writer == null) {
      throw new CsvCreationException("BufferedWriter is null");
    }

    try (writer) {
      StringBuilder line = new StringBuilder();

      jdbcTemplate.query(
          (Connection con) -> {
            return buildPreparedStatement(sqlQuery, con, appConfig.getDataChunkSize());
          },
          new CsvRowCallbackHandler(writer, line, appConfig.getBufferFlushFrequency())
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