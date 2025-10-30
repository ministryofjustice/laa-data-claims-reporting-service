package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;

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

  public Report000Service(JdbcTemplate jdbcTemplate, DataSource dataSource, AppConfig appConfig) {
    super(jdbcTemplate, dataSource, appConfig);
  }

  @Override
  protected String getMaterializedViewName() {
    return "claims.mvw_report_000";
  }

  @Override
  public void generateReport() {
    log.info("Generating report from {}", getClass().getSimpleName());
    var csvCreationService = new CsvCreationService(jdbcTemplate, dataSource, appConfig);
    try {
      csvCreationService.buildCsvFromData("SELECT * FROM claims.mvw_report_000", new BufferedWriter(new FileWriter("/tmp/report000.csv")));
    } catch (CsvCreationException e) {
      log.info("Failure to create Report000");
      throw e;
    } catch (IOException e) {
      throw new CsvCreationException("Failure to create Report000: " + e.getMessage());
    }

  }

}