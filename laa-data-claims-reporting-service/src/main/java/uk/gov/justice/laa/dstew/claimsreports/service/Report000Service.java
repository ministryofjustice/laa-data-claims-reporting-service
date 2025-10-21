package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.entity.Report000Entity;
import uk.gov.justice.laa.dstew.claimsreports.repository.Report000Repository;

/**
 * Report000Service is responsible for generating and managing reports specific to
 * the Report000Entity. This service extends the AbstractReportService and provides
 * an implementation for the report generation process.
 * Responsibilities:
 * - Implements report generation logic for Report000Entity data.
 * - Utilizes the inherited functionality to refresh materialized views as needed.
 */
@Slf4j
@Service
public class Report000Service extends AbstractReportService<Report000Entity, Report000Repository> {

  public Report000Service(Report000Repository repository) {
    super(repository);
  }

  @Override
  public void generateReport() {
    log.info("Generating report from {}", getClass().getSimpleName());
  }

}