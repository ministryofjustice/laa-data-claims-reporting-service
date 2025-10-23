package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowCallbackHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * This class defines how each row of data will be appended to the new CSV file, as well as how frequently the output buffer
 * will be flushed to ensure CSV creation remains performant and does not hold too much data in memory during processing.
 * Final buffer flush will need to be done by method that utilises this handler, to ensure there are no remaining rows left in the buffer.
 */
class CsvRowCallbackHandler implements RowCallbackHandler {
  private final BufferedWriter writer;
  private final StringBuilder line;
  private final int bufferFlushFrequency;

  @Autowired
  public CsvRowCallbackHandler(BufferedWriter writer, StringBuilder line, int bufferFlushFreq) {
    this.writer = writer;
    this.line = line;
    this.bufferFlushFrequency = bufferFlushFreq;
  }

  @Override
  public void processRow(ResultSet resultSet) throws SQLException {
    if (resultSet == null) {
      throw new CsvCreationException("Result set invalid");
    }

    if (resultSet.getMetaData() == null) {
      throw new CsvCreationException("Metadata invalid");
    }

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

      // Regular flush of buffer reduces memory usage when
      // processing large files.
      if (resultSet.getRow() % bufferFlushFrequency == 0) {
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