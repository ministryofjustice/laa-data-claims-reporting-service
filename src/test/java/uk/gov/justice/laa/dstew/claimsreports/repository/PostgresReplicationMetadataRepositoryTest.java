package uk.gov.justice.laa.dstew.claimsreports.repository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
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
        contains("FROM pg_publication_tables"),
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
}