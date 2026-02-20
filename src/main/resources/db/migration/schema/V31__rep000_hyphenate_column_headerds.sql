DROP MATERIALIZED VIEW IF EXISTS mvw_report_000;

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
)
SELECT
    -- SUBMISSION DATA
    COALESCE(sp.id::text, '')                                               AS "Submission data - Submission ID",
    COALESCE(TO_CHAR(sp.submission_period_start, 'MON-YYYY'), '')           AS "Submission data - Submission Period",
    COALESCE(TO_CHAR(sp.submission_period_start, 'DD/MM/YYYY'), '')         AS "Submission data - Submission For Date",
    COALESCE(sp.submission_status, '')                                      AS "Submission data - Submission Status",
    COALESCE(crime_lower_schedule_number, '')                               AS "Submission data - Crime Lower Submission Reference",
    COALESCE(legal_help_submission_reference, '')                           AS "Submission data - Legal Help Submission Reference",
    COALESCE(mediation_submission_reference, '')                            AS "Submission data - Mediation Submission Reference",
    COALESCE(c.line_number::text, '')                                       AS "Submission data - Line Number",
    COALESCE(TO_CHAR(DATE(bs.created_on), 'DD/MM/YYYY'), '')                AS "Submission data - Date Submitted",
    -- PROVIDER DATA
    COALESCE(sp.office_account_number, '')                                  AS "Provider Data - Office Account Number",
    COALESCE(c.procurement_area_code, '')                                   AS "Provider Data - Procurement Area Code",
    COALESCE(c.access_point_code, '')                                       AS "Provider Data - Access Point Code",
    COALESCE(c.delivery_location, '')                                       AS "Provider Data - Delivery Location",
    -- CLIENT INFORMATION
    COALESCE(cl.client_forename, '')                                        AS "Client details - Client Forename",
    COALESCE(cl.client_surname, '')                                         AS "Client details - Client Surname",
    COALESCE(TO_CHAR(cl.client_date_of_birth, 'DD/MM/YYYY'), '')            AS "Client details - Client Date of Birth",
    COALESCE(cl.gender_code, '')                                            AS "Client details - Gender",
    COALESCE(cl.ethnicity_code, '')                                         AS "Client details - Ethnicity",
    COALESCE(cl.disability_code, '')                                        AS "Client details - Disability",
    COALESCE(cl.unique_client_number, '')                                   AS "Client details - Unique Client Number",
    COALESCE(cl.client_postcode, '')                                        AS "Client details - Client Postcode",
    COALESCE(CASE WHEN cl.is_legally_aided IS TRUE THEN 'Yes' WHEN cl.is_legally_aided IS FALSE THEN 'No' END, '') AS "Client details - First Client Legally Aided",
    COALESCE(cl.client_type_code, '')                                       AS "Client details - Client Type Code",
    COALESCE(cl.client_2_forename, '')                                      AS "Client details - Second Client Forename",
    COALESCE(cl.client_2_surname, '')                                       AS "Client details - Second Client Surname",
    COALESCE(TO_CHAR(cl.client_2_date_of_birth, 'DD/MM/YYYY'), '')          AS "Client details - Second Client Date of Birth",
    COALESCE(cl.client_2_gender_code, '')                                   AS "Client details - Second Client Gender",
    COALESCE(cl.client_2_ethnicity_code, '')                                AS "Client details - Second Client Ethnicity",
    COALESCE(cl.client_2_disability_code, '')                               AS "Client details - Second Client Disability",
    COALESCE(cl.client_2_ucn::text, '')                                     AS "Client details - Second Client Unique Client Number",
    COALESCE(cl.client_2_postcode, '')                                      AS "Client details - Second Client Postcode",
    COALESCE(CASE WHEN cl.client_2_is_legally_aided IS TRUE THEN 'Yes' WHEN cl.client_2_is_legally_aided IS FALSE THEN 'No' END, '') AS "Client details - Second Client Legally Aided",
    -- CASE DETAILS
    COALESCE(sp.area_of_law, '')                                            AS "Case Details - Area of Law",
    -- Intentional: "Total Value" removed per stakeholder decision (25/09/2025)
    COALESCE(calc.category_of_law, '')                                      AS "Case Details - Category of Law Code",
    COALESCE(c.id::text, '')                                                AS "Case Details - Claim ID",
    COALESCE(c.case_reference_number, '')                                   AS "Case Details - Case Reference Number",
    COALESCE(c.unique_file_number, '')                                      AS "Case Details - Unique File Number",
    COALESCE(cc.case_id, '')                                                AS "Case Details - Case ID",
    COALESCE(cc.unique_case_id, '')                                         AS "Case Details - Unique Case ID",
    COALESCE(c.fee_code, '')                                                AS "Case Details - Fee Code",
    COALESCE(calc.fee_code_description, '')                                 AS "Case Details - Fee Code Description",
    COALESCE(cc.standard_fee_category_code, '')                             AS "Case Details - Standard Fee Category Code",
    COALESCE(LEFT(c.matter_type_code, 4), '')                               AS "Case Details - Matter Type 1",
    COALESCE(RIGHT(c.matter_type_code, 4), '')                              AS "Case Details - Matter Type 2",
    COALESCE(c.matter_type_code, '')                                        AS "Case Details - Matter Type Code",
    COALESCE(cc.case_stage_code, '')                                        AS "Case Details - Case Stage Level",
    COALESCE(cc.stage_reached_code, '')                                     AS "Case Details - Stage Reached",
    COALESCE(cc.outcome_code, '')                                           AS "Case Details - Outcome Code",
    COALESCE(TO_CHAR(c.case_start_date, 'DD/MM/YYYY'), '')                  AS "Case Details - Case Start Date",
    COALESCE(TO_CHAR(c.case_concluded_date, 'DD/MM/YYYY'), '')              AS "Case Details - Case Concluded Date",
    COALESCE(TO_CHAR(cc.transfer_date, 'DD/MM/YYYY'), '')                   AS "Case Details - Transfer Date",
    COALESCE(cc.exemption_criteria_satisfied, '')                           AS "Case Details - Exemption Criteria Satisfied",
    COALESCE(cc.exceptional_case_funding_reference, '')                     AS "Case Details - ECF Reference",
    COALESCE(CASE WHEN csf.is_tolerance_applicable IS TRUE THEN 'Yes' WHEN csf.is_tolerance_applicable IS FALSE THEN 'No' END, '') AS "Case Details - Tolerance Indicator",
    COALESCE(c.referral_source, '')                                         AS "Case Details - Referral Source Code",
    COALESCE(CASE WHEN csf.is_london_rate IS TRUE THEN 'Yes' WHEN csf.is_london_rate IS FALSE THEN 'No' END, '') AS "Case Details - London Rate Flag",
    COALESCE(csf.local_authority_number, '')                                AS "Case Details - Local Authority Number",
    COALESCE(cl.cla_reference_number, '')                                   AS "Case Details - CLA Reference Number",
    COALESCE(cl.cla_exemption_code, '')                                     AS "Case Details - CLA Exemption Code",
    CASE WHEN
             COALESCE(cc.exceptional_case_funding_reference, '') <> ''
             THEN 'Y'
         ELSE 'N'
        END                                                                 AS "Case Details - Is Exceptional Claim",
    COALESCE(CASE WHEN cc.is_postal_application_accepted IS TRUE THEN 'Yes' WHEN cc.is_postal_application_accepted IS FALSE THEN 'No' END, '') AS "Case Details - Postal Application Accepted",
    -- IMMIGRATION
    COALESCE(cl.home_office_client_number, '')                              AS "Immigration - Home Office Client Number",
    COALESCE(csf.prior_authority_reference, '')                             AS "Immigration - Immigration Prior Authority Number",
    COALESCE(csf.ait_hearing_centre_code, '')                               AS "Immigration - AIT Hearing Centre Code",
    COALESCE(CASE WHEN cc.is_legacy_case IS TRUE THEN 'Yes' WHEN cc.is_legacy_case IS FALSE THEN 'No' END, '') AS "Immigration - Legacy Case Flag",
    COALESCE(CASE WHEN csf.is_irc_surgery IS TRUE THEN 'Yes' WHEN csf.is_irc_surgery IS FALSE THEN 'No' END, '') AS "Immigration - IRC Surgery",
    COALESCE(csf.surgery_date::text, '')                                     AS "Immigration - Surgery Date",
    COALESCE(csf.surgery_clients_count::text, '')                            AS "Immigration - Number Of Clients Seen At The Surgery",
    COALESCE(csf.surgery_matters_count::text, '')                            AS "Immigration - Surgery Clients Resulting in Legal Help Matters",
    COALESCE(CASE WHEN cc.is_nrm_advice IS TRUE THEN 'Yes' WHEN cc.is_nrm_advice IS FALSE THEN 'No' END, '') AS "Immigration - NRM Advice",
    COALESCE(cc.follow_on_work::text, '')                                    AS "Immigration - PRN Follow On Work",
    -- CRIME
    COALESCE(c.scheme_id, '')                                               AS "Crime - Scheme ID",
    COALESCE(c.police_station_court_prison_id, '')                          AS "Crime - Police Station Court Prison ID",
    COALESCE(CASE WHEN c.is_youth_court IS TRUE THEN 'Yes' WHEN c.is_youth_court IS FALSE THEN 'No' END, '') AS "Crime - Is Youth Court",
    COALESCE(c.police_station_court_attendances_count::text, '')            AS "Crime - Police Station Court Attendances Count",
    COALESCE(c.suspects_defendants_count::text, '')                         AS "Crime - Suspects Defendants Count",
    COALESCE(TO_CHAR(c.representation_order_date, 'DD/MM/YYYY'), '')        AS "Crime - Representation Order Date",
    COALESCE(c.maat_id, '')                                                 AS "Crime - MAAT ID",
    COALESCE(CASE WHEN c.is_duty_solicitor IS TRUE THEN 'Yes' WHEN c.is_duty_solicitor IS FALSE THEN 'No' END, '') AS "Crime - Is Duty Solicitor",
    -- MEDIATION
    COALESCE(c.mediation_sessions_count::text, '')                          AS "Mediation - Mediation Sessions Count",
    -- MENTAL HEALTH
    COALESCE(csf.medical_reports_count::text, '')                           AS "Mental Health - Medical Reports Count",
    COALESCE(csf.meetings_attended_code, '')                                AS "Mental Health - Meetings Attended Code",
    -- TIME REPORTED
    COALESCE(csf.advice_time::text, '')                                     AS "Time reported - Advice Time",
    COALESCE(csf.travel_time::text, '')                                     AS "Time reported - Travel Time",
    COALESCE(csf.waiting_time::text, '')                                    AS "Time reported - Waiting Time",
    -- REPORTED COSTS
    COALESCE(calc.fee_type, '')                                             AS "Reported costs - Fee Type",
    COALESCE(csf.net_profit_costs_amount::text, '')                         AS "Reported costs - Profit Costs",
    COALESCE(csf.net_counsel_costs_amount::text, '')                        AS "Reported costs - Counsel Fees",
    COALESCE(csf.net_disbursement_amount::text, '')                         AS "Reported costs - Disbursement Costs",
    COALESCE(csf.travel_waiting_costs_amount::text, '')                     AS "Reported costs - Travel Waiting Costs",
    COALESCE(calc.vat_rate_applied::text, '')                               AS "Reported costs - VAT Rate Applied",
    COALESCE(CASE WHEN csf.is_vat_applicable IS TRUE THEN 'Yes' WHEN csf.is_vat_applicable IS FALSE THEN 'No' END, '') AS "Reported costs - VAT Indicator",
    COALESCE(csf.jr_form_filling_amount::text, '')                          AS "Reported costs - JR Form Filling Costs",
    COALESCE(a.disbursement_amount::text, '')                               AS "Reported costs - Disbursement Amount",
    COALESCE(a.detention_travel_and_waiting_costs_amount::text, '')         AS "Reported costs - Detention Travel And Waiting Costs Amount",
    COALESCE(a.jr_form_filling_amount::text, '')                            AS "Reported costs - JR Form Filling Amount",
    COALESCE(csf.costs_damages_recovered_amount::text, '')                  AS "Reported costs - Cost / Damages Recovered",
    COALESCE(csf.detention_travel_waiting_costs_amount::text, '')           AS "Reported costs - Detention Travel & Waiting Costs",
    COALESCE(csf.adjourned_hearing_fee_amount::text, '')                    AS "Reported costs - Adjourned Hearing Fee Count",
    COALESCE(csf.cmrh_oral_count::text, '')                                 AS "Reported costs - CMRH Oral Count",
    COALESCE(csf.cmrh_telephone_count::text, '')                            AS "Reported costs - CMRH Telephone Count",
    COALESCE(csf.ho_interview::text, '')                                    AS "Reported costs - HO Interview Count",
    COALESCE(CASE WHEN csf.is_substantive_hearing IS TRUE THEN 'Yes' WHEN csf.is_substantive_hearing IS FALSE THEN 'No' END, '') AS "Reported costs - Substantive Hearing Flag",
    COALESCE(CASE WHEN csf.is_additional_travel_payment IS TRUE THEN 'Yes' WHEN csf.is_additional_travel_payment IS FALSE THEN 'No' END, '') AS "Reported costs - Additional Travel Payment Flag",
    -- SABC INITIAL COSTS
    COALESCE(calc.bolt_on_adjourned_hearing_count::text, '')                AS "SaBC Initial Costs - Current Bolt On Adjourned Hearing Count",
    COALESCE(calc.bolt_on_adjourned_hearing_fee::text, '')                  AS "SaBC Initial Costs - Current Bolt On Adjourned Hearing Fee",
    COALESCE(calc.bolt_on_cmrh_telephone_count::text, '')                   AS "SaBC Initial Costs - Current Bolt On CMRH Telephone Count",
    COALESCE(calc.bolt_on_cmrh_telephone_fee::text, '')                     AS "SaBC Initial Costs - Current Bolt On CMRH Telephone Fee",
    COALESCE(calc.bolt_on_cmrh_oral_count::text, '')                        AS "SaBC Initial Costs - Current Bolt On CMRH Oral Count",
    COALESCE(calc.bolt_on_cmrh_oral_fee::text, '')                          AS "SaBC Initial Costs - Current Bolt On CMRH Oral Fee",
    COALESCE(calc.bolt_on_total_fee_amount::text, '')                       AS "SaBC Initial Costs - Current Bolt On Total Fee Amount",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.bolt_on_total_fee_amount IS NOT NULL
                    THEN ROUND(calc.bolt_on_total_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Bolt On Fees VAT",
    COALESCE(calc.bolt_on_home_office_interview_fee::text, '')              AS "SaBC Initial Costs - Current Bolt On Home Office Interview Fee",
    COALESCE(calc.bolt_on_home_office_interview_count::text, '')            AS "SaBC Initial Costs - Current Bolt On HO Interviews count",
    COALESCE(calc.fixed_fee_amount::text, '')                               AS "SaBC Initial Costs - Current Fixed Fee Amount",
    COALESCE(calc.hourly_total_amount::text, '')                            AS "SaBC Initial Costs - Current Hourly Total Amount",
    COALESCE(calc.net_profit_costs_amount::text, '')                        AS "SaBC Initial Costs - Current Net Profit Costs Amount",
    COALESCE(calc.net_cost_of_counsel_amount::text, '')                     AS "SaBC Initial Costs - Current Net Cost Of Counsel Amount",
    COALESCE(calc.disbursement_amount::text, '')                            AS "SaBC Initial Costs - Current Disbursement Amount",
    COALESCE(csf.disbursements_vat_amount::text, '')                        AS "SaBC Initial Costs - Disbursement VAT Costs",
    COALESCE(calc.travel_and_waiting_costs_amount::text, '')                AS "SaBC Initial Costs - Current Travel And Waiting Costs Amount",
    COALESCE(calc.detention_travel_and_waiting_costs_amount::text, '')      AS "SaBC Initial Costs - Current Detention And Waiting Costs Amount",
    COALESCE(calc.jr_form_filling_amount::text, '')                         AS "SaBC Initial Costs - Current JR Form Filling Amount",
    COALESCE(CASE WHEN calc.vat_indicator IS TRUE THEN 'Yes' WHEN calc.vat_indicator IS FALSE THEN 'No' END, '') AS "SaBC Initial Costs - Current VAT Indicator",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.fixed_fee_amount IS NOT NULL
                    THEN ROUND(calc.fixed_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Fixed Fee VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_profit_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_profit_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Profit Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_cost_of_counsel_amount IS NOT NULL
                    THEN ROUND(calc.net_cost_of_counsel_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Counsel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_travel_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_travel_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Travel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_waiting_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_waiting_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current Waiting Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.jr_form_filling_amount IS NOT NULL
                    THEN ROUND(calc.jr_form_filling_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Initial Costs - Current JR / Form Filling Costs VAT",
    -- SABC AMENDMENT COSTS
    COALESCE(a.fixed_fee_amount::text, '')                                  AS "SaBC Adjusted Costs - Fixed Fee Amount",
    COALESCE(a.net_profit_costs_amount::text, '')                           AS "SaBC Adjusted Costs - Net Profit Costs Amount",
    COALESCE(a.net_cost_of_counsel_amount::text, '')                        AS "SaBC Adjusted Costs - Net Cost Of Counsel Amount",
    COALESCE(a.net_travel_costs_amount::text, '')                           AS "SaBC Adjusted Costs - Net Travel Costs Amount",
    COALESCE(csf.net_waiting_costs_amount::text, '')                        AS "SaBC Adjusted Costs - Net Waiting Costs Amount",
    COALESCE(a.bolt_on_home_office_interview_fee::text, '')                 AS "SaBC Adjusted Costs - Bolt On Home Office Interview Fee",
    COALESCE(a.bolt_on_adjourned_hearing_fee::text, '')                     AS "SaBC Adjusted Costs - Bolt On Adjourned Hearing Fee",
    COALESCE(a.bolt_on_cmrh_telephone_fee::text, '')                        AS "SaBC Adjusted Costs - Bolt On CMRH Telephone Fee",
    COALESCE(a.bolt_on_cmrh_oral_fee::text, '')                             AS "SaBC Adjusted Costs - Bolt On CMRH Oral Fee",
    COALESCE(a.bolt_on_substantive_hearing_fee::text, '')                   AS "SaBC Adjusted Costs - Bolt On Substantive Hearing Fee",
    -- SABC SYSTEM FLAGS
    COALESCE(CASE WHEN calc.escape_case_flag IS TRUE THEN 'Yes' WHEN calc.escape_case_flag IS FALSE THEN 'No' END, '') AS "SaBC System Flags - Current Escape Case Flag",
    COALESCE
    (CASE WHEN c.matched_claim_id IS NOT NULL
              THEN 'Y'
          ELSE 'N' END, 'N'
    )                                                                       AS "SaBC System Flags - Is Duplicate Claim",
    COALESCE(CASE WHEN c.is_amended IS TRUE THEN 'Yes' WHEN c.is_amended IS FALSE THEN 'No' END, '') AS "SaBC System Flags - Amended Flag",
    -- will be populated after FSP adds this field
    COALESCE(
            CASE WHEN cc.stage_reached_code = 'VOID' THEN 'Y' ELSE 'N' END, 'N'
    )                                                                       AS "SaBC System Flags - Is Void",
    ''                                                                      AS "SaBC System Flags - Has Post Submission Change",
    COALESCE(CASE WHEN c.has_assessment IS TRUE THEN 'Yes' WHEN c.has_assessment IS FALSE THEN 'No' END, '') AS "SaBC System Flags - Assessed Flag",
    -- SABC AMENDMENT COSTS
    COALESCE(a.created_by_user_id, '')                                      AS "SaBC Adjusted Costs - Assessed By User ID",
    COALESCE(a.updated_by_user_id, '')                                      AS "SaBC Adjusted Costs - Assessment Updated By User ID",
    COALESCE(a.created_on::text, '')                                        AS "SaBC Adjusted Costs - Assessment Date Time",
    COALESCE(a.updated_on::text, '')                                        AS "SaBC Adjusted Costs - Assessment Update Date Time",
    COALESCE(a.net_waiting_costs_amount::text, '')                          AS "SaBC Adjusted Costs - Assessed Net Waiting Costs Amount",
    -- SABC TOTAL COSTING
    COALESCE(
            CASE
                WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                ELSE NULL
                END, '')                                                    AS "SaBC Total Costing information - Initial Calculated Claim Value",
    COALESCE(a.allowed_total_vat::text, '')                                 AS "SaBC Total Costing information - Allowed Total VAT",
    COALESCE(a.allowed_total_incl_vat::text, '')                            AS "SaBC Total Costing information - Allowed Total Inc VAT",
    COALESCE(a.assessed_total_vat::text, '')                                AS "SaBC Total Costing information - Assessed Total VAT",
    COALESCE(a.assessed_total_incl_vat::text, '')                           AS "SaBC Total Costing information - Assessed Total Inc VAT",
    CASE
        WHEN COALESCE(a.allowed_total_incl_vat::text, '') <> '' THEN COALESCE(a.allowed_total_incl_vat::text, '')
        ELSE COALESCE(
                CASE
                    WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                    ELSE NULL
                    END, '')
        END                                                                 AS "SaBC Total Costing information - Final Claim Value"
FROM submission_periods AS sp
         JOIN claims.bulk_submission AS bs
              ON bs.id = sp.bulk_submission_id
         JOIN claims.claim AS c
              ON c.submission_id = sp.id
         JOIN claims.claim_case AS cc
              ON cc.claim_id = c.id
    -- LATERAL join for latest assessment (performance optimization)
         LEFT JOIN LATERAL (
    SELECT *
    FROM claims.assessment a_inner
    WHERE a_inner.claim_id = c.id
    ORDER BY a_inner.created_on DESC NULLS LAST
        LIMIT 1
         ) a ON TRUE
    LEFT JOIN claims.claim_summary_fee AS csf
    ON csf.claim_id = c.id
    -- LATERAL join for latest calculated fee detail (performance optimization)
    LEFT JOIN LATERAL (
    SELECT *
    FROM claims.calculated_fee_detail cfd_inner
    WHERE cfd_inner.claim_id = c.id
    ORDER BY COALESCE(cfd_inner.updated_on, cfd_inner.created_on) DESC NULLS LAST
    LIMIT 1
    ) calc ON TRUE
    LEFT JOIN claims.client AS cl
    ON cl.claim_id = c.id
WHERE sp.submission_status = 'VALIDATION_SUCCEEDED'
  AND c.status = 'VALID'
  AND bs.created_on >= (CURRENT_DATE - INTERVAL '3 years')
ORDER BY
    sp.submission_period_start NULLS LAST,
    sp.office_account_number,
    c.line_number;
