package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.endpoint.web.Link;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SequenceWriter;
import tools.jackson.databind.SerializationFeature;
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

  @BeforeEach
  void setup() throws IOException {
    row = new LinkedHashMap<>();
    stringWriter = new StringWriter();
    writer = new BufferedWriter(stringWriter);
    csvRowCallbackHandler = new CsvRowCallbackHandler(writer, row, 10, csvMapper);
  }

  @Test
  void willSetupHeaderRowFirstTimeAround() throws SQLException {
    var expectedRowMap = new LinkedHashMap<String, String>();
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    for (int i = 1; i <= 10; i++) {
      when(resultSetMetaData.getColumnName(i)).thenReturn("column_" + i);
      expectedRowMap.put("column_" + i, "data");
    }
    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter).write(expectedRowMap);
  }

  @Test
  void buildsOutputForSubsequentRowsWithoutRebuildingSequenceWriter() throws SQLException {
    var expectedRowMap = new LinkedHashMap<String, String>();
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(10);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    for (int i = 1; i <= 10; i++) {
      when(resultSetMetaData.getColumnName(i)).thenReturn("column_" + i);
      expectedRowMap.put("column_" + i, "data");
    }
    when(csvMapper.writer(any(CsvSchema.class))).thenReturn(objectWriter);
    when(objectWriter.writeValues(writer)).thenReturn(sequenceWriter);

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter).write(expectedRowMap);
  }


  @Test
  void willNotFlushBufferIfDataSizeIsSmallerThanBufferFlushValue() throws SQLException {
    var exampleRow = new LinkedHashMap<String, String>();
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    for (int i = 1; i <= 10; i++) {
      when(resultSetMetaData.getColumnName(i)).thenReturn("column_" + i);
      exampleRow.put("column_" + i, "data");

    }

    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter, times(0)).flush();
  }

  @Test
  void willFlushWhenRowNumberEqualsFlushSize() throws SQLException, IOException {
    // Confirms that buffer will not be fully flushed if no. of rows % flush frequent != 0
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(writer, row, 1, csvMapper);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    for (int i = 1; i <= 10; i++) {
      when(resultSetMetaData.getColumnName(i)).thenReturn("column_" + i);
    }
    csvRowCallbackHandler.processRow(resultSet);
    verify(sequenceWriter, times(1)).flush();
  }
//
//  @Test
//  void willFlushOnOddRowsWhenFlushSizeIsDivisibleByTen() throws SQLException, IOException {
//    // Confirms that buffer will be fully flushed if no. of rows % flush frequent == 0
//    BufferedWriter spyWriter = spy(writer);
//    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, row, 10);
//    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
//    when(resultSet.getRow()).thenReturn(20);
//    csvRowCallbackHandler.processRow(resultSet);
//    verify(spyWriter, times(1)).flush();
//  }
//
//  @Test
//  void willThrowIfResultSetIsNull() throws SQLException, IOException {
//    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(null));
//  }
//
//  @Test
//  void willThrowIfMetadataIsNull() throws SQLException, IOException {
//    when(resultSet.getMetaData()).thenReturn(null);
//    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
//  }
//
//  @Test
//  void willThrowCsvCreationExceptionIfWriterThrows() throws SQLException, IOException {
//    BufferedWriter spyWriter = spy(writer);
//    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, row, 10);
//    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
//    when(resultSet.getRow()).thenReturn(6);
//    doThrow(IOException.class).when(spyWriter).write(any(String.class));
//    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
//  }
}