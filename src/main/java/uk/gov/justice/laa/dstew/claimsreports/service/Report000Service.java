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

/**
 * Report000Service is responsible for generating and managing report_000.
 * This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report000 data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report000Service extends AbstractReportService {

  private final AppConfig appConfig;

  private static final int MONTHLY_REPORT_DATE = 21;

  public Report000Service(JdbcTemplate jdbcTemplate,
                          S3ClientWrapper s3ClientWrapper, CsvCreationService csvCreationService, MetricsHandler metricsHandler,
                          Clock clock, AppConfig appConfig) {
    super(jdbcTemplate, s3ClientWrapper, csvCreationService, metricsHandler, clock);
    this.appConfig = appConfig;
  }

  @Override
  protected String getDataSourceName() {
    return "claims.mvw_report_000";
  }

  @Override
  protected String getRefreshCommand() {
    return "REFRESH MATERIALIZED VIEW claims.mvw_report_000";
  }

  @Override
  protected String getReportFileName() {
    return "report_000";
  }

  @Override
  public String getReportName() {
    return "REPORT000";
  }

  @Override
  protected String getReportFolder() {
    return "monthly";
  }

  @Override
  protected String getOrderByClause() {
    return " to_char(to_date(\"Submission Period\", 'MON-YYYY'), 'YYYYMM') NULLS LAST,"
        + "    \"Office Account Number\","
        + "    \"Line Number\"";
  }

        @Override
        protected List<String> getExpectedCsvHeaders() {
          return List.of(
          "Firm name", "Firm number", "Office name", "Procurement Area Code", "Procurement Area Description",
          "Access Point Code", "Access Point Description", "Delivery Location", "Submission ID", "Submission Period",
          "Submission For Date", "Submission Status", "Crime Lower Submission Reference", "Legal Help Submission Reference",
          "Mediation Submission Reference", "Schedule Reference", "Line Number", "Date Submitted", "Office Account Number",
          "Client Forename", "Client Surname", "Client Date of Birth", "Gender", "Ethnicity", "Disability",
          "Unique Client Number", "Client Postcode", "First Client Legally Aided", "Client Type Code",
          "Second Client Forename", "Second Client Surname", "Second Client Date of Birth", "Second Client Gender",
          "Second Client Ethnicity", "Second Client Disability", "Second Client Unique Client Number", "Second Client Postcode",
          "Second Client Legally Aided", "Second Client Postal Application Accepted", "Area of Law", "Category of Law Code",
          "Claim ID", "Case Reference Number", "Unique File Number", "Case ID", "Unique Case ID", "Fee Code",
          "Fee Code Description", "Standard Fee Category Code", "Matter Type 1", "Matter Type 2", "Matter Type Code",
          "Case Stage Level", "Stage Reached", "Outcome Code", "Case Start Date", "Case Concluded Date", "Transfer Date",
          "Exemption Criteria Satisfied", "ECF Reference", "Tolerance Indicator", "Referral Source Code", "London Rate Flag",
          "Local Authority Number", "CLA Reference Number", "CLA Exemption Code", "Is Exceptional Claim",
          "Postal Application Accepted", "Type of Advice", "Eligible Client", "Court Location (HPCDS)",
          "Home Office Client Number", "Immigration Prior Authority Number", "AIT Hearing Centre Code", "Legacy Case Flag",
          "IRC Surgery", "Surgery Date", "Number Of Clients Seen At The Surgery",
          "Surgery Clients Resulting in Legal Help Matters", "NRM Advice", "PRN Follow On Work", "Scheme ID",
          "Police Station Court Prison ID", "Is Youth Court", "Police Station Court Attendances Count",
          "Suspects Defendants Count", "Crime Matter Type", "Representation Order Date", "MAAT ID", "Is Duty Solicitor",
          "DSCC Number", "Prison law Prior Approval number", "Outreach Location", "Mediation Time", "Mediation Sessions Count",
          "Mental Health Tribunal Reference", "Medical Reports Count", "Meetings Attended Code",
          "Designated Accredited Representative", "Advice Time", "Travel Time", "Waiting Time", "Fee Type", "Profit Costs",
          "Counsel Fees", "Disbursement Costs", "Travel Waiting Costs", "VAT Rate Applied", "VAT Indicator",
          "JR Form Filling Costs", "Disbursement Amount", "Detention Travel And Waiting Costs Amount", "JR Form Filling Amount",
          "Cost / Damages Recovered", "Detention Travel & Waiting Costs", "Adjourned Hearing Fee Count", "CMRH Oral Count",
          "CMRH Telephone Count", "HO Interview Count", "Substantive Hearing Flag", "Additional Travel Payment Flag",
          "Current Bolt On Adjourned Hearing Count", "Current Bolt On Adjourned Hearing Fee",
          "Current Bolt On CMRH Telephone Count", "Current Bolt On CMRH Telephone Fee", "Current Bolt On CMRH Oral Count",
          "Current Bolt On CMRH Oral Fee", "Current Bolt On Total Fee Amount", "Current Bolt On Fees VAT",
          "Current Bolt On Home Office Interview Fee", "Current Bolt On HO Interviews count", "Current Fixed Fee Amount",
          "Current Hourly Total Amount", "Current Net Profit Costs Amount", "Current Net Cost Of Counsel Amount",
          "Current Disbursement Amount", "Disbursement VAT Costs", "Current Travel And Waiting Costs Amount",
          "Current Detention And Waiting Costs Amount", "Current JR Form Filling Amount", "Current VAT Indicator",
          "Current Fixed Fee VAT", "Current Profit Costs VAT", "Current Counsel Costs VAT", "Current Travel Costs VAT",
          "Current Waiting Costs VAT", "Current JR / Form Filling Costs VAT", "Fixed Fee Amount", "Net Profit Costs Amount",
          "Net Cost Of Counsel Amount", "Net Travel Costs Amount", "Net Waiting Costs Amount", "Bolt On Home Office Interview Fee",
          "Bolt On Adjourned Hearing Fee", "Bolt On CMRH Telephone Fee", "Bolt On CMRH Oral Fee", "Bolt On Substantive Hearing Fee",
          "Current Escape Case Flag", "Is Duplicate Claim", "Amended Flag", "Claim Status", "Has Post Submission Change",
          "Assessed Flag", "Assessed By User ID", "Assessment Updated By User ID", "Assessment Date Time",
          "Assessment Update Date Time", "Assessed Net Waiting Costs Amount", "Initial Calculated Claim Value", "Allowed Total VAT",
          "Allowed Total Inc VAT", "Assessed Total VAT", "Assessed Total Inc VAT", "Final Claim Value");
        }

  // Monthly report
  @Override
  protected boolean runToday() {
    if (appConfig.isForceRunReport000()) {
      log.info("Force run for Report000 is enabled. Running report regardless of date.");
    } else {
      log.info("Force run for Report000 is disabled. Running report only if today is the {}th of the month.", MONTHLY_REPORT_DATE);
    }
    return appConfig.isForceRunReport000() || LocalDate.now(clock).getDayOfMonth() == MONTHLY_REPORT_DATE;
  }
}