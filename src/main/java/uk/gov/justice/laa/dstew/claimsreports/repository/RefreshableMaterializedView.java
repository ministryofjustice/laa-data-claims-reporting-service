package uk.gov.justice.laa.dstew.claimsreports.repository;

/**
 * Interface representing a refreshable materialized view.
 * Classes implementing this interface must provide a mechanism
 * to refresh the content of a materialized view, typically in a database.
 */
public interface RefreshableMaterializedView {
  void refreshMaterializedView();
}
