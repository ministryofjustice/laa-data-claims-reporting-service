CREATE MATERIALIZED VIEW mvw_report_012 AS
/*
  REP012 Original Submission Values Report
  Scope: all submission periods present in claims.submission.
*/
WITH submission_normalised AS (
    SELECT
        s.id AS submission_id,
        s.office_account_number,
        s.area_of_law,
        s.submission_period,
        s.status,
        s.created_on,
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
),
eligible_submissions AS (
    SELECT
        sn.submission_id,
        sn.office_account_number,
        sn.area_of_law,
        sn.submission_period_date,
        sn.created_on,
        ROW_NUMBER() OVER (
            PARTITION BY
                sn.office_account_number,
                sn.area_of_law,
                sn.submission_period_date
            ORDER BY
                sn.created_on DESC,
                sn.submission_id DESC
        ) AS rn
    FROM submission_normalised AS sn
    WHERE sn.status = 'VALIDATION_SUCCEEDED'
      AND sn.area_of_law IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
      AND sn.submission_period_date IS NOT NULL
),
original_submissions AS (
    SELECT *
    FROM eligible_submissions
    WHERE rn = 1
),
valid_claim_counts AS (
    SELECT
        c.submission_id,
        COUNT(*) FILTER (WHERE c.status = 'VALID') AS valid_claim_count
    FROM claims.claim AS c
    WHERE c.submission_id IN (SELECT submission_id FROM original_submissions)
    GROUP BY c.submission_id
),
fee_totals AS (
    SELECT
        c.submission_id,
        SUM(COALESCE(cfd.total_amount, 0)) AS total_fee
    FROM claims.claim AS c
    JOIN claims.calculated_fee_detail AS cfd
      ON cfd.claim_id = c.id
    WHERE c.submission_id IN (SELECT submission_id FROM original_submissions)
    GROUP BY c.submission_id
)
SELECT
    os.office_account_number                                  AS "Provider office account number",
    TO_CHAR(os.submission_period_date, 'MON-YYYY')            AS "Submission month",
    os.area_of_law                                            AS "Area of law",
    COALESCE(
            ROUND(ft.total_fee, 2),
            0::NUMERIC(18, 2)
    ) AS "Original submission value",
    TO_CHAR(os.created_on AT TIME ZONE 'Europe/London', 'DD/MM/YYYY') AS "Date submission was uploaded"
FROM original_submissions AS os
         LEFT JOIN valid_claim_counts AS vcc
                   ON vcc.submission_id = os.submission_id
         LEFT JOIN fee_totals AS ft
                   ON ft.submission_id = os.submission_id
ORDER BY
    os.office_account_number,
    os.submission_period_date,
    os.area_of_law;