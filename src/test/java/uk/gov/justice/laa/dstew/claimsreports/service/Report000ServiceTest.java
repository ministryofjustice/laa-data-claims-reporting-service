package uk.gov.justice.laa.dstew.claimsreports.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import uk.gov.justice.laa.dstew.claimsreports.config.AppConfig;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvCreationException;
import uk.gov.justice.laa.dstew.claimsreports.repository.Report000Repository;

import static org.mockito.Mockito.*;

import javax.sql.DataSource;

/**
 * Unit tests for {@link Report000Service}.
 */
class Report000ServiceTest {

  private Report000Repository repository;
  private Report000Service service;
  private JdbcTemplate jdbcTemplate;
  private DataSource dataSource;
  private AppConfig appConfig;
  private CsvCreationService creationService;

  @BeforeEach
  void setUp() {
    repository = mock(Report000Repository.class);
    jdbcTemplate = mock(JdbcTemplate.class);
    dataSource = mock(DataSource.class);
    appConfig = mock(AppConfig.class);
    creationService = mock(CsvCreationService.class);
    service = new Report000Service(repository,jdbcTemplate, dataSource, appConfig);
  }

  @Test
  void refreshMaterializedView_ShouldCallRepositoryMethod() {
    // when
    service.refreshMaterializedView();
    // then
    verify(repository, times(1)).refreshMaterializedView();
    verifyNoMoreInteractions(repository);
  }

  @Test
  void willThrowCsvExceptionWhenCsvServiceThrows() {
    doThrow(new CsvCreationException("Simulated SQL error"))
        .when(jdbcTemplate)
        .query(any(PreparedStatementCreator.class), any(RowCallbackHandler.class));
    Assertions.assertThrows(CsvCreationException.class, () -> service.generateReport());
  }

}