package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.repository.Report000Repository;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link Report000Service}.
 */
class Report000ServiceTest {

  private Report000Repository repository;
  private Report000Service service;

  @BeforeEach
  void setUp() {
    repository = mock(Report000Repository.class);
    service = new Report000Service(repository);
  }

  @Test
  void refreshMaterializedView_ShouldCallRepositoryMethod() {
    // when
    service.refreshMaterializedView();
    // then
    verify(repository, times(1)).refreshMaterializedView();
    verifyNoMoreInteractions(repository);
  }

}