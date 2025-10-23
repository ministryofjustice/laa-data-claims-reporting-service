package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.Assertions;
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
  private ResultSet resultSet;

  @Mock
  private ResultSetMetaData resultSetMetaData;

  @BeforeEach
  void setup() throws IOException {
    line = new StringBuilder();
    stringWriter = new StringWriter();
    writer = new BufferedWriter(stringWriter);
    csvRowCallbackHandler = new CsvRowCallbackHandler(writer, line, 10);
  }

  @Test
  void buildsOutputWithSingleRow() throws SQLException {
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(10);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    csvRowCallbackHandler.processRow(resultSet);
    Assertions.assertFalse(stringWriter.toString().isEmpty());
  }

  @Test
  void willNotFlushBufferIfDataSizeIsSmallerThanBufferFlushValue() throws SQLException {
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(9);
    when(resultSet.getString(anyInt())).thenReturn("data");
    when(resultSetMetaData.getColumnCount()).thenReturn(10);
    csvRowCallbackHandler.processRow(resultSet);
    // This is expected behaviour. The final flush will be done after all rows have been constructed, in the CsvCreationService
    Assertions.assertTrue(stringWriter.toString().isEmpty());
  }

  @Test
  void willNotFlushOnOddRowsWhenFlushSizeIsNotDivisibleByTen() throws SQLException, IOException {
    // Confirms that buffer will not be fully flushed if no. of rows % flush frequent != 0
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, 10);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(19);
    csvRowCallbackHandler.processRow(resultSet);
    verify(spyWriter, times(0)).flush();
  }

  @Test
  void willFlushOnOddRowsWhenFlushSizeIsDivisibleByTen() throws SQLException, IOException {
    // Confirms that buffer will be fully flushed if no. of rows % flush frequent == 0
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, 10);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(20);
    csvRowCallbackHandler.processRow(resultSet);
    verify(spyWriter, times(1)).flush();
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
  void willThrowCsvCreationExceptionIfWriterThrows() throws SQLException, IOException {
    BufferedWriter spyWriter = spy(writer);
    CsvRowCallbackHandler csvRowCallbackHandler = new CsvRowCallbackHandler(spyWriter, line, 10);
    when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
    when(resultSet.getRow()).thenReturn(6);
    doThrow(IOException.class).when(spyWriter).write(any(String.class));
    assertThrows(CsvCreationException.class, () -> csvRowCallbackHandler.processRow(resultSet));
  }
}