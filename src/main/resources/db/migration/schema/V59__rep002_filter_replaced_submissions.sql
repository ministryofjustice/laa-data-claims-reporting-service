DROP MATERIALIZED VIEW IF EXISTS mvw_report_002;

CREATE MATERIALIZED VIEW mvw_report_002 AS
-- Report: New Matter Starts data extract for Submit a Bulk Claim Data (REP002)
WITH latest_valid_submissions AS (
    SELECT
        s.id,
        s.office_account_number,
        CASE
            WHEN s.submission_period ~ '^\d{4}-\d{2}-\d{2}$'
                THEN TO_DATE(s.submission_period, 'YYYY-MM-DD')
            WHEN s.submission_period ~ '^\d{4}-\d{2}$'
                THEN TO_DATE(s.submission_period || '-01', 'YYYY-MM-DD')
            WHEN s.submission_period ~ '^[A-Za-z]{3}-\d{4}$'
                THEN TO_DATE(UPPER(s.submission_period), 'MON-YYYY')
            ELSE NULL
        END AS submission_period_date
    FROM claims.submission AS s
    WHERE s.status = 'VALIDATION_SUCCEEDED'
            AND NOT EXISTS (
                    SELECT 1
                    FROM claims.submission AS newer
                    WHERE newer.previous_submission_id = s.id
                        AND newer.status = 'VALIDATION_SUCCEEDED'
            )
),
submission_matter_starts AS (
    SELECT
        lvs.office_account_number,
        lvs.submission_period_date,
        m.category_code,
        m.procurement_area_code,
        m.access_point_code,
        m.number_of_matter_starts
    FROM latest_valid_submissions AS lvs
    INNER JOIN claims.matter_start AS m
        ON lvs.id = m.submission_id
)
SELECT
    ''                                                              AS "Firm name",
    ''                                                              AS "Firm number",
    ''                                                              AS "File name",
    COALESCE(sms.office_account_number, '')                         AS "Office code",
    COALESCE(TO_CHAR(sms.submission_period_date, 'MON-YYYY'), '')   AS "Submission for date",
    COALESCE(sms.category_code, '')                                 AS "Category code",
    COALESCE(sms.procurement_area_code, '')                         AS "Procurement area code",
    ''                                                              AS "Procurement area desc",
    COALESCE(sms.access_point_code, '')                             AS "Access point code",
    ''                                                              AS "Access point desc",
    sms.number_of_matter_starts                                     AS "New cases count"
FROM submission_matter_starts AS sms;