package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
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
  private MetricsHandler metricsHandler;

  @Mock
  Connection connection;

  @Mock
  AppConfig appConfig;

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsNull() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData(null, bufferedWriter, "test_report"));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsEmpty() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("", bufferedWriter, "test_report"));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenSqlIsBlank() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData(" ", bufferedWriter, "test_report"));
  }

  @Test
  void shouldThrowIllegalArgumentExceptionWhenBufferedWriterIsNull() {
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", null, "test_report"));
  }

  @Test
  void shouldExecuteQueryWhenValidParametersProvided() {
    csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter, "test_report");
    verify(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));
  }

  @Test
  void shouldThrowCsvCreationExceptionWhenFlushThrows() throws IOException {
    doThrow(new IOException("Stream error")).when(bufferedWriter).flush();
    Exception ex = assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter, "test_report"));
    assertTrue(ex.getMessage().contains("Failure to write to file"));
  }

  @Test
  void shouldThrowCsvCreationExceptionIfConnectionFails() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(jdbcTemplate)
        .query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter, "test_report"));
  }

  @Test
  void shouldThrowCsvCreationExceptionWhenStatementIsNull() throws SQLException {
    doAnswer(invocation -> {
      PreparedStatementCreator creator = invocation.getArgument(0);
      RowCallbackHandler handler = invocation.getArgument(1);

      try (PreparedStatement statement = creator.createPreparedStatement(connection)) {
        // This will call the lambda, which calls the private buildPreparedStatement method
      }

      return null;
    }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));

    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter, "test_report"));
  }

  @SuppressFBWarnings("OBL_UNSATISFIED_OBLIGATION")
  @Test
  void shouldThrowCsvCreationExceptionWhenCreateStatementThrows() throws SQLException {
    when(connection.prepareStatement("SELECT * FROM ANY_REPORT.DATA", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY))
        .thenThrow(SQLException.class);

    doAnswer(invocation -> {
      PreparedStatementCreator creator = invocation.getArgument(0);
      try (PreparedStatement statement = creator.createPreparedStatement(connection)) {
        // Any returned statement will be cleaned up by the try-with-resources.
      }
      return null;
    }).when(jdbcTemplate).query(any(PreparedStatementCreator.class), any(CsvRowCallbackHandler.class));

    assertThrows(CsvCreationException.class, () -> csvCreationService.buildCsvFromData("SELECT * FROM ANY_REPORT.DATA", bufferedWriter, "test_report"));
  }
}