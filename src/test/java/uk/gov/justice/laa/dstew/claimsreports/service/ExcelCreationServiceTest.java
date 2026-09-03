package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import javax.sql.DataSource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

@ExtendWith(MockitoExtension.class)
class ExcelCreationServiceTest {

  @Mock private DataSource dataSource;

  @Mock private AppConfig appConfig;

  @Mock private MetricsHandler metricsHandler;

  @Mock private Connection connection;

  @Mock private PreparedStatement statement;

  @Mock private ResultSet resultSet;

  @Mock private ResultSetMetaData metaData;

  private ExcelCreationService excelCreationService;

  @TempDir File tempDir;

  @BeforeEach
  void setUp() throws Exception {
    excelCreationService = new ExcelCreationService(dataSource, appConfig, metricsHandler);

    when(appConfig.getDataChunkSize()).thenReturn(1000);
    when(appConfig.getExcelRowAccessWindowSize()).thenReturn(50);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(
            any(), eq(ResultSet.TYPE_FORWARD_ONLY), eq(ResultSet.CONCUR_READ_ONLY)))
        .thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.getMetaData()).thenReturn(metaData);
  }

  @Test
  void shouldCreateValidXlsxForHappyPath() throws Exception {
    when(appConfig.getBufferFlushFrequency()).thenReturn(2);
    when(metaData.getColumnCount()).thenReturn(2);
    when(metaData.getColumnName(1)).thenReturn("Header A");
    when(metaData.getColumnName(2)).thenReturn("Header B");
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getString(1)).thenReturn("A1", "A2");
    when(resultSet.getString(2)).thenReturn("B1", "B2");

    File output = new File(tempDir, "happy.xlsx");
    excelCreationService.buildExcelFromData("SELECT * FROM table", output, "REPORT012");

    assertTrue(output.exists());
    assertTrue(output.length() > 0);
    try (var workbook = new XSSFWorkbook(Files.newInputStream(output.toPath()))) {
      assertEquals("REPORT012", workbook.getSheetAt(0).getSheetName());
      assertEquals("Header A", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
      assertEquals("Header B", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
      assertEquals("A1", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
      assertEquals("B2", workbook.getSheetAt(0).getRow(2).getCell(1).getStringCellValue());
    }
    verify(metricsHandler).setCustomMetric(CustomMetricId.ROWS_WRITTEN, 2);
  }

  @Test
  void shouldCreateHeaderOnlySheetWhenDatasetIsEmpty() throws Exception {
    when(metaData.getColumnCount()).thenReturn(2);
    when(metaData.getColumnName(1)).thenReturn("Header A");
    when(metaData.getColumnName(2)).thenReturn("Header B");
    when(resultSet.next()).thenReturn(false);

    File output = new File(tempDir, "empty.xlsx");
    excelCreationService.buildExcelFromData("SELECT * FROM table", output, "REPORT012");

    try (var workbook = new XSSFWorkbook(Files.newInputStream(output.toPath()))) {
      assertEquals("Header A", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
      assertEquals("Header B", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
      assertEquals(0, workbook.getSheetAt(0).getLastRowNum());
    }
    verify(metricsHandler).setCustomMetric(CustomMetricId.ROWS_WRITTEN, 0);
  }

  @Test
  void shouldHandleLargeDatasetSmokeTest() throws Exception {
    when(appConfig.getBufferFlushFrequency()).thenReturn(2);
    when(metaData.getColumnCount()).thenReturn(2);
    when(metaData.getColumnName(1)).thenReturn("Header A");
    when(metaData.getColumnName(2)).thenReturn("Header B");
    final int rowCount = 5000;
    final int[] currentRow = {0};
    when(resultSet.next()).thenAnswer(invocation -> ++currentRow[0] <= rowCount);
    when(resultSet.getString(anyInt()))
        .thenAnswer(
            invocation -> {
              int column = invocation.getArgument(0);
              return "R" + currentRow[0] + "C" + column;
            });

    File output = new File(tempDir, "large.xlsx");
    excelCreationService.buildExcelFromData("SELECT * FROM table", output, "REPORT012");

    assertTrue(output.exists());
    try (var workbook = new XSSFWorkbook(Files.newInputStream(output.toPath()))) {
      assertEquals(rowCount, workbook.getSheetAt(0).getLastRowNum());
      assertEquals("R5000C2", workbook.getSheetAt(0).getRow(5000).getCell(1).getStringCellValue());
    }
    verify(metricsHandler).setCustomMetric(CustomMetricId.ROWS_WRITTEN, rowCount);
  }

  @Test
  void shouldThrowWhenResultSetMetadataMissing() throws Exception {
    when(resultSet.getMetaData()).thenReturn(null);

    File output = new File(tempDir, "broken.xlsx");
    assertThrows(
        CsvCreationException.class,
        () -> excelCreationService.buildExcelFromData("SELECT * FROM table", output, "REPORT012"));
  }
}
