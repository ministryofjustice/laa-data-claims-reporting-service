package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowCallbackHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * Some docs.
 */
class CsvRowCallbackHandler implements RowCallbackHandler {
  protected AppConfig appConfig;
  private final BufferedWriter writer;
  private final StringBuilder line;

  @Autowired
  public CsvRowCallbackHandler(BufferedWriter writer, StringBuilder line, AppConfig appConfig) {
    this.appConfig = appConfig;
    this.writer = writer;
    this.line = line;
  }

  @Override
  public void processRow(ResultSet resultSet) throws SQLException {
    if (resultSet == null) {
      throw new CsvCreationException("Result set invalid");
    }

    if (resultSet.getMetaData() == null) {
      throw new CsvCreationException("Metadata invalid");
    }

    var flushSize = ((appConfig.getBufferFlushFrequency() != 0) ? appConfig.getBufferFlushFrequency() : 1000);

    try {
      ResultSetMetaData meta = resultSet.getMetaData();
      int columnCount = meta.getColumnCount();

      // Write header once
      if (resultSet.getRow() == 1) {
        System.out.println("FLUSH: " + flushSize);

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
      if (resultSet.getRow() % flushSize == 0) {
        writer.flush();
      }


    } catch (IOException ex) {
      throw new CsvCreationException("Failure to write data row to new csv file: "
          + ex.getMessage());
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
