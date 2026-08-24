package uk.gov.justice.laa.dstew.claimsreports.sql;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class Report014IntegrationTest extends IntegrationTestBase {

  @Test
  void testUsesCalculatedFeeForBeforeWhenFirstAssessment() {

    insertDataForFirstAssessmentTest();

    List<Map<String, Object>> firstAssessmentRow = jdbcTemplate.queryForList("""
        SELECT "Value before Amendment", "Difference"
        FROM claims.mvw_report_014
        WHERE "Assessment ID" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab'
        """
        );

    assertThat(firstAssessmentRow).isNotNull();
    assertThat(firstAssessmentRow).isNotEmpty();
    assertThat(firstAssessmentRow).hasSize(1);

    // Should have grabbed value from Calculated Fee Detail for before value
    var beforeValue = firstAssessmentRow.getFirst().get("Value before Amendment");
    assertThat(beforeValue).isEqualTo("1501.00");

    // Should have used that for the difference
    var difference = firstAssessmentRow.getFirst().get("Difference");
    assertThat(difference).isEqualTo("579.00");

  }

  @Test
  void testUsesFirstAssessmentForBeforeFeeWhenSecondAssessment() {

    insertDataForFirstAssessmentTest();
    insertDataForSecondAssessmentTest();

    List<Map<String, Object>> secondAssessmentRow = jdbcTemplate.queryForList("""
        SELECT "Value before Amendment", "Difference"
        FROM claims.mvw_report_014
        WHERE "Assessment ID" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaac'
        """
    );

    assertThat(secondAssessmentRow).isNotNull();
    assertThat(secondAssessmentRow).isNotEmpty();
    assertThat(secondAssessmentRow).hasSize(1);

    // Should have grabbed value from the first Assessment Row for before value
    var beforeValue = secondAssessmentRow.getFirst().get("Value before Amendment");
    assertThat(beforeValue).isEqualTo("2080.00");

    // Should have used that for the difference
    var difference = secondAssessmentRow.getFirst().get("Difference");
    assertThat(difference).isEqualTo("-10.00");

  }

  @Test
  void testSkipsAssessmentIfSubmissionStatusIsNotValidationSucceeded() {

    insertFullSubmissionWithClaimsAndAssessments("VALIDATION_FAILED", "VALID");

    List<Map<String, Object>> returnedRows = jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.mvw_report_014
        WHERE 'Submission ID' = 'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1'
        """
    );

    assertThat(returnedRows).isNotNull();
    assertThat(returnedRows).isEmpty();

  }

  @Test
  void testSkipsAssessmentIfClaimStatusIsInValid() {

    insertFullSubmissionWithClaimsAndAssessments("VALIDATION_SUCCEEDED", "INVALID");

    List<Map<String, Object>> returnedRows = jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.mvw_report_014
        WHERE "Claim ID" = 'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5'
        """
    );

    assertThat(returnedRows).isNotNull();
    assertThat(returnedRows).isEmpty();

  }

  @Test
  void testVoidAssessmentsAreIncluded() {

    insertFullSubmissionWithClaimsAndAssessments("VALIDATION_SUCCEEDED", "VOID");

    List<Map<String, Object>> returnedRows = jdbcTemplate.queryForList("""
        SELECT "Assessment Type", "Assessment Reason"
        FROM claims.mvw_report_014
        WHERE "Claim ID" = 'cccccccc-cccc-cccc-cccc-ccccccccccc5'
        """
    );

    assertThat(returnedRows).isNotNull();
    assertThat(returnedRows.getFirst().get("Assessment Type")).isEqualTo("Void");
    assertThat(returnedRows.getFirst().get("Assessment Reason")).isEqualTo("Voided");

  }

  @Test
  void testNoTypeAndReasonIsMappedToEscapeFeeDuringCrossOverPeriod() {

    // This case will no longer be valid when crossover period is over and existing records are populated properly.
    insertDataForFirstAssessmentTest();

    List<Map<String, Object>> returnedRows = jdbcTemplate.queryForList("""
        SELECT "Assessment Type", "Assessment Reason"
        FROM claims.mvw_report_014
        WHERE "Assessment ID" = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab'
        """
    );

    assertThat(returnedRows).isNotNull();
    assertThat(returnedRows.getFirst().get("Assessment Type")).isEqualTo("Escape Case Assessment");
    assertThat(returnedRows.getFirst().get("Assessment Reason")).isEqualTo("Escape Fee Case Assessment");

  }

  private void insertDataForFirstAssessmentTest() {
    insertFullSubmissionWithClaimsAndAssessments("VALIDATION_SUCCEEDED", "VALID");
  }

  private void insertDataForSecondAssessmentTest() {

    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaac', 'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5', '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00,
                    1400.00, 210.00, 2070.00, 'integration_test_user', now() )
            """
    );

    jdbcTemplate.update("""
      REFRESH MATERIALIZED VIEW claims.mvw_report_014
      """);
  }

  private void insertFullSubmissionWithClaimsAndAssessments(String submissionStatus, String claimStatus) {
    jdbcTemplate.update("""
    INSERT INTO claims.submission (
        id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
        previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
    ) VALUES (
        'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1',
        '11111111-1111-1111-1111-111111111111',
        'OA001',
        'MAR-2025',
        'LEGAL HELP',
        ?,
        'CSN001',
        NULL,
        FALSE,
        1,
        NULL,
        'integration_test_user',
        '2025-11-21 05:00:00',
        'test provider user')
      """, submissionStatus);

    jdbcTemplate.update("""
      INSERT INTO claims.claim (
          id, submission_id, status, line_number, matter_type_code, created_by_user_id, created_on
      ) VALUES (
          'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5',
          'BBBBBBBB-BBBB-BBBB-BBBB-BBBBBBBBBBB1',
            ?,
          1,
          'MT001',
          'integration_test_user',
          TIMESTAMP '2025-11-21 05:00:00' - interval '1 day')
          """, claimStatus);

    jdbcTemplate.update("""
    INSERT INTO claims.claim_case (
        id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code, created_by_user_id, created_on
    ) VALUES (
        'CC555555-5555-5555-5555-555555555555',
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

    if (Objects.equals(claimStatus, "VOID")){
      jdbcTemplate.update(
          """
              INSERT INTO claims.assessment
              (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
               allowed_total_vat, allowed_total_incl_vat, assessment_type, assessment_reason, created_by_user_id, created_on)
              VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5', '66666666-6666-6666-6666-666666666666', 'NILLED', 00.00,
                      00.00, 00.00, 00.0, 'VOID', 'Voided', 'integration_test_user', now() )
              """
      );
    } else {
      jdbcTemplate.update(
          """
              INSERT INTO claims.assessment
              (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
               allowed_total_vat, allowed_total_incl_vat, assessment_type, assessment_reason, created_by_user_id, created_on)
              VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'CCCCCCCC-CCCC-CCCC-CCCC-CCCCCCCCCCC5', '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00,
                      1400.00, 210.00, 2080.00, 'ESCAPE_CASE_ASSESSMENT', 'Escape Fee Case Assessment', 'integration_test_user', now() )
              """
      );
    }

    jdbcTemplate.update("""
      REFRESH MATERIALIZED VIEW claims.mvw_report_014
      """);

  }

}
