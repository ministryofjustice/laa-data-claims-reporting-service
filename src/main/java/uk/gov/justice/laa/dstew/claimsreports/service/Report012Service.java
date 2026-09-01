package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/**
 * Report012Service is responsible for generating and managing report_012.
 * This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report012 data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report012Service extends AbstractReportService {

  private final AppConfig appConfig;
  private final ExcelCreationService excelCreationService;

  /**
   * Creates Report012Service.
   */
  public Report012Service(JdbcTemplate jdbcTemplate,
                          S3ClientWrapper s3ClientWrapper,
                          CsvCreationService csvCreationService,
                          MetricsHandler metricsHandler,
                          Clock clock,
                          AppConfig appConfig,
                          ExcelCreationService excelCreationService) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
    this.appConfig = appConfig;
    this.excelCreationService = excelCreationService;
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_012";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_012";
  }

  @Override
  protected String getReportFileName() {
    return "report_012";
  }

  @Override
  public String getReportName() {
    return "REPORT012";
  }

  @Override
  protected String getReportFolder() {
    return "daily";
  }

  @Override
  protected String getOrderByClause() {
    return " \"Provider office account number\","
        + "    to_char(to_date(\"Submission month\", 'MON-YYYY'), 'YYYYMM'),"
        + "    \"Area of law\"";
  }

  @Override
  protected List<String> getExpectedCsvHeaders() {
    return List.of(
    "Provider office account number", "Submission month", "Area of law",
    "Original submission value", "Date submission was uploaded");
  }

  // Daily report
  @Override
  protected boolean runToday() {
    return true;
  }

  @Override
  protected String getReportFileExtension() {
    return appConfig.isEnableRep012Xlsx() ? ".xlsx" : ".csv";
  }

  @Override
  protected void writeReportToTempFile(String sql, File tempFile) throws IOException {
    if (appConfig.isEnableRep012Xlsx()) {
      excelCreationService.buildExcelFromData(sql, tempFile, getReportName());
      return;
    }
    super.writeReportToTempFile(sql, tempFile);
  }

  @Override
  protected void uploadReportFile(File tempFile, String s3FileKey) {
    if (appConfig.isEnableRep012Xlsx()) {
      s3ClientWrapper.uploadFile(tempFile, s3FileKey,
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
      return;
    }
    super.uploadReportFile(tempFile, s3FileKey);
  }
}