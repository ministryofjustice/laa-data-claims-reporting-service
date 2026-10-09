package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import uk.gov.justice.laa.dstew.claimsreports.exception.SharePointUploadException;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;
import uk.gov.justice.laa.dstew.claimsreports.service.sharepoint.SharePointUploadService;

/**
 * Report012Service is responsible for generating and managing report_012. This service extends the
 * AbstractReportService and provides an implementation for the report generation process.
 * Responsibilities: - Implements report generation logic for Report012 data. - Utilizes the
 * inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report012Service extends AbstractReportService {

  private final AppConfig appConfig;
  private final ExcelCreationService excelCreationService;
  private final SharePointUploadService sharePointUploadService;

  /** Creates Report012Service. */
  public Report012Service(
      JdbcTemplate jdbcTemplate,
      S3ClientWrapper s3ClientWrapper,
      CsvCreationService csvCreationService,
      MetricsHandler metricsHandler,
      Clock clock,
      AppConfig appConfig,
      ExcelCreationService excelCreationService,
      SharePointUploadService sharePointUploadService) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
    this.appConfig = appConfig;
    this.excelCreationService = excelCreationService;
    this.sharePointUploadService = sharePointUploadService;
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
        "Provider office account number",
        "Submission month",
        "Area of law",
        "Original submission value",
        "Date submission was uploaded");
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
      uploadXlsxReport(tempFile, s3FileKey);
      return;
    }
    super.uploadReportFile(tempFile, s3FileKey);
  }

  private void uploadXlsxReport(File tempFile, String s3FileKey) {
    String xlsxContentType =
        uk.gov.justice.laa.dstew.claimsreports.service.sharepoint.SharePointProperties
            .XLSX_MIME_TYPE;
    boolean s3Succeeded = false;
    boolean sharePointSucceeded = false;
    RuntimeException s3Failure = null;
    RuntimeException sharePointFailure = null;
    try {
      s3ClientWrapper.uploadFile(tempFile, s3FileKey, xlsxContentType);
      s3Succeeded = true;
    } catch (RuntimeException ex) {
      s3Failure = ex;
      log.warn("S3 upload failed for {}: {}", getReportName(), ex.getMessage());
    }

    if (appConfig.isEnableRep012SharePointUpload()) {
      try {
        var result = sharePointUploadService.uploadFile(tempFile, getCurrentReportFileName());
        sharePointSucceeded = true;
        log.info("SharePoint upload complete for {} at {}", getReportName(), result.webUrl());
      } catch (SharePointUploadException ex) {
        sharePointFailure = ex;
        log.warn("SharePoint upload failed for {}: {}", getReportName(), ex.getMessage());
      }
    }

    if (!s3Succeeded && !sharePointSucceeded) {
      throw new CsvUploadException(
          "S3 and SharePoint uploads both failed for " + getReportName(),
          s3Failure != null ? s3Failure : sharePointFailure);
    }
  }

  private String getCurrentReportFileName() {
    return getReportFileName() + "_" + LocalDate.now(clock) + getReportFileExtension();
  }
}
