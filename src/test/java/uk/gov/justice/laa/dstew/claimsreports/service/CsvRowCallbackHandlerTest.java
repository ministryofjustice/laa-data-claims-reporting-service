package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.dataformat.csv.CsvMapper;
import tools.jackson.dataformat.csv.CsvSchema;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class CsvRowCallbackHandlerTest {
  private CsvRowCallbackHandler csvRowCallbackHandler;
  private BufferedWriter writer;
  private StringWriter stringWriter;
  private Map<String, String> row;

  @Mock
  private ResultSet resultSet;

  @Mock
  private ResultSetMetaData resultSetMetaData;

  @Mock
  private SequenceWriter sequenceWriter;

  @Mock
  private ObjectWriter objectWriter;

  @Mock
  private CsvMapper csvMapper;

  private final Map<String, String> expectedDataRow = new LinkedHashMap<>();

  @BeforeEach
  void setup() {
    row = new LinkedHashMap<>();
    stringWriter = new StringWriter();
    writer = new BufferedWriter(stringWriter);
    csvRowCallbackHandler = new CsvRowCallbackHandler(writer, row, 10, csvMapper);
    reset(sequenceWriter, resultSetMetaData);
    for (int i = 1; i <= 10; i++) {
      expectedDataRow.put("column_" + i, "data");
    }
  }

  @Test
  void willSetupHeaderRowFirstTimeAround() throws SQLException {
    setupResultSetData(1, "data");

    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter).write(expectedDataRow);
  }

  @Test
  void buildsOutputForSubsequentRowsWithoutRebuildingSequenceWriter() throws SQLException {
    buildFirstRowAndSchema();
    var secondRowMap = new LinkedHashMap<String, String>();
    setupResultSetData(10, "second_data_row");
    for (int i = 1; i <= 10; i++) {
      secondRowMap.put("column_" + i, "second_data_row");
    }
    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter, times(1)).write(secondRowMap);
    verify(csvMapper, times(0)).writer();

  }

  @Test
  void willNotFlushBufferIfDataSizeIsSmallerThanBufferFlushValue() throws SQLException {
    setupResultSetData(1, "data");

    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter, times(0)).flush();
  }

  @Test
  void willFlushWhenRowNumberEqualsFlushSize() throws SQLException, IOException {
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(writer, row, 1, csvMapper);
    setupResultSetData(1, "data");

    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter, times(1)).flush();
  }

  @Test
  void willThrowIfResultSetIsNull() {
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(null));
  }

  @Test
  void willThrowIfMetadataIsNull() throws SQLException {
    when(resultSet.getMetaData()).thenReturn(null);
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }

  @Test
  void willThrowCsvCreationExceptionIfWriterThrows() throws SQLException {
    setupResultSetData(1, "data");

    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);
    when(sequenceWriter.write(any())).thenThrow(JacksonIOException.class);

    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }

  @Test
  void willThrowCsvCreationExceptionIfResultSetThrows() throws SQLException {
    when(resultSet.getMetaData()).thenThrow(SQLException.class);
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }

  @Test
  void willHandleCommaInData() throws SQLException {
    var dataWithCommaMap = new LinkedHashMap<String, String>();
    setupResultSetData(1, "Data, Mrs. S");
    for (int i = 1; i <= 10; i++) {
      dataWithCommaMap.put("column_" + i, "Data, Mrs. S");
    }

    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter).write(dataWithCommaMap);
  }

  private void setupResultSetData(int rowNo, String data) throws SQLException {
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(rowNo);
    when(resultSet.getString(anyInt())).thenReturn(data);
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    for (int i = 1; i <= 10; i++) {
      when(resultSetMetaData.getColumnName(i)).thenReturn("column_" + i);
    }
  }

  // To process subsequent rows you need to process 1st row so it builds the headers etc
  private void buildFirstRowAndSchema() throws SQLException {
    setupResultSetData(1, "data");
    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);

    // Resets verify counters
    reset(sequenceWriter);
  }
}