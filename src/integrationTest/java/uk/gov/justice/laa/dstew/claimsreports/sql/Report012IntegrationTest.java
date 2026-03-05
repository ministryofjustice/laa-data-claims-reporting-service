package uk.gov.justice.laa.dstew.claimsreports.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
class Report012IntegrationTest extends IntegrationTestBase {

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("""
        DELETE FROM claims.claim_case
        WHERE created_by_user_id = 'integration_test_user'
        """);
    jdbcTemplate.update("""
        DELETE FROM claims.claim
        WHERE created_by_user_id = 'integration_test_user'
        """);
    jdbcTemplate.update("""
        DELETE FROM claims.submission
        WHERE created_by_user_id = 'integration_test_user'
        """);
  }

  @Test
  void areaOfLawValuesAreNormalised() {
    // When
    jdbcTemplate.update("""
    INSERT INTO claims.submission (
        id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
        previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
    ) VALUES (
        'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1',
        '11111111-1111-1111-1111-111111111111',
        'OA001',
        'APR-2025',
        'LEGAL HELP',
        'VALIDATION_SUCCEEDED',
        'CSN001',
        NULL,
        FALSE,
        1,
        NULL,
        'integration_test_user',
        '2025-11-21 05:00:00',
        'test provider user')
    """);

    jdbcTemplate.update("""
      INSERT INTO claims.claim (
          id, submission_id, status, line_number, matter_type_code, created_by_user_id, created_on
      ) VALUES (
          'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC4',
          'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1',
          'VALID',
          1,
          'MT001',
          'integration_test_user',
          TIMESTAMP '2025-11-21 05:00:00' - interval '1 day')
      """);

    jdbcTemplate.update("""
    INSERT INTO claims.claim_case (
        id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code, created_by_user_id, created_on
    ) VALUES (
        'CC555555-5555-5555-5555-555555555555',
        'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC4',
        'CASE002',
        'UCASE001',
        'STAGE1',
        'REACHED1',
        'SUCCESS',
        'integration_test_user',
        TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         )
    """);

jdbcTemplate.update("""
      REFRESH MATERIALIZED VIEW claims.mvw_report_012
      """);

    List<Map<String, Object>> areaOfLawCounts = jdbcTemplate.queryForList("""
        SELECT "Area of law", count(*)
        FROM claims.mvw_report_012
        GROUP BY "Area of law"
        """
        );

    // Then
    assertThat(areaOfLawCounts).isNotNull().isNotEmpty().hasSize(2);

    Map<String, Long> countsByAreaOfLaw =
        areaOfLawCounts.stream()
            .collect(Collectors.toMap(
                row -> (String) row.get("Area of Law"),
                row -> ((Number) row.get("count")).longValue()
            ));

    assertThat(countsByAreaOfLaw)
        .containsKeys("LEGAL_HELP", "CRIME_LOWER");

    assertThat(countsByAreaOfLaw.get("LEGAL_HELP")).isEqualTo(2L);
    assertThat(countsByAreaOfLaw.get("CRIME_LOWER")).isEqualTo(2L);

    long totalRows =
        countsByAreaOfLaw.values().stream().mapToLong(Long::longValue).sum();

    assertThat(totalRows).isEqualTo(4L);  }


}
