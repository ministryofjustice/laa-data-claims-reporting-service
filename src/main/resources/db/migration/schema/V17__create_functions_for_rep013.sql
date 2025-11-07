CREATE OR REPLACE FUNCTION claims.month_order(submission_period TEXT)
RETURNS INT AS $$
BEGIN
    BEGIN
    RETURN EXTRACT(MONTH FROM TO_DATE(UPPER(SUBSTRING(submission_period, 1, 3)), 'MON'))::INT;
    EXCEPTION WHEN others THEN
        RETURN 99;
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION claims.refresh_report013()
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    column_definitions  TEXT;
    column_selections   TEXT;
    crosstab_columns    TEXT;
    values_clause       TEXT;
    insert_sql          TEXT;
    data_query          TEXT;
BEGIN
    -- Step 1: Build dynamic column lists directly from subqueries
    SELECT string_agg('"' || submission_period || '" TEXT', ', ' ORDER BY year_order, month_order)
    INTO column_definitions
    FROM (
             SELECT DISTINCT submission_period,
                             claims.month_order(submission_period) AS month_order,
                             SUBSTRING(submission_period, 5)::INTEGER AS year_order
             FROM claims.submission
             WHERE status = 'VALIDATION_SUCCEEDED'
               AND submission_period IS NOT NULL
         ) AS ordered_periods;

    SELECT string_agg('COALESCE(ROUND(p."' || submission_period || '",2)::TEXT, '''') AS "' || submission_period || '"', ', ' ORDER BY year_order, month_order)
    INTO column_selections
    FROM (
             SELECT DISTINCT submission_period,
                             claims.month_order(submission_period) AS month_order,
                             SUBSTRING(submission_period, 5)::INTEGER AS year_order
             FROM claims.submission
             WHERE status = 'VALIDATION_SUCCEEDED'
               AND submission_period IS NOT NULL
         ) AS ordered_periods;

    SELECT '"' || string_agg(submission_period, '" NUMERIC(12,2), "' ORDER BY year_order, month_order) || '" NUMERIC(12,2)'
    INTO crosstab_columns
    FROM (
             SELECT DISTINCT submission_period,
                             claims.month_order(submission_period) AS month_order,
                             SUBSTRING(submission_period, 5)::INTEGER AS year_order
             FROM claims.submission
             WHERE status = 'VALIDATION_SUCCEEDED'
               AND submission_period IS NOT NULL
         ) AS ordered_periods;

    SELECT 'VALUES (' || string_agg('''' || submission_period || '''', '), (' ORDER BY year_order, month_order) || ')'
    INTO values_clause
    FROM (
             SELECT DISTINCT submission_period,
                             claims.month_order(submission_period) AS month_order,
                             SUBSTRING(submission_period, 5)::INTEGER AS year_order
             FROM claims.submission
             WHERE status = 'VALIDATION_SUCCEEDED'
               AND submission_period IS NOT NULL
         ) AS ordered_periods;

    -- Step 2: Create the table if it doesn't exist
    EXECUTE 'CREATE TABLE IF NOT EXISTS claims.report_013 (
            order_by_id BIGINT,
            "Provider Office Account Number" TEXT,
            "Area of Law" TEXT,
            ' || column_definitions || '
        )';

    -- Step 3: Clear persistent table
    TRUNCATE TABLE claims.report_013;

    -- Step 4: Build the data query
    data_query := $dq$
            WITH canonical_submission AS (
                SELECT s.id,
                       s.office_account_number,
                       UPPER(
                           CASE
                               WHEN UPPER(s.area_of_law) = 'CRIME LOWER' THEN 'CRIME'
                               WHEN UPPER(s.area_of_law) = 'LEGAL HELP' THEN 'CIVIL'
                               ELSE s.area_of_law
                           END
                       ) AS area_of_law,
                       s.is_nil_submission,
                       s.submission_period
                FROM claims.submission AS s
                WHERE s.status = 'VALIDATION_SUCCEEDED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM claims.submission AS newer
                      WHERE newer.previous_submission_id = s.id
                  )
                  AND UPPER(
                       CASE
                           WHEN UPPER(s.area_of_law) = 'CRIME LOWER' THEN 'CRIME'
                           WHEN UPPER(s.area_of_law) = 'LEGAL HELP' THEN 'CIVIL'
                           ELSE s.area_of_law
                       END
                  ) IN ('CIVIL','CRIME','MEDIATION')
            ),
            submission_totals AS (
                SELECT cs.id AS submission_id,
                       SUM(cf.total_amount) AS total_amount
                FROM canonical_submission cs
                LEFT JOIN claims.claim c
                  ON c.submission_id = cs.id AND c.status='VALID'
                LEFT JOIN claims.calculated_fee_detail cf
                  ON cf.claim_id = c.id
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

    -- Step 5: Build final INSERT
    insert_sql := 'INSERT INTO claims.report_013
            SELECT
                ROW_NUMBER() OVER (
                    ORDER BY split_part(p.row_id, ''|'', 2), split_part(p.row_id, ''|'', 1)
                )::BIGINT AS order_by_id,
                split_part(p.row_id, ''|'', 2) AS "Provider Office Account Number",
                split_part(p.row_id, ''|'', 1) AS "Area of Law",
                ' || column_selections || '
            FROM claims.crosstab(
                ' || quote_literal(data_query) || ',
                ' || quote_literal(values_clause) || '
            ) AS p(row_id TEXT, ' || crosstab_columns || ')
            ORDER BY "Provider Office Account Number", "Area of Law"';

        -- Step 6: Execute the insert
    EXECUTE insert_sql;
END $$;