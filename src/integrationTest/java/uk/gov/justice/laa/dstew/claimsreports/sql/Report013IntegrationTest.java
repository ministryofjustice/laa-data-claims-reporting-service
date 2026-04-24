package uk.gov.justice.laa.dstew.claimsreports.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
class Report013IntegrationTest extends IntegrationTestBase {

  @Test
  void areaOfLawValuesAreNormalised() {
    // When
    insertAreaOfLawTestData();

    List<Map<String, Object>> areaOfLawCounts = jdbcTemplate.queryForList("""
        SELECT "Area of Law", count(*)
        FROM claims.report_013
        GROUP BY "Area of Law"
        """
        );

    // Then
    assertThat(areaOfLawCounts).isNotNull();
    assertThat(areaOfLawCounts).isNotEmpty();
    assertThat(areaOfLawCounts).hasSize(2);

    Map<String, Long> countsByAreaOfLaw =
        areaOfLawCounts.stream()
            .collect(Collectors.toMap(
                row -> (String) row.get("Area of Law"),
                row -> ((Number) row.get("count")).longValue()
            ));

    assertThat(countsByAreaOfLaw)
        .containsKeys("LEGAL_HELP", "CRIME_LOWER");

    assertThat(countsByAreaOfLaw.get("LEGAL_HELP")).isEqualTo(1L);
    assertThat(countsByAreaOfLaw.get("CRIME_LOWER")).isEqualTo(1L);

    long totalRows =
        countsByAreaOfLaw.values().stream().mapToLong(Long::longValue).sum();

    assertThat(totalRows).isEqualTo(2L);

  }

  @Test
  void testCalculations() {

    // When
    jdbcTemplate.execute("SELECT claims.refresh_report013()");

    // Then
    List<Map<String, Object>> report013Rows = jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.report_013
        ORDER BY "Area of Law"
        """
    );

    assertThat(report013Rows).isNotEmpty();

    Map<String, Object> firstRow = report013Rows.getFirst();

    // Column count
    assertThat(firstRow)
        .hasSize(4);

    assertThat(firstRow.keySet())
        .containsExactly(
            "Provider Office Account Number",
            "Area of Law",
            "APR-2025",
            "MAY-2025"
        );

    Map<String, Object> civilRow =
        report013Rows.stream()
            .filter(r -> "LEGAL_HELP".equals(r.get("Area of Law")))
            .findFirst()
            .orElseThrow();

    assertThat(civilRow.get("FEB-2025"))
        .isNull();

    assertThat( civilRow.get("APR-2025"))
        .isEqualTo("");

    assertThat(civilRow.get("MAY-2025"))
        .isEqualTo("0.00");

    Map<String, Object> crimeRow =
        report013Rows.stream()
            .filter(r -> "CRIME_LOWER".equals(r.get("Area of Law")))
            .findFirst()
            .orElseThrow();

    assertThat(crimeRow.get("FEB-2025"))
        .isNull();

    assertThat(crimeRow.get("APR-2025"))
        .isEqualTo("11722.33");

    assertThat(crimeRow.get("MAY-2025"))
        .isEqualTo("0.00");

  }

  @Test
  void testReplacedSubmission() {
    // When
    insertDataToReplaceSubmission();

    List<Map<String, Object>> report013Rows = jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.report_013
        ORDER BY "Area of Law"
        """
    );

    // Then
    assertThat(report013Rows).isNotEmpty();

    Map<String, Object> firstRow = report013Rows.getFirst();

// Column count
    assertThat(firstRow)
        .hasSize(4);

    assertThat(firstRow.keySet())
        .containsExactly(
            "Provider Office Account Number",
            "Area of Law",
            "APR-2025",
            "MAY-2025"
        );

    Map<String, Object> crimeRow =
        report013Rows.stream()
            .filter(r -> "CRIME_LOWER".equals(r.get("Area of Law")))
            .findFirst()
            .orElseThrow();

    assertThat(crimeRow.get("FEB-2025"))
        .isNull();

    assertThat(crimeRow.get("APR-2025"))
        .isEqualTo("1501.00");

