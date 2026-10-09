package uk.gov.justice.laa.dstew.claimsreports.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationHealthReport;
import uk.gov.justice.laa.dstew.claimsreports.dto.ReplicationSummary;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;
import uk.gov.justice.laa.dstew.claimsreports.repository.ReplicationMetadataRepository;

@SuppressFBWarnings("SECSQLISPRJDBC")
class ReplicationHealthCheckServiceTest {

  // Mock WAL (Write Ahead Log) LSNs (Log Sequence Numbers) to mimic various replication test
  // scenarios
  static final String OLD_WAL_LSN = "0/16B6C40";
  static final String MID_WAL_LSN = "0/16B6C50";
  static final String RECENT_WAL_LSN = "0/16B6C60";
  static final String LATEST_WAL_LSN = "0/16B6C70";
  static final String INVALID_WAL_LSN = "invalid_lsn";

  // Other constants used in test scenarios
  static final long TABLE1_RECORD_COUNT = 10L;
  static final long TABLE2_RECORD_COUNT = 5L;
  static final long TABLE1_UPDATE_COUNT = 2L;
  static final long TABLE2_UPDATE_COUNT = 1L;
  static final long TABLE1_INCORRECT_RECORD_COUNT = 9L;
  static final long TABLE2_INCORRECT_RECORD_COUNT = 3L;

  @Mock private Clock clock;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ReplicationMetadataRepository metadataRepository;

  @Mock private WalValidationService walValidationService;

  @InjectMocks private ReplicationHealthCheckService service;

  @BeforeEach
  void initMocks() {
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
    when(metadataRepository.getPublishedTables()).thenReturn(publicationTables);

    when(walValidationService.isValidWalLsn(RECENT_WAL_LSN)).thenReturn(true);
    when(walValidationService.parseWalLsn(RECENT_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817312L));

    // Mock actual WAL LSN to be a recent one to indicate that the replication has caught up with
    // previous changes.
    SubscriptionWalStatus healthyWalStatus =
        new SubscriptionWalStatus(RECENT_WAL_LSN, RECENT_WAL_LSN, Instant.now().minusSeconds(30));
    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(healthyWalStatus);

    // Stub for replication summary query
    Map<String, ReplicationSummary> summaries =
        Map.of(
            "claims.table1",
                new ReplicationSummary(
                    "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN),
            "claims.table2",
                new ReplicationSummary(
                    "claims.table2", TABLE2_RECORD_COUNT, TABLE2_UPDATE_COUNT, OLD_WAL_LSN));

    when(metadataRepository.getReplicationSummaries(any())).thenReturn(summaries);

    // Stub for count queries
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table2 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE2_RECORD_COUNT);

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table2 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE2_UPDATE_COUNT);

    // When
    ReplicationHealthReport report = service.checkReplicationHealth();

