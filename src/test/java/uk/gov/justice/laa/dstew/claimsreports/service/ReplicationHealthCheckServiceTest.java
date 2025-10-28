package uk.gov.justice.laa.dstew.claimsreports.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.service.ReplicationHealthCheckService.ReplicationSummary;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

class ReplicationHealthCheckServiceTest {


  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private ReplicationHealthCheckService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testHealthyReplication() {
    // Given
    LocalDate summaryDate = LocalDate.now().minusDays(1);
    List<String> publicationTables = List.of("claims.table1", "claims.table2");

    Map<String, ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationSummary("claims.table1", 10, 2, "0/16B6C50"),
        "claims.table2", new ReplicationSummary("claims.table2", 5, 1, "0/16B6C40")
    );

    // Mock get tables
    when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
        .thenReturn(publicationTables);

    // Mock getReplicationSummaries() -> returns Map<String, ReplicationSummary>
    when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(Object[].class)))
        .thenReturn(summaries);

    // Mock WAL LSN
    when(jdbcTemplate.queryForObject(eq("SELECT pg_current_wal_lsn()"), eq(String.class)))
        .thenReturn("0/16B6C60");

// Stub for replication summary query
    when(jdbcTemplate.query(contains("FROM claims.replication_summary"),
        any(ResultSetExtractor.class), any(Object[].class)))
        .thenReturn(summaries);

// Stub for count queries
    when(jdbcTemplate.query(contains("WHERE created_on"),
        any(ResultSetExtractor.class), any(Object[].class)))
        .thenAnswer(invocation -> {
          String sql = invocation.getArgument(0);
          if (sql.contains("table1")) return 10L;
          if (sql.contains("table2")) return 5L;
          return 0L;
        });

    when(jdbcTemplate.query(contains("WHERE updated_on"),
        any(ResultSetExtractor.class), any(Object[].class)))
        .thenAnswer(invocation -> {
          String sql = invocation.getArgument(0);
          if (sql.contains("table1")) return 2L;
          if (sql.contains("table2")) return 1L;
          return 0L;
        });

    // When
    ReplicationHealthReport report = service.checkReplicationHealth();

    // Then
    assertTrue(report.isHealthy(), "Expected healthy report");
    assertTrue(report.getFailedChecks().isEmpty());
  }

  @Test
  void testMissingTableDetected() {
    LocalDate summaryDate = LocalDate.now().minusDays(1);
    List<String> publicationTables = List.of("claims.table1", "claims.table2");
    Map<String, ReplicationHealthCheckService.ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationHealthCheckService.ReplicationSummary("claims.table1", 10, 2, "0/16B6C50")
    );

    when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(publicationTables);
    when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(summaryDate))).thenReturn(summaries);
    when(jdbcTemplate.queryForObject(eq("SELECT pg_current_wal_lsn()"), eq(String.class)))
        .thenReturn("0/16B6C60");

    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(10L);
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(2L);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Missing replication summary"));
  }

  @Test
  void testWalProgressAheadTriggersFailure() {
    LocalDate summaryDate = LocalDate.now().minusDays(1);
    List<String> publicationTables = List.of("claims.table1");
    Map<String, ReplicationHealthCheckService.ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationHealthCheckService.ReplicationSummary("claims.table1", 10, 2, "0/16B6D50")
    );

    when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(publicationTables);
    when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(summaryDate))).thenReturn(summaries);
    when(jdbcTemplate.queryForObject(eq("SELECT pg_current_wal_lsn()"), eq(String.class)))
        .thenReturn("0/16B6C60");

// Stub for count queries
    when(jdbcTemplate.query(contains("WHERE created_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(10L);

    when(jdbcTemplate.query(contains("WHERE updated_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(2L);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("WAL LSN in summary"));
  }

  @Test
  void testCountMismatchDetected() {
    LocalDate summaryDate = LocalDate.now().minusDays(1);
    List<String> publicationTables = List.of("claims.table1");
    Map<String, ReplicationHealthCheckService.ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationHealthCheckService.ReplicationSummary("claims.table1", 10, 2, "0/16B6C50")
    );

    when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(publicationTables);
    when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(summaryDate))).thenReturn(summaries);
    when(jdbcTemplate.queryForObject(eq("SELECT pg_current_wal_lsn()"), eq(String.class)))
        .thenReturn("0/16B6C60");

    // mismatch: actual counts differ
    when(jdbcTemplate.queryForObject(startsWith("SELECT count(*) FROM claims.table1"), eq(Long.class), any()))
        .thenReturn(9L);
    when(jdbcTemplate.queryForObject(contains("claims.table1 WHERE updated_on"), eq(Long.class), any(), any()))
        .thenReturn(3L);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Count mismatch"));
  }
}