    assertThat(crimeRow.get("MAY-2025"))
        .isEqualTo("0.00");

  }

  @Test
  void testBiggerValues() {
    // When
    insertBigNumber();

    List<Map<String, Object>> report013Rows = jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.report_013
        WHERE "Provider Office Account Number" = 'BIGONE'
        ORDER BY "Area of Law"
        """
    );

    // Then
    assertThat(report013Rows).isNotEmpty();

    Map<String, Object> crimeRow =
        report013Rows.stream()
            .filter(r -> "CRIME_LOWER".equals(r.get("Area of Law")))
            .findFirst()
            .orElseThrow();

    assertThat(crimeRow.get("APR-2025"))
        .isEqualTo("10000000000000000.00");

  }


  private void insertAreaOfLawTestData() {
    jdbcTemplate.update("""
    INSERT INTO claims.submission (
        id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
        previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
    ) VALUES (
        'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1',
        '11111111-1111-1111-1111-111111111111',
        'OA001',
        'MAR-2025',
        'CIVIL',
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

    jdbcTemplate.execute("SELECT claims.refresh_report013()");
  }

  private void insertDataToReplaceSubmission() {
    jdbcTemplate.update("""
    INSERT INTO claims.submission (
        id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
        previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
    ) VALUES (
        'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB2',
        '11111111-1111-1111-1111-111111111111',
        'OA001',
        'APR-2025',
        'Crime Lower',
        'VALIDATION_SUCCEEDED',
        'CSN001',
        '22222222-2222-2222-2222-222222222222',
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
          'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5',
          'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB2',
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
        'CC555555-5555-5555-5555-555555555556',
        'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5',
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
      INSERT INTO claims.claim_summary_fee (
          id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
          net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
          is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on
      ) VALUES (
          '56666666-6666-6666-6666-666666666669',
          'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5',
          60, 30, 15, 1000, 200,
          500, 100, 50, 20,
          TRUE, FALSE, 'integration_test_user',
          TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
           )
      """);

    jdbcTemplate.update("""
      INSERT INTO claims.calculated_fee_detail (
          id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
          fee_code_description, category_of_law, total_amount
          ) VALUES ('77777777-7777-7777-7777-777777777779', '66666666-6666-6666-6666-666666666669', 'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5',
              'FEE001', 'TypeA', 'integration_test_user', '2025-10-20 09:00:00+00', 'test_user', '2025-04-20 09:30:00+00', 'Description 1', 'INVEST', 1501)
      """);

    jdbcTemplate.execute("SELECT claims.refresh_report013()");
  }

  private void insertBigNumber() {
    jdbcTemplate.update("""
    INSERT INTO claims.submission (
        id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
        previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
    ) VALUES (
        'BBBBBBBB-B181-BBBB-BBBB-BBBBBBBBBBBB',
        '11111111-1111-1111-1111-111111111111',
        'BIGONE',
        'APR-2025',
        'Crime Lower',
        'VALIDATION_SUCCEEDED',
        'CSN001',
        '22222222-2222-2222-2222-222222222222',
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
          'CCCCCCCC-B181-CCCC-CCCC-CCCCCCCCCCCC',
          'BBBBBBBB-B181-BBBB-BBBB-BBBBBBBBBBBB',
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
        'CC555555-B181-5555-5555-555555555555',
        'CCCCCCCC-B181-CCCC-CCCC-CCCCCCCCCCCC',
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
      INSERT INTO claims.claim_summary_fee (
          id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
          net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
          is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on
      ) VALUES (
          '66666666-B181-6666-6666-666666666666',
          'CCCCCCCC-B181-CCCC-CCCC-CCCCCCCCCCCC',
          60, 30, 15, 1000, 200,
          500, 100, 50, 20,
          TRUE, FALSE, 'integration_test_user',
          TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
           )
      """);

    jdbcTemplate.update("""
      INSERT INTO claims.calculated_fee_detail (
          id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
          fee_code_description, category_of_law, total_amount
          ) VALUES ('77777777-B181-7777-7777-777777777777', '66666666-B181-6666-6666-666666666666', 'CCCCCCCC-B181-CCCC-CCCC-CCCCCCCCCCCC',
              'FEE001', 'TypeA', 'integration_test_user', '2025-10-20 09:00:00+00', 'test_user', '2025-04-20 09:30:00+00', 'Description 1', 'INVEST', 10000000000000000)
      """);

    jdbcTemplate.execute("SELECT claims.refresh_report013()");
  }

}
