package uk.gov.justice.laa.dstew.claimsreports.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.repository.RefreshableMaterializedView;

/**
 * AbstractReportService serves as a base class for implementing report generation services
 * with support for operations on materialized views. This class provides a framework for
 * refreshing materialized views and generating reports. Subclasses should provide specific
 * implementations for the abstract methods as needed.
 *
 * @param <T> The type of the entity managed by the associated repository.
 * @param <R> The type of the repository, which must be a combination of JpaRepository and
 *            RefreshableMaterializedView, ensuring support for both JPA operations and
 *            refreshing materialized views.
 */
@Slf4j
@Transactional
@RequiredArgsConstructor
public abstract class AbstractReportService<T, R extends JpaRepository<T, ?> & RefreshableMaterializedView> {

  protected final R repository;

  /**
   * Refreshes the associated materialized view by invoking the repository's refresh method.
   * This method is implemented as part of the {@code AbstractReportService}, making it available
   * to all concrete services extending from this base class.
   */
  public void refreshMaterializedView() {
    log.info("Refreshing materialized view for {}", getClass().getSimpleName());
    repository.refreshMaterializedView();
    log.info("Refresh complete for {}", getClass().getSimpleName());
  }

  public abstract void generateReport();

}
