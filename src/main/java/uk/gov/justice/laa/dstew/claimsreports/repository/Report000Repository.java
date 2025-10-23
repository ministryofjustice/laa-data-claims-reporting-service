package uk.gov.justice.laa.dstew.claimsreports.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.dstew.claimsreports.entity.Report000Entity;

/**
 * Repository interface for managing Report000Entity entities and refreshing the associated materialized view.
 *
 * <p>
 * This interface extends JpaRepository for standard CRUD operations and RefreshableMaterializedView
 * for handling materialized view refresh functionality.
 **/
@Repository
public interface Report000Repository extends JpaRepository<Report000Entity, String>, RefreshableMaterializedView {

  /**
   * Refreshes the materialized view associated with the Report000Entity.
   *
   * <p>
   * The method is annotated with @Modifying to indicate that it modifies the database state (and the query doesn't return any data itself)
   * and @Transactional to indicate that it requires a transactional context.
   */
  @Override
  @Modifying
  @Transactional
  @Query(value = "REFRESH MATERIALIZED VIEW claims.mvw_report_000", nativeQuery = true)
  void refreshMaterializedView();

}
