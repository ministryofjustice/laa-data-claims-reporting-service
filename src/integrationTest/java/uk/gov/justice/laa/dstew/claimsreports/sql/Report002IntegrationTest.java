package uk.gov.justice.laa.dstew.claimsreports.sql;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

class Report002IntegrationTest extends IntegrationTestBase {

  @Test
  void report002UsesMappedFieldsAndLeavesEnrichedFieldsBlank() {
    UUID matterStartId = UUID.randomUUID();

    jdbcTemplate.update(
        """
            INSERT INTO claims.matter_start (
                id, submission_id, schedule_reference, category_code, procurement_area_code,
                access_point_code, delivery_location, created_by_user_id, created_on,
                updated_by_user_id, updated_on, number_of_matter_starts
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, now(), NULL, NULL, ?)
            """,
        matterStartId,
        "22222222-2222-2222-2222-222222222222",
        "SCHED-REP002-1",
        "CAT-INT-001",
        "PA-INT-001",
        "AP-INT-001",
        "LOC-001",
        "integration_test_user",
        17
    );

    try {
      jdbcTemplate.update("REFRESH MATERIALIZED VIEW claims.mvw_report_002");

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          """
              SELECT *
              FROM claims.mvw_report_002
              WHERE "Office code" = 'OA001'
                AND "Submission for date" = 'APR-2025'
                AND "Category code" = 'CAT-INT-001'
                AND "Procurement area code" = 'PA-INT-001'
                AND "Access point code" = 'AP-INT-001'
              """
      );

      assertThat(rows).hasSize(1);
      Map<String, Object> row = rows.getFirst();
      assertThat(row.keySet()).containsExactly(
          "Firm name",
          "Firm number",
          "File name",
          "Office code",
          "Submission for date",
          "Category code",
          "Procurement area code",
          "Procurement area desc",
          "Access point code",
          "Access point desc",
          "New cases count"
      );
      assertThat(row.get("Firm name")).isEqualTo("");
      assertThat(row.get("Firm number")).isEqualTo("");
      assertThat(row.get("File name")).isEqualTo("");
      assertThat(row.get("Procurement area desc")).isEqualTo("");
      assertThat(row.get("Access point desc")).isEqualTo("");
      assertThat(row.get("Office code")).isEqualTo("OA001");
      assertThat(row.get("Submission for date")).isEqualTo("APR-2025");
      assertThat(row.get("Category code")).isEqualTo("CAT-INT-001");
      assertThat(row.get("Procurement area code")).isEqualTo("PA-INT-001");
      assertThat(row.get("Access point code")).isEqualTo("AP-INT-001");
      assertThat(((Number) row.get("New cases count")).intValue()).isEqualTo(17);
    } finally {
      jdbcTemplate.update("DELETE FROM claims.matter_start WHERE id = ?", matterStartId);
      jdbcTemplate.update("REFRESH MATERIALIZED VIEW claims.mvw_report_002");
    }
  }

  @Test
  void report002OnlyIncludesLatestSubmissionVersion() {
    UUID newerSubmissionId = UUID.randomUUID();
    UUID olderMatterStartId = UUID.randomUUID();
    UUID newerMatterStartId = UUID.randomUUID();

    jdbcTemplate.update(
        """
            INSERT INTO claims.submission (
                id, bulk_submission_id, office_account_number, submission_period, area_of_law, status,
                crime_lower_schedule_number, previous_submission_id, is_nil_submission, number_of_claims,
                error_messages, created_by_user_id, created_on, provider_user_id
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?::uuid, ?, ?, ?, ?, now(), ?)
            """,
        newerSubmissionId,
        "11111111-1111-1111-1111-111111111111",
        "OA001",
        "APR-2025",
        "CRIME_LOWER",
        "VALIDATION_SUCCEEDED",
        "CSN-NEWER",
        "22222222-2222-2222-2222-222222222222",
        false,
        1,
        null,
        "integration_test_user",
        "integration_test_provider"
    );

    jdbcTemplate.update(
        """
            INSERT INTO claims.matter_start (
                id, submission_id, schedule_reference, category_code, procurement_area_code,
                access_point_code, delivery_location, created_by_user_id, created_on,
                updated_by_user_id, updated_on, number_of_matter_starts
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, now(), NULL, NULL, ?)
            """,
        olderMatterStartId,
        "22222222-2222-2222-2222-222222222222",
        "MSCH-OLD",
        "CAT-DEDUP",
        "PA-DEDUP",
        "AP-DEDUP",
        "DL-OLD",
        "integration_test_user",
        17
    );

    jdbcTemplate.update(
        """
            INSERT INTO claims.matter_start (
                id, submission_id, schedule_reference, category_code, procurement_area_code,
                access_point_code, delivery_location, created_by_user_id, created_on,
                updated_by_user_id, updated_on, number_of_matter_starts
            ) VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, now(), NULL, NULL, ?)
            """,
        newerMatterStartId,
        newerSubmissionId,
        "MSCH-NEW",
        "CAT-DEDUP",
        "PA-DEDUP",
        "AP-DEDUP",
        "DL-NEW",
        "integration_test_user",
        17
    );

    try {
      jdbcTemplate.update("REFRESH MATERIALIZED VIEW claims.mvw_report_002");

      List<Map<String, Object>> rows = jdbcTemplate.queryForList(
          """
              SELECT *
              FROM claims.mvw_report_002
              WHERE "Office code" = 'OA001'
                AND "Submission for date" = 'APR-2025'
                AND "Category code" = 'CAT-DEDUP'
                AND "Procurement area code" = 'PA-DEDUP'
                AND "Access point code" = 'AP-DEDUP'
              """
      );

      assertThat(rows).hasSize(1);
    } finally {
      jdbcTemplate.update("DELETE FROM claims.matter_start WHERE id IN (?, ?)", olderMatterStartId, newerMatterStartId);
      jdbcTemplate.update("DELETE FROM claims.submission WHERE id = ?", newerSubmissionId);
      jdbcTemplate.update("REFRESH MATERIALIZED VIEW claims.mvw_report_002");
    }
  }
}
