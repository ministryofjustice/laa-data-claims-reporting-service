package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
public class CsvRowCallbackHandlerTest {

  private CsvRowCallbackHandler csvRowCallbackHandler;
  private BufferedWriter writer;
  private StringWriter stringWriter;
  private StringBuilder line;

  @Mock
  private AppConfig appConfig;

  @Mock
  private ResultSet resultSet;

  @Mock
  private ResultSetMetaData resultSetMetaData;

  @BeforeEach
  void setup() throws IOException {
    line = new StringBuilder();
    stringWriter = new StringWriter();
    writer = new BufferedWriter(stringWriter);
    csvRowCallbackHandler = new CsvRowCallbackHandler(writer, line, appConfig);
  }

  @Test
  void buildsOutputWithSingleRow() throws SQLException {
    when(appConfig.getBufferFlushSize()).thenReturn(1);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    when(resultSetMetaData.getColumnName(anyInt())).thenReturn("col_heading");
    csvRowCallbackHandler.processRow(resultSet);
    Assertions.assertEquals("col_heading,col_heading,col_heading,col_heading,col_heading,col_heading,col_heading,col_heading,col_heading,col_heading\ndata,data,data,data,data,data,data,data,data,data\n", stringWriter.toString());
  }

  @Test
  void willNotFlushBufferIfDataSizeIsSmallerThanBufferFlushValue() throws SQLException {
    when(appConfig.getBufferFlushSize()).thenReturn(500);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    when(resultSetMetaData.getColumnName(anyInt())).thenReturn("col_heading");
    csvRowCallbackHandler.processRow(resultSet);
    Assertions.assertTrue(stringWriter.toString().isEmpty());
  }

  @Test
  void willNotFlushOnOddRowsWhenFlushSizeIsTwo() throws SQLException, IOException {
    when(appConfig.getBufferFlushSize()).thenReturn(2);
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, appConfig);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(5);
    csvRowCallbackHandler.processRow(resultSet);
    verify(spyWriter, times(0)).flush();
  }

  @Test
  void willFlushOnEvenRowsWhenFlushSizeIsTwo() throws SQLException, IOException {
    when(appConfig.getBufferFlushSize()).thenReturn(2);
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, appConfig);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(6);
    csvRowCallbackHandler.processRow(resultSet);
    verify(spyWriter, times(1)).flush();
  }

  @Test
  void willNotThrowIfFlushSizeIsZero() throws SQLException, IOException {
    when(appConfig.getBufferFlushSize()).thenReturn(0);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(1);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    when(resultSetMetaData.getColumnName(anyInt())).thenReturn("col_heading");
    assertDoesNotThrow(() -> csvRowCallbackHandler.processRow(resultSet));
  }

  @Test
  void willThrowIfResultSetIsNull() throws SQLException, IOException {
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(null));
  }

  @Test
  void willThrowIfMetadataIsNull() throws SQLException, IOException {
    when(resultSet.getMetaData()).thenReturn(null);
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }

  @Test
  void willThrowIfErrorWithWriter() throws SQLException, IOException {
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, appConfig);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(6);
    doThrow(IOException.class).when(spyWriter).write(any(String.class));
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }
}
