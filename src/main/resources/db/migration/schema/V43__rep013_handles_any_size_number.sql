CREATE OR REPLACE FUNCTION claims.refresh_report013()
    RETURNS void
    LANGUAGE 'plpgsql'
    COST 100
    VOLATILE PARALLEL UNSAFE
AS $BODY$
DECLARE
column_definitions  TEXT;
    column_selections   TEXT;
    crosstab_columns    TEXT;
    values_clause       TEXT;
    insert_sql          TEXT;
    data_query          TEXT;
    report_data_exists         BOOLEAN;
BEGIN
    --------------------------------------------------------------------
    -- Step 0: Detect whether we have ANY submission_period values
    --         and if not then create an empty table and exit
    --------------------------------------------------------------------
SELECT EXISTS(
    SELECT 1
    FROM claims.submission AS s
    WHERE s.status = 'VALIDATION_SUCCEEDED'
      AND s.submission_period IS NOT NULL
      AND claims.submission_is_not_replaced(s.id)
      AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
) INTO report_data_exists;

EXECUTE 'DROP TABLE IF EXISTS claims.report_013';

IF NOT report_data_exists THEN
        ----------------------------------------------------------------
        -- NO submission periods -> create an empty report table
        ----------------------------------------------------------------
        EXECUTE '
            CREATE TABLE claims.report_013 (
                "Provider Office Account Number" TEXT,
                "Area of Law" TEXT
            )';

        -- No data to insert
        RETURN;
END IF;

    -- Step 1: Build dynamic column lists directly from subqueries
SELECT string_agg('"' || submission_period || '" TEXT', ', ' ORDER BY year_order, month_order)
INTO column_definitions
FROM (
         SELECT DISTINCT submission_period,
                         claims.month_order(submission_period) AS month_order,
                         SUBSTRING(submission_period, 5)::INTEGER AS year_order
         FROM claims.submission AS s
         WHERE s.status = 'VALIDATION_SUCCEEDED'
           AND s.submission_period IS NOT NULL
           AND claims.submission_is_not_replaced(s.id)
           AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
     ) AS ordered_periods;

SELECT string_agg('COALESCE(ROUND(p."' || submission_period || '",2)::TEXT, '''') AS "' || submission_period || '"', ', ' ORDER BY year_order, month_order)
INTO column_selections
FROM (
         SELECT DISTINCT submission_period,
                         claims.month_order(submission_period) AS month_order,
                         SUBSTRING(submission_period, 5)::INTEGER AS year_order
         FROM claims.submission AS s
         WHERE s.status = 'VALIDATION_SUCCEEDED'
           AND s.submission_period IS NOT NULL
           AND claims.submission_is_not_replaced(s.id)
           AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
     ) AS ordered_periods;

SELECT '"' || string_agg(submission_period, '" NUMERIC(1000,2), "' ORDER BY year_order, month_order) || '" NUMERIC(1000,2)'
INTO crosstab_columns
FROM (
         SELECT DISTINCT submission_period,
                         claims.month_order(submission_period) AS month_order,
                         SUBSTRING(submission_period, 5)::INTEGER AS year_order
         FROM claims.submission AS s
         WHERE s.status = 'VALIDATION_SUCCEEDED'
           AND s.submission_period IS NOT NULL
           AND claims.submission_is_not_replaced(s.id)
           AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
     ) AS ordered_periods;

SELECT 'VALUES (' || string_agg('''' || submission_period || '''', '), (' ORDER BY year_order, month_order) || ')'
INTO values_clause
FROM (
         SELECT DISTINCT submission_period,
                         claims.month_order(submission_period) AS month_order,
                         SUBSTRING(submission_period, 5)::INTEGER AS year_order
         FROM claims.submission AS s
         WHERE s.status = 'VALIDATION_SUCCEEDED'
           AND s.submission_period IS NOT NULL
           AND claims.submission_is_not_replaced(s.id)
           AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
     ) AS ordered_periods;

-- Step 2: Create the table from the latest data
EXECUTE 'CREATE TABLE IF NOT EXISTS claims.report_013 (
            "Provider Office Account Number" TEXT,
            "Area of Law" TEXT,
            ' || column_definitions || '
        )';

-- Step 3: Build the data query
data_query := $dq$
            WITH canonical_submission AS (
                SELECT s.id,
                       s.office_account_number,
                       claims.normalise_area_of_law(s.area_of_law) AS area_of_law,
                       s.is_nil_submission,
                       s.submission_period
                FROM claims.submission AS s
                WHERE s.status = 'VALIDATION_SUCCEEDED'
                  AND claims.submission_is_not_replaced(s.id)
                  AND claims.normalise_area_of_law(s.area_of_law) IN ('CIVIL', 'CRIME LOWER', 'MEDIATION')
                ),
            claim_totals AS (
                SELECT c.submission_id,
                       c.id AS claim_id,
                       CASE
                           WHEN a_latest.claim_id IS NULL THEN COALESCE(SUM(cf.total_amount), 0)
                           ELSE COALESCE(MAX(a_latest.allowed_total_incl_vat), 0)
                       END AS claim_amount
                FROM canonical_submission cs
                JOIN claims.claim c
                  ON c.submission_id = cs.id AND c.status IN ('VOID', 'VALID')
                LEFT JOIN claims.calculated_fee_detail cf
                  ON cf.claim_id = c.id
                LEFT JOIN LATERAL (
                    SELECT a.claim_id,
                           a.allowed_total_incl_vat
                    FROM claims.assessment a
                    WHERE a.claim_id = c.id
                    ORDER BY a.created_on DESC NULLS LAST
                    LIMIT 1
                ) a_latest ON true
                GROUP BY c.submission_id, c.id, a_latest.claim_id
            ),
            submission_totals AS (
                SELECT cs.id AS submission_id,
                       SUM(ct.claim_amount) AS total_amount
                FROM canonical_submission cs
                LEFT JOIN claim_totals ct
                  ON ct.submission_id = cs.id
                GROUP BY cs.id
            ),
            final_aggregated AS (
                SELECT cs.office_account_number,
                       cs.area_of_law,
                       cs.submission_period,
                       CASE WHEN cs.is_nil_submission THEN ROUND(0::numeric,2)
                            ELSE ROUND(COALESCE(st.total_amount,0),2)
                       END AS month_total
                FROM canonical_submission cs
                LEFT JOIN submission_totals st ON st.submission_id = cs.id
            )
SELECT concat_ws('|', fa.area_of_law, fa.office_account_number),
       fa.submission_period,
       fa.month_total
FROM final_aggregated fa
ORDER BY 1,2
    $dq$;

-- Step 4: Build final INSERT
insert_sql := 'INSERT INTO claims.report_013
            SELECT
                split_part(p.row_id, ''|'', 2) AS "Provider Office Account Number",
                CASE split_part(p.row_id, ''|'', 1)
                    WHEN ''CIVIL'' THEN ''LEGAL_HELP''
                    WHEN ''CRIME LOWER'' THEN ''CRIME_LOWER''
                    ELSE split_part(p.row_id, ''|'', 1)
                END AS "Area of Law",
                ' || column_selections || '
            FROM claims.crosstab(
                ' || quote_literal(data_query) || ',
                ' || quote_literal(values_clause) || '
            ) AS p(row_id TEXT, ' || crosstab_columns || ')';

        -- Step 6: Execute the insert
EXECUTE insert_sql;
END
$BODY$;
