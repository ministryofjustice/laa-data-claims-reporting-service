package uk.gov.justice.laa.dstew.claimsreports.service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationSummary;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;
import uk.gov.justice.laa.dstew.claimsreports.repository.ReplicationMetadataRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

class ReplicationHealthCheckServiceTest {

  //Mock WAL (Write Ahead Log) LSNs (Log Sequence Numbers) to mimic various replication test scenarios
  public static final String OLD_WAL_LSN = "0/16B6C40";
  public static final String MID_WAL_LSN = "0/16B6C50";
  public static final String RECENT_WAL_LSN = "0/16B6C60";
  public static final String LATEST_WAL_LSN = "0/16B6C70";

  //Other constants used in test scenarios
  public static final long TABLE1_RECORD_COUNT = 10L;
  public static final long TABLE2_RECORD_COUNT = 5L;
  public static final long TABLE1_UPDATE_COUNT = 2L;
  public static final long TABLE2_UPDATE_COUNT = 1L;
  public static final long TABLE1_INCORRECT_RECORD_COUNT = 9L;
  public static final long TABLE2_INCORRECT_RECORD_COUNT = 3L;

  @Mock
  private Clock clock;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private ReplicationMetadataRepository metadataRepository;

  @InjectMocks
  private ReplicationHealthCheckService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Make clock.now() return a fixed instant
    Instant fixedInstant = Instant.parse("2025-11-03T05:00:00Z");
    when(clock.instant()).thenReturn(fixedInstant);
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());
  }

  @Test
  void testHealthyReplication() {
    // Given

    // Mock get tables
    List<String> publicationTables = List.of("claims.table1", "claims.table2");
    when(metadataRepository.getPublishedTables())
        .thenReturn(publicationTables);

    // Mock actual WAL LSN to be a recent one to indicate that the replication has caught up with previous changes.
    SubscriptionWalStatus healthyWalStatus =
        new SubscriptionWalStatus(
            RECENT_WAL_LSN,
            RECENT_WAL_LSN,
            Timestamp.from(Instant.now().minusSeconds(30))
        );
    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(healthyWalStatus);

    // Stub for replication summary query
    Map<String, ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationSummary("claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN),
        "claims.table2", new ReplicationSummary("claims.table2", TABLE2_RECORD_COUNT, TABLE2_UPDATE_COUNT, OLD_WAL_LSN)
    );

    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(summaries);

// Stub for count queries
    when(jdbcTemplate.query(contains("WHERE created_on"),
        any(ResultSetExtractor.class), any(Object[].class)))
        .thenAnswer(invocation -> {
          String sql = invocation.getArgument(0);
          if (sql.contains("table1")) return TABLE1_RECORD_COUNT;
          if (sql.contains("table2")) return TABLE2_RECORD_COUNT;
          return 0L;
        });

    when(jdbcTemplate.query(contains("WHERE updated_on"),
        any(ResultSetExtractor.class), any(Object[].class)))
        .thenAnswer(invocation -> {
          String sql = invocation.getArgument(0);
          if (sql.contains("table1")) return TABLE1_UPDATE_COUNT;
          if (sql.contains("table2")) return TABLE2_UPDATE_COUNT;
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
    mockReplicationHealth(List.of("claims.table1", "claims.table2"), MID_WAL_LSN, MID_WAL_LSN, 30);

    Map<String, ReplicationSummary> partialSummary = Map.of(
        "claims.table1", new ReplicationSummary("claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN));

    // Stub for replication summary query
    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(partialSummary);

    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any())).thenReturn(TABLE1_RECORD_COUNT);
    when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Missing replication summary"));
  }

  @Test
  void testWalProgressAheadTriggersFailure() {
    mockReplicationHealth(List.of("claims.table1"), LATEST_WAL_LSN, MID_WAL_LSN, 600);

// Stub for count queries
    when(jdbcTemplate.query(contains("WHERE created_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);

    when(jdbcTemplate.query(contains("WHERE updated_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Replication lag detected"));
  }

  @Test
  void testCountMismatchDetected() {
    mockReplicationHealth(List.of("claims.table1"), MID_WAL_LSN, MID_WAL_LSN, 30);
    // Stub for replication summary query
    Map<String, ReplicationSummary> summaries = Map.of(
        "claims.table1", new ReplicationSummary("claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN),
        "claims.table2", new ReplicationSummary("claims.table2", TABLE2_RECORD_COUNT, TABLE2_UPDATE_COUNT, OLD_WAL_LSN)
    );

    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(summaries);

    // mismatch: actual counts differ
    when(jdbcTemplate.queryForObject(startsWith("SELECT count(*) FROM claims.table1"), eq(Long.class), any()))
        .thenReturn(TABLE1_INCORRECT_RECORD_COUNT);
    when(jdbcTemplate.queryForObject(contains("claims.table1 WHERE updated_on"), eq(Long.class), any(), any()))
        .thenReturn(TABLE2_INCORRECT_RECORD_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Count mismatch"));
  }

  private void mockReplicationHealth(
      List<@NotNull String> publicationTables,
      String receivedLsn,
      String latestEndLsn,
      int secondsDelay
    ) {

      LocalDate summaryDate = LocalDate.now(clock).minusDays(1);
      Map<String, ReplicationSummary> summaries = Map.of(
          "claims.table1", new ReplicationSummary("claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, receivedLsn)
      );

      when(metadataRepository.getPublishedTables())
          .thenReturn(publicationTables);

      when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(summaryDate))).thenReturn(
          summaries);

      //Mock the WAL (Write Ahead Log)'s LSN (Log Sequence Number) to a high value to indicate that the replication has processed all previous changes.
      SubscriptionWalStatus healthyWalStatus =
          new SubscriptionWalStatus(
              receivedLsn,
              latestEndLsn,
              Timestamp.from(clock.instant().minusSeconds(secondsDelay))
          );
      when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
          .thenReturn(healthyWalStatus);
  }

  @Test
  void testWalStatusMissingTriggersFailure() {
    when(metadataRepository.getPublishedTables())
        .thenReturn(List.of("claims.table1"));

    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(null);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("No WAL progress information available"));
  }

  @Test
  void testWalLatestEndLsnNullTriggersFailure() {
    when(metadataRepository.getPublishedTables())
        .thenReturn(List.of("claims.table1"));

    SubscriptionWalStatus walStatus =
        new SubscriptionWalStatus(
            MID_WAL_LSN,
            null,
            Timestamp.from(clock.instant())
        );

    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(walStatus);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("No WAL progress information available"));
  }

  @Test
  void testWalApplyStalledTriggersFailure() {
    mockReplicationHealth(
        List.of("claims.table1"),
        MID_WAL_LSN,
        MID_WAL_LSN,
        600   // > 5 minutes
    );

    when(jdbcTemplate.query(
        contains("WHERE created_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);

    when(jdbcTemplate.query(
        contains("WHERE updated_on"),
        any(ResultSetExtractor.class),
        any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Replication apply has not progressed"));
  }

}