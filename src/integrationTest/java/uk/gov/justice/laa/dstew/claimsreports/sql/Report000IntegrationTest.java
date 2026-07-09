package uk.gov.justice.laa.dstew.claimsreports.sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
public class Report000IntegrationTest extends IntegrationTestBase {

  public static final String CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333334";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333333";
  public static final String CLAIM_ID_FOR_VOIDED_CLAIM = "33333333-3333-3333-3333-333333333336";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS = "33333333-3333-3333-3333-333333333337";
    public static final String CLAIM_SUMMARY_FEE_ID_FOR_SINGLE_FEE_CLAIM = "66666666-6666-6666-6666-666666666667";

  @Test
  void claimDataIsReturnedIfNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    assertThat(row.get("Fee Code"))
        .isEqualTo("FEE002");
    assertThat(row.get("Fee Code Description"))
        .isEqualTo("Description 2");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2000.00");
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
        .isEqualTo("Description 2");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
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
        .isEqualTo("Description 2");
    BigDecimal claimValue =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
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
        .isEqualByComparingTo("2100.00");
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
        .isEqualByComparingTo("2000.00");
    var row = rows.getFirst();
    BigDecimal totalCurrentClaimValue1 =
        new BigDecimal(row.get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue1)
        .isEqualByComparingTo("2000.00");
    // When
    rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal finalClaimValue2 =
        new BigDecimal(rows.getFirst().get("Final Claim Value").toString());
    assertThat(finalClaimValue2)
        .isEqualByComparingTo("2100.00");
    BigDecimal totalCurrentClaimValue2 =
        new BigDecimal(rows.getFirst().get("Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue2)
        .isEqualByComparingTo("2100.00");
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
        .isEqualByComparingTo("2000.00");
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
        .isEqualByComparingTo("2100.00");
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
    void rep000DisplaysAllProvidedFeeCodesAndDescriptions() {
    List<Map.Entry<String, String>> expectedPairs = List.of(
                Map.entry("MHLDIS", "Mental Health - Interim Claim for Disbursements"),
                Map.entry("FPB010", "Public Family LH Fixed Fee"),
                Map.entry("FPB020", "Public Family FH Fixed Fee (Section 31 Pre-proceedings Only)"),
                Map.entry("FPB030", "Public Family LH+FH (Public Family Help Lower can be claimed for Section 31 Pre-proceedings Only)"),
                Map.entry("FVP100", "Private Family LH Fixed Fee - Divorce Petitioner Only"),
                Map.entry("FVP012", "Private Family LH Fixed Fee - Divorce Respondent Only"),
                Map.entry("FVP011", "Private Family LH Fixed Fee - Domestic Abuse Proceedings"),
                Map.entry("FVP013", "Private Family LH Fixed Fee - Child Abduction (International)"),
                Map.entry("MISCASBI", "Miscellaneous (ASBI) Legal Help Fixed Fee"),
                Map.entry("MISCEMP", "Miscellaneous (Employment) Legal Help Fixed Fee"),
                Map.entry("PUB", "Public Law Legal Help Fixed Fee"),
                Map.entry("WFB1", "Welfare Benefits Controlled Work fee"),
                Map.entry("INVC", "Police station: attendance"),
                Map.entry("INVA", "Advice and Assistance (not at the police station)"),
                Map.entry("INVE", "Warrant of further detention (including armed forces, Terrorism Act 2000, advice & assistance and other police station advice where given)"),
                Map.entry("INVH", "Police Station: Post-charge attendance"),
                Map.entry("INVK", "Advocacy Assistance in the magistrates' court on applications to extend Pre-Charge Bail (Extension to Pre-Charge Bail)"),
                Map.entry("PROJ7", "Representation in the Magistrates Court - second claim for deferred sentence - category 1A - higher standard fee - designated area"),
                Map.entry("PROJ8", "Representation in the Magistrates Court - second claim for deferred sentence - category 1B - higher standard fee - designated area"),
                Map.entry("PROK1", "Representation in the Magistrates Court - category 1A - lower standard fee - designated area"),
                Map.entry("PROK2", "Representation in the Magistrates Court - category 1B - lower standard fee - designated area"),
                Map.entry("PROK3", "Representation in the Magistrates Court - category 2 - lower standard fee - designated area"),
                Map.entry("PROL1", "Representation in the Magistrates Court - category 1A - higher standard fee - designated area"),
                Map.entry("PRIE1", "Advocacy Assistance at Parole Board Reconsideration Hearings - lower standard fee"),
                Map.entry("PRIE2", "Advocacy Assistance at Parole Board Reconsideration Hearings - higher standard fee"),
                Map.entry("APPA", "Advice and assistance in relation to an appeal (except CCRC)"),
                Map.entry("APPB", "Advice and assistance in relation to CCRC application"),
                Map.entry("ASMS", "Legal Help and Associated Civil Work - Miscellaneous"),
                Map.entry("ASPL", "Legal Help and Associated Civil Work - Public Law")
    );

    for (int i = 0; i < expectedPairs.size(); i++) {
      Map.Entry<String, String> pair = expectedPairs.get(i);
            String feeDetailId = UUID.randomUUID().toString();

      jdbcTemplate.update(
          """
              UPDATE claims.claim
              SET fee_code = ?
              WHERE id = ?::uuid
              """,
          pair.getKey(),
          CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);

      jdbcTemplate.update(
          """
              INSERT INTO claims.calculated_fee_detail
              (id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
               fee_code_description, category_of_law, total_amount)
              VALUES (?::uuid, ?::uuid, ?::uuid, ?, 'TypeMHL', 'integration_test_user', now(), 'integration_test_user',
                      now() + (? * interval '1 minute'), ?, 'AAP', 2000)
              """,
          feeDetailId,
          CLAIM_SUMMARY_FEE_ID_FOR_SINGLE_FEE_CLAIM,
          CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS,
          pair.getKey(),
          i + 1,
          pair.getValue());

      List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
      assertThat(rows).hasSize(1);
      Map<String, Object> row = rows.getFirst();

      assertThat(row.get("Fee Code"))
          .isEqualTo(pair.getKey());
      assertThat(row.get("Fee Code Description"))
          .isEqualTo(pair.getValue());
    }
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
