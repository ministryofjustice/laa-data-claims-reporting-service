package uk.gov.justice.laa.dstew.claimsreports;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * Data access object class to provide interface between application and database layer.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ReportDao {
  private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;
  private static final int DATA_CHUNK_SIZE = 1000;
  private static final int BUFFER_FLUSH_INTERVAL = 5000;

  /**
   * Db call with streams.
   *
   * @param sqlQuery the query
   * @param filePath the file location
   */
  public void buildCsvFromData(String sqlQuery, String filePath) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      StringBuilder line = new StringBuilder();

      jdbcTemplate.query(
          (Connection con) -> {
            return buildPreparedStatement(sqlQuery, con);
          },
          resultSet -> {
            try {
              ResultSetMetaData meta = resultSet.getMetaData();
              int columnCount = meta.getColumnCount();

              // Write header once
              if (resultSet.getRow() == 1) {
                for (int i = 1; i <= columnCount; i++) {
                  buildRow(line, meta.getColumnName(i), i, columnCount);
                }
                writer.write(line.toString());
                writer.write("\n");
              }

              // Clear StringBuilder instead of creating new instance,
              // as performance saving
              line.setLength(0);

              // Write row
              for (int i = 1; i <= columnCount; i++) {
                buildRow(line, resultSet.getString(i), i, columnCount);
              }
              writer.write(line.toString());
              writer.write("\n");

              // Regular flush of buffer reduced memory usage when
              // processing large files
              if (resultSet.getRow() % BUFFER_FLUSH_INTERVAL == 0) {
                writer.flush();
              }


            } catch (IOException ex) {
              throw new CsvCreationException("Failure to write data rows to new csv file: "
                  + ex.getMessage());
            }
          }

      );

      log.info("CSV export completed: " + filePath);

    } catch (IOException ex) {
      throw new CsvCreationException("Failure to write to file name: " + ex.getMessage());
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
  private static PreparedStatement buildPreparedStatement(String sqlQuery, Connection con) {
    try {
      con.setAutoCommit(false);
      PreparedStatement statement = con.prepareStatement(sqlQuery,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
      statement.setFetchSize(DATA_CHUNK_SIZE);
      return statement;
    } catch (SQLException ex) {
      throw new CsvCreationException("Failed on creation of prepared statement "
          + ex.getNextException());
    }
  }

  /**
   * Uses {StringBuilder} to create a row of data; each value is separated by a comma.
   *
   * @param line row number
   * @param value contents of cell
   * @param colNo column number
   * @param columnCount total columns in sheet
   */
  private static void buildRow(StringBuilder line, String value, int colNo, int columnCount) {
    line.append(value != null ? value : "");
    if (colNo < columnCount) {
      line.append(",");
    }
  }
}
