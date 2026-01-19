package uk.gov.justice.laa.dstew.claimsreports.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.gov.justice.laa.dstew.claimsreports.dto.SubscriptionWalStatus;

@ExtendWith(MockitoExtension.class)
class PostgresReplicationMetadataRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private PostgresReplicationMetadataRepository repository;

  @Test
  void getPublishedTables_returnsPublishedTablesExcludingReplicationSummary() {
    // Given
    List<String> expectedTables = List.of(
        "claims.claim",
        "claims.assessment"
    );

    when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
        .thenReturn(expectedTables);

    // When
    List<String> tables = repository.getPublishedTables();

    // Then
    assertThat(tables)
        .isNotNull()
        .containsExactlyElementsOf(expectedTables);

    verify(jdbcTemplate).queryForList(
        contains("FROM pg_subscription_rel"),
        eq(String.class)
    );
  }

  @Test
  void getSubscriptionWalStatus_returnsMappedWalStatus() {
    // Given
    String subscriptionName = "claims_reporting_service_sub";

    SubscriptionWalStatus expected =
        new SubscriptionWalStatus(
            "2CE/0000FFF0",
            "2CE/0000FFF0",
            Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"))
        );

    when(jdbcTemplate.queryForObject(
        anyString(),
        any(RowMapper.class),
        eq(subscriptionName)))
        .thenReturn(expected);

    // When
    SubscriptionWalStatus actual =
        repository.getSubscriptionWalStatus(subscriptionName);

    // Then
    assertThat(actual)
        .isNotNull()
        .usingRecursiveComparison()
        .isEqualTo(expected);

    verify(jdbcTemplate).queryForObject(
        contains("FROM pg_stat_subscription"),
        any(RowMapper.class),
        eq(subscriptionName)
    );
  }

  @Test
  void getSubscriptionWalStatus_returnsNullWhenSubscriptionNotFound() {
    // Given
    String subscriptionName = "missing_subscription";

    when(jdbcTemplate.queryForObject(
        anyString(),
        any(RowMapper.class),
        eq(subscriptionName)))
        .thenThrow(new EmptyResultDataAccessException(1));

    // When
    SubscriptionWalStatus result =
        repository.getSubscriptionWalStatus(subscriptionName);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void getSubscriptionWalStatus_returnsStatus_whenRowExists() {
    // Given
    String subscriptionName = "claims_reporting_service_sub";

    when(jdbcTemplate.queryForObject(
        anyString(),
        any(RowMapper.class),
        eq(subscriptionName)))
        .thenAnswer(invocation -> {
          RowMapper<SubscriptionWalStatus> mapper = invocation.getArgument(1);
          ResultSet rs = mock(ResultSet.class);

          when(rs.getString("received_lsn")).thenReturn("0/16B6C50");
          when(rs.getString("latest_end_lsn")).thenReturn("0/16B6C40");
          when(rs.getTimestamp("latest_end_time"))
              .thenReturn(Timestamp.valueOf(LocalDate.now().atStartOfDay()));

          return mapper.mapRow(rs, 1);
        });

    // When
    SubscriptionWalStatus result =
        repository.getSubscriptionWalStatus(subscriptionName);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.receivedLsn()).isEqualTo("0/16B6C50");
    assertThat(result.latestEndLsn()).isEqualTo("0/16B6C40");
  }
}