    // Then
    assertTrue(report.isHealthy(), "Expected healthy report");
    assertTrue(report.getFailedChecks().isEmpty());
  }

  @Test
  void testMissingTableDetected() {
    when(walValidationService.isValidWalLsn(RECENT_WAL_LSN)).thenReturn(true);
    when(walValidationService.parseWalLsn(RECENT_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817312L));

    mockReplicationHealth(List.of("claims.table1", "claims.table2"), MID_WAL_LSN, MID_WAL_LSN, 30);

    Map<String, ReplicationSummary> partialSummary =
        Map.of(
            "claims.table1",
            new ReplicationSummary(
                "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN));

    // Stub for replication summary query
    when(metadataRepository.getReplicationSummaries(any())).thenReturn(partialSummary);

    when(jdbcTemplate.queryForObject(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"), eq(Long.class), any()))
        .thenReturn(TABLE1_RECORD_COUNT);
    when(jdbcTemplate.queryForObject(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            eq(Long.class),
            any(),
            any()))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Missing replication summary"));
  }

  @Test
  void testWalProgressAheadTriggersFailure() {
    when(walValidationService.isValidWalLsn(LATEST_WAL_LSN)).thenReturn(true);
    when(walValidationService.isValidWalLsn(MID_WAL_LSN)).thenReturn(true);
    when(walValidationService.parseWalLsn(LATEST_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817328L));
    when(walValidationService.parseWalLsn(MID_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817296L));

    mockReplicationHealth(List.of("claims.table1"), LATEST_WAL_LSN, MID_WAL_LSN, 600);

    // Stub for count queries
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Replication lag detected"));
  }

  @Test
  void testWalLatestEndTimeNullTriggersFailure() {
    // Given
    when(metadataRepository.getPublishedTables()).thenReturn(List.of("claims.table1"));

    SubscriptionWalStatus walStatus = new SubscriptionWalStatus(MID_WAL_LSN, MID_WAL_LSN, null);

    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(walStatus);

    // When
    ReplicationHealthReport report = service.checkReplicationHealth();

    // Then
    assertFalse(report.isHealthy());

    assertTrue(report.summary().contains("WAL latest end time is null"));
  }

  @Test
  void testCountMismatchDetected() {
    when(walValidationService.isValidWalLsn(RECENT_WAL_LSN)).thenReturn(true);
    when(walValidationService.parseWalLsn(RECENT_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817312L));

    mockReplicationHealth(List.of("claims.table1"), MID_WAL_LSN, MID_WAL_LSN, 30);
    // Stub for replication summary query
    Map<String, ReplicationSummary> summaries =
        Map.of(
            "claims.table1",
                new ReplicationSummary(
                    "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN),
            "claims.table2",
                new ReplicationSummary(
                    "claims.table2", TABLE2_RECORD_COUNT, TABLE2_UPDATE_COUNT, OLD_WAL_LSN));

    when(metadataRepository.getReplicationSummaries(any())).thenReturn(summaries);

    // mismatch: actual counts differ
    when(jdbcTemplate.queryForObject(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"), eq(Long.class), any()))
        .thenReturn(TABLE1_INCORRECT_RECORD_COUNT);
    when(jdbcTemplate.queryForObject(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            eq(Long.class),
            any(),
            any()))
        .thenReturn(TABLE2_INCORRECT_RECORD_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Count mismatch"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("walComparisonCases")
  void testWalOrderingDetectsReplicationLag(
      String description,
      String receivedLsn,
      Long receivedAsDecimal,
      String latestEndLsn,
      Long latestEndAsDecimal) {

    when(walValidationService.isValidWalLsn(any())).thenReturn(true);
    when(walValidationService.parseWalLsn(receivedLsn))
        .thenReturn(java.util.Optional.of(receivedAsDecimal));
    when(walValidationService.parseWalLsn(latestEndLsn))
        .thenReturn(java.util.Optional.of(latestEndAsDecimal));

    mockReplicationHealth(
        List.of("claims.table1", "claims.table2"), receivedLsn, latestEndLsn, 3000);

    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(
            Map.of(
                "claims.table1",
                new ReplicationSummary(
                    "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN),
                "claims.table2",
                new ReplicationSummary(
                    "claims.table2", TABLE2_RECORD_COUNT, TABLE2_UPDATE_COUNT, OLD_WAL_LSN)));

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table2 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE2_RECORD_COUNT);
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table2 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE2_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy(), description);
    assertTrue(report.summary().contains("Replication lag detected"), description);
  }

  private static Stream<Arguments> walComparisonCases() {
    return Stream.of(
        Arguments.of(
            "ordering across the 32-bit low-half boundary",
            "1/0",
            4294967296L,
            "0/FFFFFFFF",
            4294967295L),
        Arguments.of(
            "unsigned ordering when the high half crosses the signed int boundary",
            "80000000/00000000",
            -9223372036854775808L,
            "7FFFFFFF/FFFFFFFF",
            9223372036854775807L));
  }

  @Test
  void errorWhenInvalidLSNForReceivedLSN() {
    when(walValidationService.isValidWalLsn(INVALID_WAL_LSN)).thenReturn(false);
    mockReplicationHealth(List.of("claims.table1"), INVALID_WAL_LSN, MID_WAL_LSN, 30);

    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(
            Map.of(
                "claims.table1",
                new ReplicationSummary(
                    "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN)));

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Malformed WAL LSN — received WAL " + INVALID_WAL_LSN));
  }

  @Test
  void errorWhenInvalidLSNForLatestLSN() {
    when(walValidationService.isValidWalLsn(MID_WAL_LSN)).thenReturn(true);
    when(walValidationService.isValidWalLsn(INVALID_WAL_LSN)).thenReturn(false);
    mockReplicationHealth(List.of("claims.table1"), MID_WAL_LSN, INVALID_WAL_LSN, 30);

    when(metadataRepository.getReplicationSummaries(any()))
        .thenReturn(
            Map.of(
                "claims.table1",
                new ReplicationSummary(
                    "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, MID_WAL_LSN)));

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);
    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(
        report.summary().contains("Malformed WAL LSN — last applied WAL " + INVALID_WAL_LSN));
  }

  @Test
  void testPublicationTableNull() {
    when(metadataRepository.getPublishedTables()).thenReturn(null);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("No tables found for publication"));
  }

  @Test
  void testPublicationTableEmpty() {
    when(metadataRepository.getPublishedTables()).thenReturn(List.of());

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("No tables found for publication"));
  }

  private void mockReplicationHealth(
      List<@NotNull String> publicationTables,
      String receivedLsn,
      String latestEndLsn,
      int secondsDelay) {

    LocalDate summaryDate = LocalDate.now(clock).minusDays(1);
    Map<String, ReplicationSummary> summaries =
        Map.of(
            "claims.table1",
            new ReplicationSummary(
                "claims.table1", TABLE1_RECORD_COUNT, TABLE1_UPDATE_COUNT, receivedLsn));

    when(metadataRepository.getPublishedTables()).thenReturn(publicationTables);

    when(jdbcTemplate.query(
            eq(
                "SELECT table_name, record_count, updated_count, wal_lsn\n        FROM claims.replication_summary\n        WHERE summary_date = ?\n        "),
            any(ResultSetExtractor.class),
            eq(summaryDate)))
        .thenReturn(summaries);

    // Mock the WAL (Write Ahead Log)'s LSN (Log Sequence Number) to a high value to indicate that
    // the replication has processed all previous changes.
    SubscriptionWalStatus healthyWalStatus =
        new SubscriptionWalStatus(
            receivedLsn, latestEndLsn, clock.instant().minusSeconds(secondsDelay));
    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(healthyWalStatus);
  }

  @Test
  void testWalStatusMissingTriggersFailure() {
    when(metadataRepository.getPublishedTables()).thenReturn(List.of("claims.table1"));

    when(metadataRepository.getSubscriptionWalStatus("claims_reporting_service_sub"))
        .thenReturn(null);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("No WAL progress information available"));
  }

  @Test
  void testWalApplyStalledTriggersFailure() {
    when(walValidationService.isValidWalLsn(MID_WAL_LSN)).thenReturn(true);
    when(walValidationService.parseWalLsn(MID_WAL_LSN))
        .thenReturn(java.util.Optional.of(23817296L));
    mockReplicationHealth(
        List.of("claims.table1"), MID_WAL_LSN, MID_WAL_LSN, 600 // > 5 minutes
        );

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE created_on < ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_RECORD_COUNT);

    when(jdbcTemplate.query(
            eq("SELECT count(*) FROM claims.table1 WHERE updated_on BETWEEN ? AND ?"),
            any(ResultSetExtractor.class),
            any(Object[].class)))
        .thenReturn(TABLE1_UPDATE_COUNT);

    ReplicationHealthReport report = service.checkReplicationHealth();

    assertFalse(report.isHealthy());
    assertTrue(report.summary().contains("Replication apply has not progressed"));
  }
}
