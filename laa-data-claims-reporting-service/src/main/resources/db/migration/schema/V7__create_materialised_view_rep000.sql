CREATE MATERIALIZED VIEW mvw_report_000 AS
       -- Report: Combined Data Extract for Submit a Bulk Claim Data (REP000)
WITH submission_periods AS (
    SELECT
        s.id,
        s.bulk_submission_id,
        s.office_account_number,
        s.submission_period,
        s.area_of_law,
        s.status AS submission_status,
        s.crime_schedule_number,
        s.civil_submission_reference,
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
    COALESCE(sp.office_account_number, '')                                  AS "office_account_number",
    COALESCE(TO_CHAR(sp.submission_period_start, 'MON-YYYY'), '')           AS "submission_period",
    COALESCE(TO_CHAR(sp.submission_period_start, 'DD/MM/YYYY'), '')         AS "submission_for_date",
    COALESCE(TO_CHAR(DATE(bs.created_on), 'DD/MM/YYYY'), '')                AS "date_submitted",
    COALESCE(sp.submission_status, '')                                      AS "submission_status",
    COALESCE(sp.area_of_law, '')                                            AS "area_of_law",
    COALESCE(crime_schedule_number, '')                                     AS "crime_submission_reference",
    COALESCE(civil_submission_reference, '')                                AS "civil_submission_reference",
    COALESCE(mediation_submission_reference, '')                            AS "mediation_submission_reference",
    COALESCE(c.id::text, '')                                                AS "claim_id",
    COALESCE(c.line_number::text, '')                                       AS "line_number",
    COALESCE(c.case_reference_number, '')                                   AS "case_reference_number",
    COALESCE(c.unique_file_number, '')                                      AS "unique_file_number",
    COALESCE(cc.case_id, '')                                                AS "case_id",
    COALESCE(TO_CHAR(c.case_start_date, 'DD/MM/YYYY'), '')                  AS "case_start_date",
    COALESCE(TO_CHAR(c.case_concluded_date, 'DD/MM/YYYY'), '')              AS "case_concluded_date",
    COALESCE(c.matter_type_code, '')                                        AS "matter_type_code",
    COALESCE(LEFT(c.matter_type_code, 4), '')                               AS "matter_type_1",
    COALESCE(RIGHT(c.matter_type_code, 4), '')                              AS "matter_type_2",
    COALESCE(cc.case_stage_code, '')                                        AS "case_stage_level",
    COALESCE(cc.stage_reached_code, '')                                     AS "stage_reached",
    COALESCE(cc.outcome_code, '')                                           AS "outcome_code",
    COALESCE(TO_CHAR(cc.transfer_date, 'DD/MM/YYYY'), '')                   AS "transfer_date",
    COALESCE(cl.client_forename, '')                                        AS "client_forename",
    COALESCE(cl.client_surname, '')                                         AS "client_surname",
    COALESCE(TO_CHAR(cl.client_date_of_birth, 'DD/MM/YYYY'), '')            AS "client_date_of_birth",
    COALESCE(cl.unique_client_number, '')                                   AS "unique_client_number",
    COALESCE(cl.home_office_client_number, '')                              AS "home_office_client_number",
    COALESCE(cl.gender_code, '')                                            AS "gender",
    COALESCE(cl.ethnicity_code, '')                                         AS "ethnicity",
    COALESCE(cl.disability_code, '')                                        AS "disability",
    COALESCE(cl.client_postcode, '')                                        AS "client_postcode",
    COALESCE(cl.client_type_code, '')                                       AS "client_type_code",
    COALESCE(cl.is_legally_aided::text, '')                                 AS "first_client_legally_aided",
    COALESCE(cl.client_2_forename, '')                                      AS "second_client_forename",
    COALESCE(cl.client_2_surname, '')                                       AS "second_client_surname",
    COALESCE(TO_CHAR(cl.client_2_date_of_birth, 'DD/MM/YYYY'), '')          AS "second_client_date_of_birth",
    COALESCE(cl.client_2_postcode, '')                                      AS "second_client_postcode",
    COALESCE(cl.client_2_gender_code, '')                                   AS "second_client_gender",
    COALESCE(cl.client_2_ethnicity_code, '')                                AS "second_client_ethnicity",
    COALESCE(cl.client_2_disability_code, '')                               AS "second_client_disability",
    COALESCE(cl.client_2_is_legally_aided::text, '')                        AS "second_client_legally_aided",
    COALESCE(calc.category_of_law, '')                                      AS "category_of_law_code",
    COALESCE(c.fee_code, '')                                                AS "fee_code",
    COALESCE(calc.fee_code_description, '')                                 AS "fee_code_description",
    COALESCE(calc.fee_type, '')                                             AS "fee_type",
    COALESCE(
            CASE
                WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                ELSE NULL
                END, '')                                                            AS "total_current_claim_value",
    COALESCE(calc.vat_rate_applied::text, '')                               AS "vat_rate_applied",
    COALESCE(csf.is_vat_applicable::text, '')                               AS "vat_indicator",
    COALESCE(csf.waiting_time::text, '')                                    AS "waiting_time",
    COALESCE(csf.travel_time::text, '')                                     AS "travel_time",
    COALESCE(csf.advice_time::text, '')                                     AS "advice_time",
    COALESCE(csf.net_profit_costs_amount::text, '')                         AS "profit_costs",
    COALESCE(csf.net_counsel_costs_amount::text, '')                        AS "counsel_fees",
    COALESCE(csf.net_disbursement_amount::text, '')                         AS "disbursement_costs",
    COALESCE(csf.disbursements_vat_amount::text, '')                        AS "disbursement_vat_costs",
    COALESCE(csf.travel_waiting_costs_amount::text, '')                     AS "travel_waiting_costs",
    COALESCE(csf.jr_form_filling_amount::text, '')                          AS "jr_form_filling_costs",
    COALESCE(csf.costs_damages_recovered_amount::text, '')                  AS "cost_/_damages_recovered",
    COALESCE(csf.detention_travel_waiting_costs_amount::text, '')           AS "detention_travel_&_waiting_costs",
    COALESCE(csf.adjourned_hearing_fee_amount::text, '')                    AS "adjourned_hearing_fee_count",
    COALESCE(c.mediation_sessions_count::text, '')                          AS "mediation_sessions_count",
    COALESCE(csf.cmrh_oral_count::text, '')                                 AS "cmrh_oral_count",
    COALESCE(csf.cmrh_telephone_count::text, '')                            AS "cmrh_telephone_count",
    COALESCE(csf.ho_interview::text, '')                                    AS "ho_interview_count",
    COALESCE(csf.medical_reports_count::text, '')                           AS "medical_reports_count",
    COALESCE(csf.meetings_attended_code, '')                                AS "meetings_attended_code",
    COALESCE(calc.vat_indicator::text, '')                                  AS "current_vat_indicator",
    COALESCE(calc.net_profit_costs_amount::text, '')                        AS "current_net_profit_costs_amount",
    COALESCE(calc.net_cost_of_counsel_amount::text, '')                     AS "current_net_cost_of_counsel_amount",
    COALESCE(calc.disbursement_amount::text, '')                            AS "current_disbursement_amount",
    COALESCE(calc.travel_and_waiting_costs_amount::text, '')                AS "current_travel_and_waiting_costs_amount",
    COALESCE(calc.detention_and_waiting_costs_amount::text, '')             AS "current_detention_and_waiting_costs_amount",
    COALESCE(calc.jr_form_filling_amount::text, '')                         AS "current_jr_form_filling_amount",
    COALESCE(calc.fixed_fee_amount::text, '')                               AS "current_fixed_fee_amount",
    COALESCE(calc.escape_case_flag::text, '')                               AS "current_escape_case_flag",
    COALESCE(calc.hourly_total_amount::text, '')                            AS "current_hourly_total_amount",
    COALESCE(calc.bolt_on_total_fee_amount::text, '')                       AS "current_bolt_on_total_fee_amount",
    COALESCE(calc.bolt_on_adjourned_hearing_count::text, '')                AS "current_bolt_on_adjourned_hearing_count",
    COALESCE(calc.bolt_on_adjourned_hearing_fee::text, '')                  AS "current_bolt_on_adjourned_hearing_fee",
    COALESCE(calc.bolt_on_cmrh_telephone_count::text, '')                   AS "current_bolt_on_cmrh_telephone_count",
    COALESCE(calc.bolt_on_cmrh_telephone_fee::text, '')                     AS "current_bolt_on_cmrh_telephone_fee",
    COALESCE(calc.bolt_on_cmrh_oral_count::text, '')                        AS "current_bolt_on_cmrh_oral_count",
    COALESCE(calc.bolt_on_cmrh_oral_fee::text, '')                          AS "current_bolt_on_cmrh_oral_fee",
    COALESCE(calc.bolt_on_home_office_interview_count::text, '')            AS "current_bolt_on_home_office_interview_count",
    COALESCE(calc.bolt_on_home_office_interview_fee::text, '')              AS "current_bolt_on_home_office_interview_fee",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_profit_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_profit_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_profit_costs_vat",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_cost_of_counsel_amount IS NOT NULL
                    THEN ROUND(calc.net_cost_of_counsel_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_counsel_costs_vat",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_travel_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_travel_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_travel_costs_vat",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_waiting_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_waiting_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_waiting_costs_vat",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.jr_form_filling_amount IS NOT NULL
                    THEN ROUND(calc.jr_form_filling_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_jr_form_filling_costs_vat",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.bolt_on_total_fee_amount IS NOT NULL
                    THEN ROUND(calc.bolt_on_total_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                            AS "current_bolt_on_fees_vat",
    COALESCE(cc.is_legacy_case::text, '')                                   AS "legacy_case_flag",
    COALESCE(csf.is_london_rate::text, '')                                  AS "london_rate_flag",
    COALESCE(csf.is_tolerance_applicable::text, '')                         AS "tolerance_indicator",
    COALESCE(csf.is_substantive_hearing::text, '')                          AS "substantive_hearing_flag",
    COALESCE(csf.is_additional_travel_payment::text, '')                    AS "additional_travel_payment_flag",
    COALESCE(csf.local_authority_number, '')                                AS "local_authority_number",
    COALESCE(c.procurement_area_code, '')                                   AS "procurement_area_code",
    COALESCE(c.access_point_code, '')                                       AS "access_point_code",
    COALESCE(c.referral_source, '')                                         AS "referral_source_code",
    COALESCE(csf.ait_hearing_centre_code, '')                               AS "ait_hearing_centre_code",
    COALESCE(cc.exceptional_case_funding_reference, '')                     AS "ecf_reference",
    COALESCE(cc.exemption_criteria_satisfied, '')                           AS "exemption_criteria_satisfied",
    COALESCE(cl.cla_reference_number, '')                                   AS "cla_reference_number",
    COALESCE(cl.cla_exemption_code, '')                                     AS "cla_exemption_code",
    COALESCE(csf.prior_authority_reference, '')                             AS "immigration_prior_authority_number",
    COALESCE(cc.is_postal_application_accepted::text, '')                   AS "postal_application_accepted",
    COALESCE(csf.is_irc_surgery::text, '')                                  AS "irc_surgery",
    COALESCE(csf.surgery_date::text, '')                                    AS "surgery_date",
    COALESCE(csf.surgery_clients_count::text, '')                           AS "number_of_clients_seen_at_the_surgery",
    COALESCE(csf.surgery_matters_count::text, '')                           AS "number_of_surgery_clients_resulting_in_a_legal_help_matter_opened",
    COALESCE(cc.is_nrm_advice::text, '')                                    AS "nrm_advice",
    COALESCE(cc.follow_on_work::text, '')                                   AS "prn_follow_on_work"
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
  AND bs.created_on >= (CURRENT_DATE - INTERVAL '3 years')
ORDER BY
    sp.submission_period_start NULLS LAST,
    sp.office_account_number,
    c.line_number;
