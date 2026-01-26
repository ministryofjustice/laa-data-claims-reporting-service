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

  @AfterEach
  void cleanup() {
    jdbcTemplate.update("""
        DELETE FROM claims.assessment
        """);
  }

  @Test
  void claimDataIsReturnedIfNoAssessmentDataPresent() {
    // When
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_SINGLE_FEE_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("Total Current Claim Value").toString());
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
    BigDecimal claimValue =
        new BigDecimal(row.get("Total Current Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    assertThat(row.get("Assessed Total Inc VAT"))
        .isEqualTo("");
  }

  @Test
  void latestFeeAndAssessmentIsReturnedIfMultipleFeesAndOneAssessmentPresent() {
    // When
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA', ?::uuid, '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00, 1400.00,
                    210.00, 1440.00, 'Test Assessor', now() )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("Total Current Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("Assessed Total Inc VAT").toString());
    assertThat(claimAssessedValue)
        .isEqualByComparingTo("1400.00");
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
                    210.00, 1440.00, 'Test Assessor', now() - interval '1 day' )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    jdbcTemplate.update(
        """
            INSERT INTO claims.assessment
            (id, claim_id, claim_summary_fee_id, assessment_outcome, assessed_total_vat, assessed_total_incl_vat,
             allowed_total_vat, allowed_total_incl_vat, created_by_user_id, created_on)
            VALUES ('AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAA2', ?::uuid, '66666666-6666-6666-6666-666666666666', 'REDUCED_STILL_ESCAPED', 200.00, 1300.00,
                    210.00, 1440.00, 'Test Assessor', now() )
            """,
        CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);

    List<Map<String, Object>> rows = getDataForClaimId(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    // Then
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row.get("Claim ID"))
        .isEqualTo(CLAIM_ID_WITH_MULTIPLE_FEES_AND_NO_ASSESSMENTS);
    BigDecimal claimValue =
        new BigDecimal(row.get("Total Current Claim Value").toString());
    assertThat(claimValue)
        .isEqualByComparingTo("2100.00");
    BigDecimal claimAssessedValue =
        new BigDecimal(row.get("Assessed Total Inc VAT").toString());
    assertThat(claimAssessedValue)
        .isEqualByComparingTo("1300.00");
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
