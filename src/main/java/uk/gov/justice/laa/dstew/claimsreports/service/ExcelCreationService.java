package uk.gov.justice.laa.dstew.claimsreports.service;

import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.PrometheusConfiguration.CustomMetricId;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

/**
 * Builds XLSX files directly from JDBC row streams using SXSSF to bound memory usage.
 */
@Service
@Slf4j
@AllArgsConstructor
public class ExcelCreationService {

  private final DataSource dataSource;
  private final AppConfig appConfig;
  private final MetricsHandler metricsHandler;

  /**
   * Builds an Excel file by streaming rows from the database and writing to a temp file on disk.
   *
   * @param sqlQuery SELECT query for report data
   * @param outputFile destination file
   * @param reportName report name used for logging and sheet naming
   */
  @SuppressFBWarnings(
      value = "SQL_INJECTION_JDBC",
      justification = "Report SQL is assembled from validated service-owned identifiers before reaching this method."
  )
  public void buildExcelFromData(String sqlQuery, File outputFile, String reportName) {
    if (sqlQuery == null || sqlQuery.trim().isEmpty()) {
      throw new CsvCreationException("SQL query is not provided");
    }
    if (outputFile == null) {
      throw new CsvCreationException("Output file is null");
    }

    SXSSFWorkbook workbook = null;
    try (Connection connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement statement = connection.prepareStatement(
          sqlQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
        statement.setFetchSize(appConfig.getDataChunkSize());

        workbook = new SXSSFWorkbook(appConfig.getExcelRowAccessWindowSize());
        workbook.setCompressTempFiles(true);
        SXSSFSheet sheet = workbook.createSheet(reportName);
        int rowsWritten = writeRows(statement.executeQuery(), sheet);

        try (OutputStream outputStream = java.nio.file.Files.newOutputStream(outputFile.toPath())) {
          workbook.write(outputStream);
        }

        log.info("Excel creation completed for {}", sanitise(reportName));
        log.info("Rows written for {}: {}", sanitise(reportName), rowsWritten);
        metricsHandler.setCustomMetric(CustomMetricId.ROWS_WRITTEN, rowsWritten);
      }
    } catch (IOException | SQLException ex) {
      throw new CsvCreationException("Failure during Excel creation of " + reportName, ex);
    } finally {
      if (workbook != null) {
        workbook.dispose();
      }
    }
  }

  private int writeRows(ResultSet resultSet, SXSSFSheet sheet) throws SQLException, IOException {
    ResultSetMetaData metaData = resultSet.getMetaData();
    if (metaData == null) {
      throw new CsvCreationException("Metadata invalid");
    }
    int columnCount = metaData.getColumnCount();
    writeHeaderRow(sheet, metaData, columnCount);

    int rowCount = 0;
    while (resultSet.next()) {
      Row row = sheet.createRow(rowCount + 1);
      for (int i = 1; i <= columnCount; i++) {
        Cell cell = row.createCell(i - 1);
        String value = resultSet.getString(i);
        if (value != null) {
          value = value.replace("\n", "").replace("\r", "");
        }
        cell.setCellValue(value);
      }
      rowCount++;
      if (rowCount % appConfig.getBufferFlushFrequency() == 0) {
        sheet.flushRows(appConfig.getExcelRowAccessWindowSize());
      }
    }
    return rowCount;
  }

  private void writeHeaderRow(SXSSFSheet sheet, ResultSetMetaData metaData, int columnCount) throws SQLException {
    Row header = sheet.createRow(0);
    for (int i = 1; i <= columnCount; i++) {
      header.createCell(i - 1).setCellValue(metaData.getColumnName(i));
    }
  }
}
