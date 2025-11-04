package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowCallbackHandler;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * This class defines how each row of data will be appended to the new CSV file, as well as how frequently the output buffer
 * will be flushed to ensure CSV creation remains performant and does not hold too much data in memory during processing.
 * Final buffer flush will need to be done by method that utilises this handler, to ensure there are no remaining rows left in the buffer.
 */
@Slf4j
class CsvRowCallbackHandler implements RowCallbackHandler {
  private final BufferedWriter writer;
  private final int bufferFlushFrequency;
  private CsvSchema schema;
  private final Map<String, String> row;
  private SequenceWriter sequenceWriter;


  @Autowired
  public CsvRowCallbackHandler(BufferedWriter writer, Map<String, String> row, int bufferFlushFreq) {
    this.writer = writer;
    this.row = row;
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

//    try {
      ResultSetMetaData meta = resultSet.getMetaData();
      int columnCount = meta.getColumnCount();

//      log.info("column count: " + columnCount);

      // Write header once
      if (resultSet.getRow() == 1) {

        for (int i = 1; i <= columnCount; i++) {
          log.info("I have the first row");

          CsvSchema.Builder schemaBuilder = CsvSchema.builder().setUseHeader(true);
          for (int j = 1; j <= columnCount; j++) {
            schemaBuilder.addColumn(meta.getColumnName(j));
          }
          schema = schemaBuilder.build();

          CsvMapper mapper = new CsvMapper();
          // Knows what schema and format to use
          ObjectWriter objectWriter = mapper.writer(schema);
          // Streaming writer that handles CSV formatting, quotations, line endings etc.
          sequenceWriter = objectWriter.writeValues(writer);

//          buildRow(line, meta.getColumnName(i), i, columnCount);
        }
//        writer.write(line.toString());
//        writer.write("\n");
      }






      // Clear Map instead of creating new instance,
      // as performance saving
      row.clear();


      for (int i = 1; i <= columnCount; i++) {
        row.put(meta.getColumnName(i), resultSet.getString(i));
      }
//      log.info("I have written the row");
      sequenceWriter.write(row);

//
//      // Write row
//      for (int i = 1; i <= columnCount; i++) {
//        buildRow(line, resultSet.getString(i), i, columnCount);
//      }
//      writer.write(line.toString());
//      writer.write("\n");

      // Regular flush of buffer reduces memory usage when
      // processing large files.
      if (resultSet.getRow() % bufferFlushFrequency == 0) {
        log.info("I flush 1");
        sequenceWriter.flush();
//        writer.flush();
      }

//    } catch (IOException ex) {
//      throw new CsvCreationException("Failure to write data row to new csv file: "
//          + ex.getMessage());
//    }
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