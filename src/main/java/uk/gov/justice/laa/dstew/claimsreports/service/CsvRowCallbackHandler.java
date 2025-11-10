package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowCallbackHandler;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * This class defines how each row of data will be appended to the new CSV file, as well as how frequently the output buffer
 * will be flushed to ensure CSV creation remains performant and does not hold too much data in memory during processing.
 * Final buffer flush will need to be done by method that utilises this handler, to ensure there are no remaining rows left in the buffer.
 */
@RequiredArgsConstructor
class CsvRowCallbackHandler implements RowCallbackHandler {
  private final BufferedWriter writer;
  private final Map<String, String> row;
  private final int bufferFlushFrequency;
  private final CsvMapper csvMapper;
  private SequenceWriter sequenceWriter;
  private int rowCount;

  @Override
  public void processRow(ResultSet resultSet) {

    if (resultSet == null) {
      throw new CsvCreationException("Result set invalid");
    }

    try {
      if (resultSet.getMetaData() == null) {
        throw new CsvCreationException("Metadata invalid");
      }

      ResultSetMetaData meta = resultSet.getMetaData();
      int columnCount = meta.getColumnCount();

      // Write header once
      if (resultSet.getRow() == 1) {
        addCsvHeaders(columnCount, meta);
      }

      // Clear Map instead of creating new instance,
      // as performance saving
      row.clear();

      for (int i = 1; i <= columnCount; i++) {
        row.put(meta.getColumnName(i), resultSet.getString(i));
      }
      sequenceWriter.write(row);

      // Regular flush of buffer reduces memory usage when
      // processing large files.
      if (resultSet.getRow() % bufferFlushFrequency == 0) {
        sequenceWriter.flush();
      }

      rowCount++;

    } catch (JacksonIOException | SQLException ex) {
      throw new CsvCreationException("Failure to write data row to new csv file", ex);
    }
  }

  private void addCsvHeaders(int columnCount, ResultSetMetaData meta) throws SQLException {
    CsvSchema.Builder schemaBuilder = CsvSchema.builder().setUseHeader(true);
    for (int i = 1; i <= columnCount; i++) {
      schemaBuilder.addColumn(meta.getColumnName(i));
    }
    CsvSchema schema = schemaBuilder.build();

    // Knows what schema and format to use
    ObjectWriter objectWriter = csvMapper.writer(schema);
    // Streaming writer that handles CSV formatting, quotations, line endings etc.
    sequenceWriter = objectWriter.writeValues(writer);
  }

  public int getRowCount() {
    return rowCount;
  }
}