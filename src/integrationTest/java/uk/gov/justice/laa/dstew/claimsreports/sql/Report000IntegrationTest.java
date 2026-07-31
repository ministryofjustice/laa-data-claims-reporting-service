package uk.gov.justice.laa.dstew.claimsreports.sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
public class Report000IntegrationTest extends IntegrationTestBase {

  public static final String CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333334";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333333";
  public static final String CLAIM_ID_FOR_VOIDED_CLAIM = "33333333-3333-3333-3333-333333333336";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS = "33333333-3333-3333-3333-333333333337";
  public static final String CLAIM_SUMMARY_FEE_ID_FOR_SINGLE_FEE_CLAIM = "66666666-6666-6666-6666-666666666667";
  public static final String AMENDED_CLAIM_ID = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";
  public static final String AMENDED_CLAIM_SUMMARY_FEE_ID_OLD = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee1";
  public static final String AMENDED_CLAIM_SUMMARY_FEE_ID_LATEST = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee2";

  @Test
  void claimDataIsReturnedIfNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Fee Code here comes from claim not calculated_fee_detail table, whereas description comes from calculated_fee_detail table.
    // Hence, divergence between the two values in this test data.
    assertThat(row.get("Fee Code"))
        .isEqualTo("FEE002");
    assertThat(row.get("Fee Code Description"))
        .isEqualTo("Description 3");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("3500.00");
    assertThat(row.get("Assessed Total Inc VAT"))
        .isEqualTo("");
  }

  @Test
  void latestFeeAndNullAssessmentReturnedIfMultipleFeesAndNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    assertThat(row.get("Fee Code"))
        .isEqualTo("FEE002");
    assertThat(row.get("Fee Code Description"))
        .isEqualTo("Description 3");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2500.00");
    assertThat(row.get("Assessed Total Inc VAT"))
        .isEqualTo("");
  }

  @Test
  void latestFeeAndAssessmentIsReturnedIfMultipleFeesAndOneAssessmentPresent() {
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS);
    assertThat(row.get("Fee Code"))
        .isEqualTo("FEE002");
    assertThat(row.get("Fee Code Description"))
        .isEqualTo("Description 3");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2500.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("Assessed Total Inc VAT").toString());
    assertThat(claimAssessedValue)
        .isEqualByComparingTo("94.2");
  }

  @Test
  void latestFeeAndLatestAssessmentIsReturnedIfMultipleFeesAndMultipleAssessmentsPresent() {
    // When
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAA1', ?::uuid, '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00, 1400.00,
                    210.00, 1440.00, 'integration_test_user', now() - interval '1 day' )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAA2', ?::uuid, '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00, 1300.00,
                    210.00, 1440.00, 'integration_test_user', now() )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);

    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2500.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("Assessed Total Inc VAT").toString());
    assertThat(claimAssessedValue)
        .isEqualByComparingTo("1300.00");
  }

  @Test
  void claimTotalValueIsReturnedAsFinalValueIfNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal finalClaimValue1 =
        new BigDecimal(rows.getFirst().get("Final Claim Value").toString());
    assertThat(finalClaimValue1)
        .isEqualByComparingTo("3500.00");
    var row = rows.getFirst();
    BigDecimal totalCurrentClaimValue1 =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue1)
        .isEqualByComparingTo("3500.00");
    // When
    rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal finalClaimValue2 =
        new BigDecimal(rows.getFirst().get("Final Claim Value").toString());
    assertThat(finalClaimValue2)
        .isEqualByComparingTo("2500.00");
    BigDecimal totalCurrentClaimValue2 =
        new BigDecimal(rows.getFirst().get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue2)
        .isEqualByComparingTo("2500.00");
  }

  @Test
  void allowedTotalValueIsReturnedAsFinalValueIfAssessmentDataPresent() {
    // When
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA', ?::uuid, '66666666-6666-6666-6666-666666666667', 'REDUCED_STILL_ESCAPED', 200.00,
                    1400.00, 210.00, 1990.00, 'integration_test_user', now() )
            """,
        CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAB', ?::uuid, '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00,
                    1400.00, 210.00, 2080.00, 'integration_test_user', now() )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);

    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal totalCurrentClaimValue1 =
        new BigDecimal(rows.getFirst().get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue1)
        .isEqualByComparingTo("3500.00");
    BigDecimal allowedTotalIncVat1 =
        new BigDecimal(rows.getFirst().get("Allowed Total Inc VAT").toString());
    assertThat(allowedTotalIncVat1)
        .isEqualByComparingTo("1990.00");
    BigDecimal finalClaimValue1 =
        new BigDecimal(rows.getFirst().get("Final Claim Value").toString());
    assertThat(finalClaimValue1)
        .isEqualByComparingTo("1990.00");
    // When
    rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal totalCurrentClaimValue2 =
        new BigDecimal(rows.getFirst().get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue2)
        .isEqualByComparingTo("2500.00");
    BigDecimal allowedTotalIncVat2 =
        new BigDecimal(rows.getFirst().get("Allowed Total Inc VAT").toString());
    assertThat(allowedTotalIncVat2)
        .isEqualByComparingTo("2080.00");
    BigDecimal finalClaimValue2 =
        new BigDecimal(rows.getFirst().get("Final Claim Value").toString());
    assertThat(finalClaimValue2)
        .isEqualByComparingTo("2080.00");
  }

  @Test
  void voidClaimReturnsVoidedAssessmentValues() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_FOR_VOIDED_CLAIM);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_FOR_VOIDED_CLAIM);
    assertThat(row.get("Fee Code"))
        .isEqualTo("FEE004");
    assertThat(row.get("Fee Code Description"))
        .isEqualTo("Description 4");
    BigDecimal initialValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(initialValue)
        .isEqualByComparingTo("4500.00");
    assertThat(row.get("Final Claim Value"))
        .isEqualTo("0");
    assertThat(row.get("Allowed Total VAT"))
        .isEqualTo("0");
    assertThat(row.get("Allowed Total Inc VAT"))
        .isEqualTo("0");
    assertThat(row.get("Assessed Total VAT"))
        .isEqualTo("0");
    assertThat(row.get("Assessed Total Inc VAT"))
        .isEqualTo("0");
  }

  @Test
  void amendedClaimUsesLatestFeeRecordsAndAssessmentForFinalClaimValue() {
    jdbcTemplate.update(
        """
            INSERT INTO claims.claim
            (id, submission_id, status, line_number, matter_type_code, fee_code, unique_file_number, is_amended,
             created_by_user_id, created_on, updated_on)
            VALUES (?::uuid, '22222222-2222-2222-2222-222222222222'::uuid, 'VALID', 99, 'MT001', 'FEE500',
                    'UFN-AMEND-001', TRUE, 'integration_test_user', now() - interval '2 days', now())
            """,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.client
            (id, claim_id, client_forename, client_surname, client_date_of_birth, unique_client_number, client_postcode,
             gender_code, ethnicity_code, is_legally_aided, client_type_code, home_office_client_number,
             cla_reference_number, cla_exemption_code, created_by_user_id, created_on, updated_on)
            VALUES ('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee3'::uuid, ?::uuid, 'Amend', 'Client', DATE '1990-01-01',
                    'UCN-AMEND-1', 'AB1 2CD', 'M', 'White', TRUE, 'Type1', 'HO999', 'CLA999', 'EX999',
                    'integration_test_user', now() - interval '2 days', now())
            """,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.claim_case
            (id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code,
             created_by_user_id, created_on)
            VALUES ('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee4'::uuid, ?::uuid, 'CASE-AMEND-1', 'UCASE-AMEND-1',
                    'STAGE1', 'REACHED1', 'SUCCESS', 'integration_test_user', now() - interval '2 days')
            """,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.claim_summary_fee
            (id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
             net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
             is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on)
            VALUES (?::uuid, ?::uuid, 20, 15, 10, 500, 100, 50, 10, 5, 2, TRUE, FALSE,
                    'integration_test_user', now() - interval '2 days', now() - interval '2 days')
            """,
        AMENDED_CLAIM_SUMMARY_FEE_ID_OLD,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.claim_summary_fee
            (id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
             net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
             is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on)
            VALUES (?::uuid, ?::uuid, 80, 45, 25, 900, 150, 75, 20, 15, 11, TRUE, TRUE,
                    'integration_test_user', now() - interval '1 day', now())
            """,
        AMENDED_CLAIM_SUMMARY_FEE_ID_LATEST,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.calculated_fee_detail
            (id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
             fee_code_description, category_of_law, total_amount)
            VALUES ('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee5'::uuid, ?::uuid, ?::uuid, 'FEE500', 'TypeA',
                    'integration_test_user', now() - interval '2 days', 'integration_test_user', now() - interval '2 days',
                    'Old amendment fee', 'AAP', 1111.11)
            """,
        AMENDED_CLAIM_SUMMARY_FEE_ID_OLD,
        AMENDED_CLAIM_ID);

    jdbcTemplate.update(
        """
            INSERT INTO claims.calculated_fee_detail
            (id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
             fee_code_description, category_of_law, total_amount)
            VALUES ('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee6'::uuid, ?::uuid, ?::uuid, 'FEE501', 'TypeB',
                    'integration_test_user', now() - interval '1 day', 'integration_test_user', now(),
                    'Latest amendment fee', 'IMMAS', 2222.22)
            """,
        AMENDED_CLAIM_SUMMARY_FEE_ID_LATEST,
        AMENDED_CLAIM_ID);

    List<Map<String, Object>> rows = getDataForClaimId(AMENDED_CLAIM_ID);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();

    assertThat(row.get("Amended Flag")).isEqualTo("Yes");
    assertThat(row.get("Unique File Number")).isEqualTo("UFN-AMEND-001");
    assertThat(row.get("Fee Code")).isEqualTo("FEE500");
    assertThat(row.get("Fee Code Description")).isEqualTo("Latest amendment fee");
    assertThat(row.get("Travel Time")).isEqualTo("45");
    assertThat(new BigDecimal(row.get("Initial Calculated Claim Value").toString()))
        .isEqualByComparingTo("2222.22");
    assertThat(new BigDecimal(row.get("Final Claim Value").toString()))
        .isEqualByComparingTo("2222.22");

    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeee7'::uuid, ?::uuid, ?::uuid, 'PAID_IN_FULL', 123.45, 2500.00,
                    123.45, 1999.99, 'integration_test_user', now() + interval '1 minute')
            """,
        AMENDED_CLAIM_ID,
        AMENDED_CLAIM_SUMMARY_FEE_ID_LATEST);

    rows = getDataForClaimId(AMENDED_CLAIM_ID);
    assertThat(rows).hasSize(1);
    row = rows.getFirst();

    assertThat(new BigDecimal(row.get("Initial Calculated Claim Value").toString()))
        .isEqualByComparingTo("2222.22");
    assertThat(new BigDecimal(row.get("Allowed Total Inc VAT").toString()))
        .isEqualByComparingTo("1999.99");
    assertThat(new BigDecimal(row.get("Final Claim Value").toString()))
        .isEqualByComparingTo("1999.99");
  }

  @ParameterizedTest(name = "{index} => feeCode={0}")
  @MethodSource("providedFeeCodeDescriptionPairs")
  void rep000DisplaysAllProvidedFeeCodesAndDescriptions(String feeCode, String feeCodeDescription) {
    String feeDetailId = UUID.randomUUID().toString();

    jdbcTemplate.update(
        """
            UPDATE claims.claim
            SET fee_code = ?
            WHERE id = ?::uuid
            """,
        feeCode,
        CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);

    jdbcTemplate.update(
        """
            INSERT INTO claims.calculated_fee_detail
            (id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
             fee_code_description, category_of_law, total_amount)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'TypeMHL', 'integration_test_user', now(), 'integration_test_user',
                    now() + interval '1 minute', ?, 'AAP', 2000)
            """,
        feeDetailId,
        CLAIM_SUMMARY_FEE_ID_FOR_SINGLE_FEE_CLAIM,
        CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS,
        feeCode,
        feeCodeDescription);

    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();

    assertThat(row.get("Fee Code"))
        .isEqualTo(feeCode);
    assertThat(row.get("Fee Code Description"))
        .isEqualTo(feeCodeDescription);
  }

  private static Stream<Arguments> providedFeeCodeDescriptionPairs() {
    return Stream.of(
        Arguments.of("MHLDIS", "Mental Health - Interim Claim for Disbursements"),
        Arguments.of("FPB010", "Public Family LH Fixed Fee"),
        Arguments.of("FPB020", "Public Family FH Fixed Fee (Section 31 Pre-proceedings Only)"),
        Arguments.of("FPB030", "Public Family LH+FH (Public Family Help Lower can be claimed for Section 31 Pre-proceedings Only)"),
        Arguments.of("FVP100", "Private Family LH Fixed Fee - Divorce Petitioner Only"),
        Arguments.of("FVP012", "Private Family LH Fixed Fee - Divorce Respondent Only"),
        Arguments.of("FVP011", "Private Family LH Fixed Fee - Domestic Abuse Proceedings"),
        Arguments.of("FVP013", "Private Family LH Fixed Fee - Child Abduction (International)"),
        Arguments.of("MISCASBI", "Miscellaneous (ASBI) Legal Help Fixed Fee"),
        Arguments.of("MISCEMP", "Miscellaneous (Employment) Legal Help Fixed Fee"),
        Arguments.of("PUB", "Public Law Legal Help Fixed Fee"),
        Arguments.of("WFB1", "Welfare Benefits Controlled Work fee"),
        Arguments.of("INVC", "Police station: attendance"),
        Arguments.of("INVA", "Advice and Assistance (not at the police station)"),
        Arguments.of("INVE", "Warrant of further detention (including armed forces, Terrorism Act 2000, advice & assistance and other police station advice where given)"),
        Arguments.of("INVH", "Police Station: Post-charge attendance"),
        Arguments.of("INVK", "Advocacy Assistance in the magistrates' court on applications to extend Pre-Charge Bail (Extension to Pre-Charge Bail)"),
        Arguments.of("PROJ7", "Representation in the Magistrates Court - second claim for deferred sentence - category 1A - higher standard fee - designated area"),
        Arguments.of("PROJ8", "Representation in the Magistrates Court - second claim for deferred sentence - category 1B - higher standard fee - designated area"),
        Arguments.of("PROK1", "Representation in the Magistrates Court - category 1A - lower standard fee - designated area"),
        Arguments.of("PROK2", "Representation in the Magistrates Court - category 1B - lower standard fee - designated area"),
        Arguments.of("PROK3", "Representation in the Magistrates Court - category 2 - lower standard fee - designated area"),
        Arguments.of("PROL1", "Representation in the Magistrates Court - category 1A - higher standard fee - designated area"),
        Arguments.of("PRIE1", "Advocacy Assistance at Parole Board Reconsideration Hearings - lower standard fee"),
        Arguments.of("PRIE2", "Advocacy Assistance at Parole Board Reconsideration Hearings - higher standard fee"),
        Arguments.of("APPA", "Advice and assistance in relation to an appeal (except CCRC)"),
        Arguments.of("APPB", "Advice and assistance in relation to CCRC application"),
        Arguments.of("ASMS", "Legal Help and Associated Civil Work - Miscellaneous"),
        Arguments.of("ASPL", "Legal Help and Associated Civil Work - Public Law")
    );
  }

  @Test
  void report000ContainsDsccNumber() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);

    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row).containsKey("DSCC Number");
    assertThat(row.get("DSCC Number"))
      .isNotNull()
      .isEqualTo("DSCC123456");
  }

 @Test
  void report000WithoutDsccNumberReturnsEmptyString() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);

    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row).containsKey("DSCC Number");
    assertThat(row.get("DSCC Number"))
      .isNotNull()
      .isEqualTo("");
  }

  private @NotNull List<Map<String, Object>> getDataForClaimId(String claimId) {

    jdbcTemplate.update("""
      REFRESH MATERIALIZED VIEW claims.mvw_report_000
      """);

    return
        jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.mvw_report_000
        WHERE "Claim ID" = ?
        """,
            claimId
        );
  }

}
