--Drop and recreate all materialised views and data source tables to remove ineffective ordering
--Order by clause is now implemented in the CSV generation code instead.

DROP MATERIALIZED VIEW IF EXISTS claims.mvw_report_000;

CREATE MATERIALIZED VIEW claims.mvw_report_000 AS
       -- Report: Combined Data Extract for Submit a Bulk Claim Data (REP000)
WITH submission_periods AS (
    SELECT
        s.id,
        s.bulk_submission_id,
        s.office_account_number,
        s.submission_period,
        s.area_of_law,
        s.status AS submission_status,
        s.crime_lower_schedule_number,
        s.legal_help_submission_reference,
        s.mediation_submission_reference,
        s.previous_submission_id,
        s.is_nil_submission,
        s.number_of_claims,
        s.error_messages,
        s.created_on,
        s.updated_on,
        s.created_by_user_id,
        s.updated_by_user_id,
        CASE
            WHEN s.submission_period ~ '^\d{4}-(0[1-9]|1[0-2])$' THEN
                TO_DATE(s.submission_period || '-01', 'YYYY-MM-DD')
            WHEN s.submission_period ~ '^[A-Za-z]{3}-\d{4}$' THEN
                TO_DATE(INITCAP(s.submission_period), 'Mon-YYYY')
            ELSE
                NULL
        END AS submission_period_start
    FROM claims.submission AS s
),
latest_calculated_fee_detail AS (
    SELECT
        calc.*
    FROM (
        SELECT
            cfd.*,
            ROW_NUMBER() OVER (
                PARTITION BY cfd.claim_id
                ORDER BY COALESCE(cfd.updated_on, cfd.created_on) DESC NULLS LAST
            ) AS rn
        FROM claims.calculated_fee_detail AS cfd
    ) AS calc
    WHERE calc.rn = 1
)
SELECT
    COALESCE(sp.id::text, '')                                               AS "Submission ID",
    COALESCE(sp.office_account_number, '')                                  AS "Office Account Number",
    COALESCE(TO_CHAR(sp.submission_period_start, 'MON-YYYY'), '')           AS "Submission Period",
    COALESCE(TO_CHAR(sp.submission_period_start, 'DD/MM/YYYY'), '')         AS "Submission For Date",
    COALESCE(TO_CHAR(DATE(bs.created_on), 'DD/MM/YYYY'), '')                AS "Date Submitted",
    COALESCE(sp.submission_status, '')                                      AS "Submission Status",
    COALESCE(sp.area_of_law, '')                                            AS "Area of Law",
    COALESCE(crime_lower_schedule_number, '')                               AS "Crime Lower Schedule Number",
    COALESCE(legal_help_submission_reference, '')                           AS "Legal Help Submission Reference",
    COALESCE(mediation_submission_reference, '')                            AS "Mediation Submission Reference",
    COALESCE(c.id::text, '')                                                AS "Claim ID",
    COALESCE(c.line_number::text, '')                                       AS "Line Number",
    COALESCE(c.case_reference_number, '')                                   AS "Case Reference Number",
    COALESCE(c.unique_file_number, '')                                      AS "Unique File Number",
    COALESCE(cc.case_id, '')                                                AS "Case ID",
    COALESCE(cc.unique_case_id, '')                                         AS "Unique Case ID",
    COALESCE(c.maat_id, '')                                         		AS "MAAT ID",
    COALESCE(c.scheme_id, '')                                               AS "Scheme ID",
    ''                                                                      AS "Has Post Submission Change", -- will be populated after FSP adds this field
    COALESCE(
            CASE WHEN cc.stage_reached_code = 'VOID' THEN 'Y' ELSE 'N' END, 'N'
    ) 																        AS "Is Void",
    COALESCE
    (CASE WHEN c.matched_claim_id IS NOT NULL
              THEN 'Y'
          ELSE 'N' END, 'N'
    ) 																	    AS "Is Duplicate Claim",
    COALESCE(TO_CHAR(c.case_start_date, 'DD/MM/YYYY'), '')                  AS "Case Start Date",
    COALESCE(TO_CHAR(c.case_concluded_date, 'DD/MM/YYYY'), '')              AS "Case Concluded Date",
    COALESCE(c.matter_type_code, '')                                        AS "Matter Type Code",
    COALESCE(LEFT(c.matter_type_code, 4), '')                               AS "Matter Type 1",
    COALESCE(RIGHT(c.matter_type_code, 4), '')                              AS "Matter Type 2",
    COALESCE(cc.case_stage_code, '')                                        AS "Case Stage Level",
    COALESCE(cc.stage_reached_code, '')                                     AS "Stage Reached",
    COALESCE(cc.outcome_code, '')                                           AS "Outcome Code",
    COALESCE(TO_CHAR(cc.transfer_date, 'DD/MM/YYYY'), '')                   AS "Transfer Date",
    COALESCE(c.delivery_location, '')                                       AS "Delivery Location",
    COALESCE(TO_CHAR(c.representation_order_date, 'DD/MM/YYYY'), '')        AS "Representation Order Date",
    COALESCE(c.police_station_court_prison_id, '')                          AS "Police Station Court Prison ID",
    -- CLIENT INFORMATION
    COALESCE(cl.client_forename, '')                                        AS "Client Forename",
    COALESCE(cl.client_surname, '')                                         AS "Client Surname",
    COALESCE(TO_CHAR(cl.client_date_of_birth, 'DD/MM/YYYY'), '')            AS "Client Date of Birth",
    COALESCE(cl.unique_client_number, '')                                   AS "Unique Client Number",
    COALESCE(cl.home_office_client_number, '')                              AS "Home Office Client Number",
    COALESCE(cl.gender_code, '')                                            AS "Gender",
    COALESCE(cl.ethnicity_code, '')                                         AS "Ethnicity",
    COALESCE(cl.disability_code, '')                                        AS "Disability",
    COALESCE(cl.client_postcode, '')                                        AS "Client Postcode",
    COALESCE(cl.client_type_code, '')                                       AS "Client Type Code",
    COALESCE(cl.is_legally_aided::text, '')                                 AS "First Client Legally Aided",
    COALESCE(cl.client_2_forename, '')                                      AS "Second Client Forename",
    COALESCE(cl.client_2_surname, '')                                       AS "Second Client Surname",
    COALESCE(TO_CHAR(cl.client_2_date_of_birth, 'DD/MM/YYYY'), '')          AS "Second Client Date of Birth",
    COALESCE(cl.client_2_postcode, '')                                      AS "Second Client Postcode",
    COALESCE(cl.client_2_gender_code, '')                                   AS "Second Client Gender",
    COALESCE(cl.client_2_ethnicity_code, '')                                AS "Second Client Ethnicity",
    COALESCE(cl.client_2_disability_code, '')                               AS "Second Client Disability",
    COALESCE(cl.client_2_is_legally_aided::text, '')                        AS "Second Client Legally Aided",
    COALESCE(cl.client_2_ucn::text, '')                			  	        AS "Second Client Unique Client Number",
    -- Intentional: "Total Value" removed per stakeholder decision (25/09/2025)
    COALESCE(calc.category_of_law, '')                                      AS "Category of Law Code",
    COALESCE(c.fee_code, '')                                                AS "Fee Code",
    COALESCE(calc.fee_code_description, '')                                 AS "Fee Code Description",
    COALESCE(calc.fee_type, '')                                             AS "Fee Type",
    COALESCE(cc.standard_fee_category_code, '')                             AS "Standard Fee Category Code",
    COALESCE(
            CASE
                WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                ELSE NULL
                END, '')                                                    AS "Total Current Claim Value",
    COALESCE(calc.vat_rate_applied::text, '')                               AS "VAT Rate Applied",
    COALESCE(csf.is_vat_applicable::text, '')                               AS "VAT Indicator",
    COALESCE(csf.waiting_time::text, '')                                    AS "Waiting Time",
    COALESCE(csf.travel_time::text, '')                                     AS "Travel Time",
    COALESCE(csf.advice_time::text, '')                                     AS "Advice Time",
    COALESCE(csf.net_profit_costs_amount::text, '')                         AS "Profit Costs",
    COALESCE(csf.net_counsel_costs_amount::text, '')                        AS "Counsel Fees",
    COALESCE(csf.net_disbursement_amount::text, '')                         AS "Disbursement Costs",
    COALESCE(csf.disbursements_vat_amount::text, '')                        AS "Disbursement VAT Costs",
    COALESCE(csf.travel_waiting_costs_amount::text, '')                     AS "Travel Waiting Costs",
    COALESCE(csf.net_waiting_costs_amount::text, '')                        AS "Net Waiting Costs Amount",
    COALESCE(csf.jr_form_filling_amount::text, '')                          AS "JR Form Filling Costs",
    COALESCE(csf.costs_damages_recovered_amount::text, '')                  AS "Cost / Damages Recovered",
    COALESCE(csf.detention_travel_waiting_costs_amount::text, '')           AS "Detention Travel & Waiting Costs",
    COALESCE(csf.adjourned_hearing_fee_amount::text, '')                    AS "Adjourned Hearing Fee Count",
    COALESCE(c.mediation_sessions_count::text, '')                          AS "Mediation Sessions Count",
    COALESCE(csf.cmrh_oral_count::text, '')                                 AS "CMRH Oral Count",
    COALESCE(csf.cmrh_telephone_count::text, '')                            AS "CMRH Telephone Count",
    COALESCE(csf.ho_interview::text, '')                                    AS "HO Interview Count",
    COALESCE(csf.medical_reports_count::text, '')                           AS "Medical Reports Count",
    COALESCE(csf.meetings_attended_code, '')                                AS "Meetings Attended Code",
    COALESCE(c.police_station_court_attendances_count::text, '')            AS "Police Station Court Attendances Count",
    COALESCE(c.suspects_defendants_count::text, '')                         AS "Suspects Defendants Count",
    COALESCE(calc.vat_indicator::text, '')                                  AS "Current VAT Indicator",
    COALESCE(calc.net_profit_costs_amount::text, '')                        AS "Current Net Profit Costs Amount",
    COALESCE(calc.net_cost_of_counsel_amount::text, '')                     AS "Current Net Cost Of Counsel Amount",
    COALESCE(calc.disbursement_amount::text, '')                            AS "Current Disbursement Amount",
    COALESCE(calc.travel_and_waiting_costs_amount::text, '')                AS "Current Travel And Waiting Costs Amount",
    COALESCE(calc.detention_travel_and_waiting_costs_amount::text, '')      AS "Current Detention And Waiting Costs Amount",
    COALESCE(calc.jr_form_filling_amount::text, '')                         AS "Current JR Form Filling Amount",
    COALESCE(calc.fixed_fee_amount::text, '')                               AS "Current Fixed Fee Amount",
    COALESCE(calc.escape_case_flag::text, '')                               AS "Current Escape Case Flag",
    COALESCE(calc.hourly_total_amount::text, '')                            AS "Current Hourly Total Amount",
    COALESCE(calc.bolt_on_total_fee_amount::text, '')                       AS "Current Bolt On Total Fee Amount",
    COALESCE(calc.bolt_on_adjourned_hearing_count::text, '')                AS "Current Bolt On Adjourned Hearing Count",
    COALESCE(calc.bolt_on_adjourned_hearing_fee::text, '')                  AS "Current Bolt On Adjourned Hearing Fee",
    COALESCE(calc.bolt_on_cmrh_telephone_count::text, '')                   AS "Current Bolt On CMRH Telephone Count",
    COALESCE(calc.bolt_on_cmrh_telephone_fee::text, '')                     AS "Current Bolt On CMRH Telephone Fee",
    COALESCE(calc.bolt_on_cmrh_oral_count::text, '')                        AS "Current Bolt On CMRH Oral Count",
    COALESCE(calc.bolt_on_cmrh_oral_fee::text, '')                          AS "Current Bolt On CMRH Oral Fee",
    COALESCE(calc.bolt_on_home_office_interview_count::text, '')            AS "Current Bolt On Home Office Interview Count",
    COALESCE(calc.bolt_on_home_office_interview_fee::text, '')              AS "Current Bolt On Home Office Interview Fee",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.fixed_fee_amount IS NOT NULL
                    THEN ROUND(calc.fixed_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Fixed Fee VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_profit_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_profit_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Profit Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_cost_of_counsel_amount IS NOT NULL
                    THEN ROUND(calc.net_cost_of_counsel_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Counsel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_travel_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_travel_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Travel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_waiting_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_waiting_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Waiting Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.jr_form_filling_amount IS NOT NULL
                    THEN ROUND(calc.jr_form_filling_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current JR / Form Filling Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.bolt_on_total_fee_amount IS NOT NULL
                    THEN ROUND(calc.bolt_on_total_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "Current Bolt On Fees VAT",
    COALESCE(cc.is_legacy_case::text, '')                                           AS "Legacy Case Flag",
    COALESCE(csf.is_london_rate::text, '')                                          AS "London Rate Flag",
    COALESCE(csf.is_tolerance_applicable::text, '')                                 AS "Tolerance Indicator",
    COALESCE(csf.is_substantive_hearing::text, '')                                  AS "Substantive Hearing Flag",
    COALESCE(csf.is_additional_travel_payment::text, '')                            AS "Additional Travel Payment Flag",
    COALESCE(csf.local_authority_number, '')                                        AS "Local Authority Number",
    COALESCE(c.procurement_area_code, '')                                           AS "Procurement Area Code",
    COALESCE(c.access_point_code, '')                                               AS "Access Point Code",
    COALESCE(c.referral_source, '')                                                 AS "Referral Source Code",
    COALESCE(csf.ait_hearing_centre_code, '')                                       AS "AIT Hearing Centre Code",
    COALESCE(cc.exceptional_case_funding_reference, '')                             AS "ECF Reference",
    CASE WHEN
             COALESCE(cc.exceptional_case_funding_reference, '') <> ''
             THEN 'Y'
         ELSE 'N'
        END		 															AS "Is Exceptional Claim",
    COALESCE(cc.exemption_criteria_satisfied, '')                           AS "Exemption Criteria Satisfied",
    COALESCE(cl.cla_reference_number, '')                                   AS "CLA Reference Number",
    COALESCE(cl.cla_exemption_code, '')                                     AS "CLA Exemption Code",
    COALESCE(csf.prior_authority_reference, '')                             AS "Immigration Prior Authority Number",
    COALESCE(cc.is_postal_application_accepted::text, '')                   AS "Postal Application Accepted",
    COALESCE(csf.is_irc_surgery::text, '')                    				AS "IRC Surgery",
    COALESCE(csf.surgery_date::text, '')                     				AS "Surgery Date",
    COALESCE(csf.surgery_clients_count::text, '')							AS "Number Of Clients Seen At The Surgery",
    COALESCE(csf.surgery_matters_count::text, '')                         	AS "Number Of Surgery Clients Resulting In A Legal Help Matter Opened",
    COALESCE(cc.is_nrm_advice::text, '')                   				    AS "NRM Advice",
    COALESCE(cc.follow_on_work::text, '')                   			    AS "PRN Follow On Work",
    COALESCE(c.is_duty_solicitor::text, '')                  			    AS "Is Duty Solicitor",
    COALESCE(c.is_youth_court::text, '')                                    AS "Is Youth Court"
FROM submission_periods AS sp
         JOIN claims.bulk_submission AS bs
              ON bs.id = sp.bulk_submission_id
         JOIN claims.claim AS c
              ON c.submission_id = sp.id
         JOIN claims.claim_case AS cc
              ON cc.claim_id = c.id
         LEFT JOIN claims.claim_summary_fee AS csf
                   ON csf.claim_id = c.id
         LEFT JOIN latest_calculated_fee_detail AS calc
                   ON calc.claim_id = c.id
         LEFT JOIN claims.client AS cl
                   ON cl.claim_id = c.id
WHERE sp.submission_status = 'VALIDATION_SUCCEEDED'
  AND c.status = 'VALID'
  AND bs.created_on >= (CURRENT_DATE - INTERVAL '3 years');

DROP MATERIALIZED VIEW IF EXISTS claims.mvw_report_012;

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
                   ON ft.submission_id = os.submission_id;

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

-- Step 2: Drop persistent table
    EXECUTE 'DROP TABLE IF EXISTS claims.report_013';

-- Step 3: Create the table from the latest data
    EXECUTE 'CREATE TABLE IF NOT EXISTS claims.report_013 (
            "Provider Office Account Number" TEXT,
            "Area of Law" TEXT,
            ' || column_definitions || '
        )';

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
                split_part(p.row_id, ''|'', 2) AS "Provider Office Account Number",
                split_part(p.row_id, ''|'', 1) AS "Area of Law",
                ' || column_selections || '
            FROM claims.crosstab(
                ' || quote_literal(data_query) || ',
                ' || quote_literal(values_clause) || '
            ) AS p(row_id TEXT, ' || crosstab_columns || ')';

        -- Step 6: Execute the insert
    EXECUTE insert_sql;
END $$;

