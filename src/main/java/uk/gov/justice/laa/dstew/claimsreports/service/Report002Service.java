package uk.gov.justice.laa.dstew.claimsreports.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.config.MetricsHandler;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.S3ClientWrapper;

/** Report002Service is responsible for generating and managing report_002. */
@Slf4j
@Service
public class Report002Service extends AbstractReportService {

  private static final int MONTHLY_REPORT_DATE = 21;
  private final AppConfig appConfig;

  public Report002Service(
      JdbcTemplate jdbcTemplate,
      S3ClientWrapper s3ClientWrapper,
      CsvCreationService csvCreationService,
      MetricsHandler metricsHandler,
      Clock clock,
      AppConfig appConfig) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
    this.appConfig = appConfig;
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_002";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_002";
  }

  @Override
  protected String getReportFileName() {
    return "report_002";
  }

  @Override
  public String getReportName() {
    return "REPORT002";
  }

  @Override
  protected String getReportFolder() {
    return "monthly";
  }

  @Override
  protected String getOrderByClause() {
    return " to_char(to_date(\"Submission for date\", 'MON-YYYY'), 'YYYYMM') NULLS LAST,"
        + "    \"Office code\","
        + "    \"Category code\","
        + "    \"Procurement area code\","
        + "    \"Access point code\"";
  }

  @Override
  protected List<String> getExpectedCsvHeaders() {
    return List.of(
        "Firm name",
        "Firm number",
        "File name",
        "Office code",
        "Submission for date",
        "Category code",
        "Procurement area code",
        "Procurement area desc",
        "Access point code",
        "Access point desc",
        "Schedule reference",
        "Mediation type",
        "New cases count");
  }

  @Override
  protected boolean runToday() {
    if (appConfig.isForceRunReport002()) {
      log.info("Force run for Report002 is enabled. Running report regardless of date.");
    } else {
      log.info(
          "Force run for Report002 is disabled. Running report only if today is the {}th of the month.",
          MONTHLY_REPORT_DATE);
    }
    return appConfig.isForceRunReport002()
        || LocalDate.now(clock).getDayOfMonth() == MONTHLY_REPORT_DATE;
  }
}
