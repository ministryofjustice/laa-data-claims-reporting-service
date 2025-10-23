package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ExtendWith(MockitoExtension.class)
public class CsvCreationServiceTest {

  @InjectMocks
  private CsvCreationService csvCreationService;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private BufferedWriter bufferedWriter;

  @Mock
  private PreparedStatement statement;

  @Mock
  Connection connection;

  @Mock
  AppConfig appConfig;

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsNull() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData(null, bufferedWriter));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsEmpty() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("", bufferedWriter));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsBlank() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData(" ", bufferedWriter));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenBufferedWriterIsNull() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", null));
  }

  @Test
  void shouldExecuteQueryWhenValidParametersProvided() {
    csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter);
    verify(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));
  }

  @Test
  void shouldThrowCsvCreationExceptionWhenFlushThrows() throws IOException {
    doThrow(new IOException("Stream error")).when(bufferedWriter).flush();
    Exception ex = assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter));
    assertTrue(ex.getMessage().contains("Failure to write to file: "));
  }

  @Test
  void shouldThrowCsvCreationExceptionIfConnectionFails() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(jdbcTemplate)
        .query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter));
  }

  @Test
  void shouldThrowCsvCreationExceptionWhenStatementIsNull() throws SQLException {
    doAnswer(invocation -> {
      PreparedStatementCreator creator = invocation.getArgument(0);
      RowCallbackHandler handler = invocation.getArgument(1);

      // This will call the lambda, which calls the private buildPreparedStatement method
      creator.createPreparedStatement(connection);

      return null;
    }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));

    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter));
  }

  @Test
  void shouldThrowCsvCreationExceptionWhenCreateStatementThrows() throws SQLException {
    when(connection.prepareStatement(any(), anyInt(), anyInt())).thenThrow(SQLException.class);

    doAnswer(invocation -> {
      PreparedStatementCreator creator = invocation.getArgument(0);

      // This will call the lambda, which calls the private buildPreparedStatement method
      // This method will then throw an exception
      creator.createPreparedStatement(connection);

      return null;
    }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));

    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter));
  }

  @Test
  void willNotThrowIfChunkSizeIsZero() throws SQLException, IOException {
    when(appConfig.getDataChunkSize()).thenReturn(0);
    when(connection.prepareStatement(any(),
        eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY))).thenReturn(statement);

    doAnswer(invocation -> {
      PreparedStatementCreator creator = invocation.getArgument(0);

      // This will call the lambda, which calls the private buildPreparedStatement method,
      // allowing verification of calls inside that method
      creator.createPreparedStatement(connection);

      return statement;
    }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));

    csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter);
    verify(statement, times(1)).setFetchSize(1000);
  }}
