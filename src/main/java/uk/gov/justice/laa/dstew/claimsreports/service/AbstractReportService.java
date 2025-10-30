package uk.gov.justice.laa.dstew.claimsreports.service;

import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.service.s3.FileUploader;

/**
 * AbstractReportService serves as a base class for implementing report generation services
 * with support for operations on materialized views. This class provides a framework for
 * refreshing materialized views and generating reports. Subclasses should provide specific
 * implementations for the abstract methods as needed.
 *
 */
@Slf4j
@Transactional
@AllArgsConstructor
public abstract class AbstractReportService {

  protected final JdbcTemplate jdbcTemplate;
  protected final DataSource dataSource;
  protected final AppConfig appConfig;
  protected final FileUploader fileUploader;

  /**
   * Gets the name of the materialized view associated with the current service.
   * This method is intended to be implemented by subclasses to define the specific
   * materialized view they are operating on.
   *
   * @return the name of the materialized view as a String
   */
  protected abstract String getMaterializedViewName();

  /**
   * Refreshes the associated materialized view.
   */
  @Transactional
  public void refreshMaterializedView() {
    String viewName = getMaterializedViewName();
    log.info("Refreshing materialized view {}", viewName);

    jdbcTemplate.execute("REFRESH MATERIALIZED VIEW " + viewName);

    log.info("Refresh complete for {}", viewName);
  }

  public abstract void generateReport();

}
