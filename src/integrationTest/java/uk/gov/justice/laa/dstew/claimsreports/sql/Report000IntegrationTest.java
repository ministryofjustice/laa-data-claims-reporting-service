package uk.gov.justice.laa.dstew.claimsreports.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
public class Report000IntegrationTest extends IntegrationTestBase {

  public static final String CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333334";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS = "33333333-3333-3333-3333-333333333333";
  public static final String CLAIM_ID_FOR_VOIDED_CLAIM = "33333333-3333-3333-3333-333333333336";
  public static final String CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS = "33333333-3333-3333-3333-333333333337";

  @AfterEach
  void cleanup() {
    cleanUpDataFromTests();
  }

  @Test
  void claimDataIsReturnedIfNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Case Details - Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2000.00");
    assertThat(row.get("SaBC Total Costing information - Assessed Total Inc VAT"))
        .isEqualTo("");
  }

  @Test
  void latestFeeAndNullAssessmentReturnedIfMultipleFeesAndNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Case Details - Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    assertThat(row.get("SaBC Total Costing information - Assessed Total Inc VAT"))
        .isEqualTo("");
  }

  @Test
  void latestFeeAndAssessmentIsReturnedIfMultipleFeesAndOneAssessmentPresent() {
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Case Details - Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("SaBC Total Costing information - Assessed Total Inc VAT").toString());
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
    assertThat(row.get("Case Details - Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("SaBC Total Costing information - Assessed Total Inc VAT").toString());
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
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Final Claim Value").toString());
    assertThat(finalClaimValue1)
        .isEqualByComparingTo("2000.00");
    var row = rows.getFirst();
    BigDecimal totalCurrentClaimValue1 =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue1)
        .isEqualByComparingTo("2000.00");
    // When
    rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal finalClaimValue2 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Final Claim Value").toString());
    assertThat(finalClaimValue2)
        .isEqualByComparingTo("2100.00");
    BigDecimal totalCurrentClaimValue2 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
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
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue1)
        .isEqualByComparingTo("2000.00");
    BigDecimal allowedTotalIncVat1 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Allowed Total Inc VAT").toString());
    assertThat(allowedTotalIncVat1)
        .isEqualByComparingTo("1990.00");
    BigDecimal finalClaimValue1 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Final Claim Value").toString());
    assertThat(finalClaimValue1)
        .isEqualByComparingTo("1990.00");
    // When
    rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    BigDecimal totalCurrentClaimValue2 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(totalCurrentClaimValue2)
        .isEqualByComparingTo("2100.00");
    BigDecimal allowedTotalIncVat2 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Allowed Total Inc VAT").toString());
    assertThat(allowedTotalIncVat2)
        .isEqualByComparingTo("2080.00");
    BigDecimal finalClaimValue2 =
        new BigDecimal(rows.getFirst().get("SaBC Total Costing information - Final Claim Value").toString());
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
    assertThat(row.get("Case Details - Claim ID"))
        .isEqualTo(CLAIM_ID_FOR_VOIDED_CLAIM);
    BigDecimal initialValue =
        new BigDecimal(row.get("SaBC Total Costing information - Initial Calculated Claim Value").toString());
    assertThat(initialValue)
        .isEqualByComparingTo("4500.00");
    assertThat(row.get("SaBC Total Costing information - Final Claim Value"))
        .isEqualTo("0");
    assertThat(row.get("SaBC Total Costing information - Allowed Total VAT"))
        .isEqualTo("0");
    assertThat(row.get("SaBC Total Costing information - Allowed Total Inc VAT"))
        .isEqualTo("0");
    assertThat(row.get("SaBC Total Costing information - Assessed Total VAT"))
        .isEqualTo("0");
    assertThat(row.get("SaBC Total Costing information - Assessed Total Inc VAT"))
        .isEqualTo("0");
  }

  private @NotNull List<Map<String, Object>> getDataForClaimId(String claimId) {

    jdbcTemplate.update("""
      REFRESH MATERIALIZED VIEW claims.mvw_report_000
      """);

    return
        jdbcTemplate.queryForList("""
        SELECT *
        FROM claims.mvw_report_000
        WHERE "Case Details - Claim ID" = ?
        """,
            claimId
        );
  }

}
