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
    -- PROVIDER DATA
    ''                                                                      AS "Firm name", -- Enriched
    -- Firm number is not held in the Claims DS database. Users perform a lookup post-export.
    ''                                                                      AS "Firm number",
    ''                                                                      AS "Office name", -- Enriched
    COALESCE(c.procurement_area_code, '')                                   AS "Procurement Area Code",
    ''                                                                      AS "Procurement Area Description", -- Enriched
    COALESCE(c.access_point_code, '')                                       AS "Access Point Code",
    ''                                                                      AS "Access Point Description", -- Enriched
    COALESCE(c.delivery_location, '')                                       AS "Delivery Location",
    -- SUBMISSION DATA
    COALESCE(sp.id::text, '')                                               AS "Submission ID",
    COALESCE(TO_CHAR(sp.submission_period_start, 'MON-YYYY'), '')           AS "Submission Period",
    COALESCE(TO_CHAR(sp.submission_period_start, 'DD/MM/YYYY'), '')         AS "Submission For Date",
    COALESCE(sp.submission_status, '')                                      AS "Submission Status",
    COALESCE(crime_lower_schedule_number, '')                               AS "Crime Lower Submission Reference",
    COALESCE(legal_help_submission_reference, '')                           AS "Legal Help Submission Reference",
    COALESCE(mediation_submission_reference, '')                            AS "Mediation Submission Reference",
    COALESCE(c.schedule_reference, '')                                      AS "Schedule Reference",
    COALESCE(c.line_number::text, '')                                       AS "Line Number",
    COALESCE(TO_CHAR(DATE(bs.created_on), 'DD/MM/YYYY'), '')                AS "Date Submitted",
    COALESCE(sp.office_account_number, '')                                  AS "Office Account Number",
    -- CLIENT INFORMATION
    COALESCE(cl.client_forename, '')                                        AS "Client Forename",
    COALESCE(cl.client_surname, '')                                         AS "Client Surname",
    COALESCE(TO_CHAR(cl.client_date_of_birth, 'DD/MM/YYYY'), '')            AS "Client Date of Birth",
    COALESCE(cl.gender_code, '')                                            AS "Gender",
    COALESCE(cl.ethnicity_code, '')                                         AS "Ethnicity",
    COALESCE(cl.disability_code, '')                                        AS "Disability",
    COALESCE(cl.unique_client_number, '')                                   AS "Unique Client Number",
    COALESCE(cl.client_postcode, '')                                        AS "Client Postcode",
    COALESCE(CASE WHEN cl.is_legally_aided IS TRUE THEN 'Yes' WHEN cl.is_legally_aided IS FALSE THEN 'No' END, '') AS "First Client Legally Aided",
    COALESCE(cl.client_type_code, '')                                       AS "Client Type Code",
    COALESCE(cl.client_2_forename, '')                                      AS "Second Client Forename",
    COALESCE(cl.client_2_surname, '')                                       AS "Second Client Surname",
    COALESCE(TO_CHAR(cl.client_2_date_of_birth, 'DD/MM/YYYY'), '')          AS "Second Client Date of Birth",
    COALESCE(cl.client_2_gender_code, '')                                   AS "Second Client Gender",
    COALESCE(cl.client_2_ethnicity_code, '')                                AS "Second Client Ethnicity",
    COALESCE(cl.client_2_disability_code, '')                               AS "Second Client Disability",
    COALESCE(cl.client_2_ucn::text, '')                                     AS "Second Client Unique Client Number",
    COALESCE(cl.client_2_postcode, '')                                      AS "Second Client Postcode",
    COALESCE(CASE WHEN cl.client_2_is_legally_aided IS TRUE THEN 'Yes' WHEN cl.client_2_is_legally_aided IS FALSE THEN 'No' END, '') AS "Second Client Legally Aided",
    COALESCE(CASE WHEN cc.is_client_2_postal_application_accepted IS TRUE THEN 'Yes' WHEN cc.is_client_2_postal_application_accepted IS FALSE THEN 'No' END, '') AS "Second Client Postal Application Accepted",
    -- CASE DETAILS
    COALESCE(sp.area_of_law, '')                                            AS "Area of Law",
    -- Intentional: "Total Value" removed per stakeholder decision (25/09/2025)
    COALESCE(calc.category_of_law, '')                                      AS "Category of Law Code",
    COALESCE(c.id::text, '')                                                AS "Claim ID",
    COALESCE(c.case_reference_number, '')                                   AS "Case Reference Number",
    COALESCE(c.unique_file_number, '')                                      AS "Unique File Number",
    COALESCE(cc.case_id, '')                                                AS "Case ID",
    COALESCE(cc.unique_case_id, '')                                         AS "Unique Case ID",
    COALESCE(c.fee_code, '')                                                AS "Fee Code",
    COALESCE(calc.fee_code_description, '')                                 AS "Fee Code Description", -- Enriched
    COALESCE(cc.standard_fee_category_code, '')                             AS "Standard Fee Category Code",
    COALESCE(LEFT(c.matter_type_code, 4), '')                               AS "Matter Type 1",
    COALESCE(RIGHT(c.matter_type_code, 4), '')                              AS "Matter Type 2",
    COALESCE(c.matter_type_code, '')                                        AS "Matter Type Code",
    COALESCE(cc.case_stage_code, '')                                        AS "Case Stage Level",
    COALESCE(cc.stage_reached_code, '')                                     AS "Stage Reached",
    COALESCE(cc.outcome_code, '')                                           AS "Outcome Code",
    COALESCE(TO_CHAR(c.case_start_date, 'DD/MM/YYYY'), '')                  AS "Case Start Date",
    COALESCE(TO_CHAR(c.case_concluded_date, 'DD/MM/YYYY'), '')              AS "Case Concluded Date",
    COALESCE(TO_CHAR(cc.transfer_date, 'DD/MM/YYYY'), '')                   AS "Transfer Date",
    COALESCE(cc.exemption_criteria_satisfied, '')                           AS "Exemption Criteria Satisfied",
    COALESCE(cc.exceptional_case_funding_reference, '')                     AS "ECF Reference",
    COALESCE(CASE WHEN csf.is_tolerance_applicable IS TRUE THEN 'Yes' WHEN csf.is_tolerance_applicable IS FALSE THEN 'No' END, '') AS "Tolerance Indicator",
    COALESCE(c.referral_source, '')                                         AS "Referral Source Code",
    COALESCE(CASE WHEN csf.is_london_rate IS TRUE THEN 'Yes' WHEN csf.is_london_rate IS FALSE THEN 'No' END, '') AS "London Rate Flag",
    COALESCE(csf.local_authority_number, '')                                AS "Local Authority Number",
    COALESCE(cl.cla_reference_number, '')                                   AS "CLA Reference Number",
    COALESCE(cl.cla_exemption_code, '')                                     AS "CLA Exemption Code",
    CASE WHEN
             COALESCE(cc.exceptional_case_funding_reference, '') <> ''
             THEN 'Y'
         ELSE 'N'
        END                                                                 AS "Is Exceptional Claim",
    COALESCE(CASE WHEN cc.is_postal_application_accepted IS TRUE THEN 'Yes' WHEN cc.is_postal_application_accepted IS FALSE THEN 'No' END, '') AS "Postal Application Accepted",
    COALESCE(csf.advice_type_code, '')                                      AS "Type of Advice",
    COALESCE(CASE WHEN csf.is_eligible_client IS TRUE THEN 'Yes' WHEN csf.is_eligible_client IS FALSE THEN 'No' END, '') AS "Eligible Client",
    COALESCE(csf.court_location_code, '')                                   AS "Court Location (HPCDS)",
    -- IMMIGRATION
    COALESCE(cl.home_office_client_number, '')                              AS "Home Office Client Number",
    COALESCE(csf.prior_authority_reference, '')                             AS "Immigration Prior Authority Number",
    COALESCE(csf.ait_hearing_centre_code, '')                               AS "AIT Hearing Centre Code",
    COALESCE(CASE WHEN cc.is_legacy_case IS TRUE THEN 'Yes' WHEN cc.is_legacy_case IS FALSE THEN 'No' END, '') AS "Legacy Case Flag",
    COALESCE(CASE WHEN csf.is_irc_surgery IS TRUE THEN 'Yes' WHEN csf.is_irc_surgery IS FALSE THEN 'No' END, '') AS "IRC Surgery",
    COALESCE(csf.surgery_date::text, '')                                    AS "Surgery Date",
    COALESCE(csf.surgery_clients_count::text, '')                           AS "Number Of Clients Seen At The Surgery",
    COALESCE(csf.surgery_matters_count::text, '')                           AS "Surgery Clients Resulting in Legal Help Matters",
    COALESCE(CASE WHEN cc.is_nrm_advice IS TRUE THEN 'Yes' WHEN cc.is_nrm_advice IS FALSE THEN 'No' END, '') AS "NRM Advice",
    COALESCE(cc.follow_on_work::text, '')                                   AS "PRN Follow On Work",
    -- CRIME
    COALESCE(c.scheme_id, '')                                               AS "Scheme ID",
    COALESCE(c.police_station_court_prison_id, '')                          AS "Police Station Court Prison ID",
    COALESCE(CASE WHEN c.is_youth_court IS TRUE THEN 'Yes' WHEN c.is_youth_court IS FALSE THEN 'No' END, '') AS "Is Youth Court",
    COALESCE(c.police_station_court_attendances_count::text, '')            AS "Police Station Court Attendances Count",
    COALESCE(c.suspects_defendants_count::text, '')                         AS "Suspects Defendants Count",
    COALESCE(c.crime_matter_type_code, '')                                  AS "Crime Matter Type",
    COALESCE(TO_CHAR(c.representation_order_date, 'DD/MM/YYYY'), '')        AS "Representation Order Date",
    COALESCE(c.maat_id, '')                                                 AS "MAAT ID",
    COALESCE(CASE WHEN c.is_duty_solicitor IS TRUE THEN 'Yes' WHEN c.is_duty_solicitor IS FALSE THEN 'No' END, '') AS "Is Duty Solicitor",
    COALESCE(c.dscc_number, '')                                             AS "DSCC Number",
    COALESCE(c.prison_law_prior_approval_number, '')                        AS "Prison law Prior Approval number",
    COALESCE(c.outreach_location, '')                                       AS "Outreach Location",
    COALESCE(c.mediation_time_minutes::text, '')                            AS "Mediation Time",
    -- MEDIATION
    COALESCE(c.mediation_sessions_count::text, '')                          AS "Mediation Sessions Count",
    -- MENTAL HEALTH
    COALESCE(cc.mental_health_tribunal_reference::text, '')                 AS "Mental Health Tribunal Reference",
    COALESCE(csf.medical_reports_count::text, '')                           AS "Medical Reports Count",
    COALESCE(csf.meetings_attended_code, '')                                AS "Meetings Attended Code",
    COALESCE(cc.designated_accredited_representative_code, '')              AS "Designated Accredited Representative",
    -- TIME REPORTED
    COALESCE(csf.advice_time::text, '')                                     AS "Advice Time",
    COALESCE(csf.travel_time::text, '')                                     AS "Travel Time",
    COALESCE(csf.waiting_time::text, '')                                    AS "Waiting Time",
    -- REPORTED COSTS
    COALESCE(calc.fee_type, '')                                             AS "Fee Type",
    COALESCE(csf.net_profit_costs_amount::text, '')                         AS "Profit Costs",
    COALESCE(csf.net_counsel_costs_amount::text, '')                        AS "Counsel Fees",
    COALESCE(csf.net_disbursement_amount::text, '')                         AS "Disbursement Costs",
    COALESCE(csf.travel_waiting_costs_amount::text, '')                     AS "Travel Waiting Costs",
    COALESCE(calc.vat_rate_applied::text, '')                               AS "VAT Rate Applied",
    COALESCE(CASE WHEN csf.is_vat_applicable IS TRUE THEN 'Yes' WHEN csf.is_vat_applicable IS FALSE THEN 'No' END, '') AS "VAT Indicator",
    COALESCE(csf.jr_form_filling_amount::text, '')                          AS "JR Form Filling Costs",
    COALESCE(a.disbursement_amount::text, '')                               AS "Disbursement Amount",
    COALESCE(a.detention_travel_and_waiting_costs_amount::text, '')         AS "Detention Travel And Waiting Costs Amount",
    COALESCE(a.jr_form_filling_amount::text, '')                            AS "JR Form Filling Amount",
    COALESCE(csf.costs_damages_recovered_amount::text, '')                  AS "Cost / Damages Recovered",
    COALESCE(csf.detention_travel_waiting_costs_amount::text, '')           AS "Detention Travel & Waiting Costs",
    COALESCE(csf.adjourned_hearing_fee_amount::text, '')                    AS "Adjourned Hearing Fee Count",
    COALESCE(csf.cmrh_oral_count::text, '')                                 AS "CMRH Oral Count",
    COALESCE(csf.cmrh_telephone_count::text, '')                            AS "CMRH Telephone Count",
    COALESCE(csf.ho_interview::text, '')                                    AS "HO Interview Count",
    COALESCE(CASE WHEN csf.is_substantive_hearing IS TRUE THEN 'Yes' WHEN csf.is_substantive_hearing IS FALSE THEN 'No' END, '') AS "Substantive Hearing Flag",
    COALESCE(CASE WHEN csf.is_additional_travel_payment IS TRUE THEN 'Yes' WHEN csf.is_additional_travel_payment IS FALSE THEN 'No' END, '') AS "Additional Travel Payment Flag",
    -- SABC INITIAL COSTS
    COALESCE(calc.bolt_on_adjourned_hearing_count::text, '')                AS "Current Bolt On Adjourned Hearing Count",
    COALESCE(calc.bolt_on_adjourned_hearing_fee::text, '')                  AS "Current Bolt On Adjourned Hearing Fee",
    COALESCE(calc.bolt_on_cmrh_telephone_count::text, '')                   AS "Current Bolt On CMRH Telephone Count",
    COALESCE(calc.bolt_on_cmrh_telephone_fee::text, '')                     AS "Current Bolt On CMRH Telephone Fee",
    COALESCE(calc.bolt_on_cmrh_oral_count::text, '')                        AS "Current Bolt On CMRH Oral Count",
    COALESCE(calc.bolt_on_cmrh_oral_fee::text, '')                          AS "Current Bolt On CMRH Oral Fee",
    COALESCE(calc.bolt_on_total_fee_amount::text, '')                       AS "Current Bolt On Total Fee Amount",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.bolt_on_total_fee_amount IS NOT NULL
                    THEN ROUND(calc.bolt_on_total_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Bolt On Fees VAT",
    COALESCE(calc.bolt_on_home_office_interview_fee::text, '')              AS "Current Bolt On Home Office Interview Fee",
    COALESCE(calc.bolt_on_home_office_interview_count::text, '')            AS "Current Bolt On HO Interviews count",
    COALESCE(calc.fixed_fee_amount::text, '')                               AS "Current Fixed Fee Amount",
    COALESCE(calc.hourly_total_amount::text, '')                            AS "Current Hourly Total Amount",
    COALESCE(calc.net_profit_costs_amount::text, '')                        AS "Current Net Profit Costs Amount",
    COALESCE(calc.net_cost_of_counsel_amount::text, '')                     AS "Current Net Cost Of Counsel Amount",
    COALESCE(calc.disbursement_amount::text, '')                            AS "Current Disbursement Amount",
    COALESCE(csf.disbursements_vat_amount::text, '')                        AS "Disbursement VAT Costs",
    COALESCE(calc.travel_and_waiting_costs_amount::text, '')                AS "Current Travel And Waiting Costs Amount",
    COALESCE(calc.detention_travel_and_waiting_costs_amount::text, '')      AS "Current Detention And Waiting Costs Amount",
    COALESCE(calc.jr_form_filling_amount::text, '')                         AS "Current JR Form Filling Amount",
    COALESCE(CASE WHEN calc.vat_indicator IS TRUE THEN 'Yes' WHEN calc.vat_indicator IS FALSE THEN 'No' END, '') AS "Current VAT Indicator",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.fixed_fee_amount IS NOT NULL
                    THEN ROUND(calc.fixed_fee_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Fixed Fee VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_profit_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_profit_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Profit Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_cost_of_counsel_amount IS NOT NULL
                    THEN ROUND(calc.net_cost_of_counsel_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Counsel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_travel_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_travel_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Travel Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.net_waiting_costs_amount IS NOT NULL
                    THEN ROUND(calc.net_waiting_costs_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current Waiting Costs VAT",
    COALESCE(
            CASE
                WHEN calc.vat_rate_applied IS NOT NULL AND calc.jr_form_filling_amount IS NOT NULL
                    THEN ROUND(calc.jr_form_filling_amount * calc.vat_rate_applied / 100, 2)::text
                ELSE NULL
                END, '')                                                    AS "Current JR / Form Filling Costs VAT",
    -- SABC AMENDMENT COSTS
    COALESCE(a.fixed_fee_amount::text, '')                                  AS "Fixed Fee Amount",
    COALESCE(a.net_profit_costs_amount::text, '')                           AS "Net Profit Costs Amount",
    COALESCE(a.net_cost_of_counsel_amount::text, '')                        AS "Net Cost Of Counsel Amount",
    COALESCE(a.net_travel_costs_amount::text, '')                           AS "Net Travel Costs Amount",
    COALESCE(csf.net_waiting_costs_amount::text, '')                        AS "Net Waiting Costs Amount",
    COALESCE(a.bolt_on_home_office_interview_fee::text, '')                 AS "Bolt On Home Office Interview Fee",
    COALESCE(a.bolt_on_adjourned_hearing_fee::text, '')                     AS "Bolt On Adjourned Hearing Fee",
    COALESCE(a.bolt_on_cmrh_telephone_fee::text, '')                        AS "Bolt On CMRH Telephone Fee",
    COALESCE(a.bolt_on_cmrh_oral_fee::text, '')                             AS "Bolt On CMRH Oral Fee",
    COALESCE(a.bolt_on_substantive_hearing_fee::text, '')                   AS "Bolt On Substantive Hearing Fee",
    -- SABC SYSTEM FLAGS
    COALESCE(CASE WHEN calc.escape_case_flag IS TRUE THEN 'Yes' WHEN calc.escape_case_flag IS FALSE THEN 'No' END, '') AS "Current Escape Case Flag",
    COALESCE
    (CASE WHEN c.matched_claim_id IS NOT NULL
              THEN 'Y'
          ELSE 'N' END, 'N'
    )                                                                       AS "Is Duplicate Claim",
    COALESCE(CASE WHEN c.is_amended IS TRUE THEN 'Yes' WHEN c.is_amended IS FALSE THEN 'No' END, '') AS "Amended Flag",
    -- this currently isn't populated by the source system
    COALESCE(
            CASE WHEN cc.stage_reached_code = 'VOID' THEN 'Y' ELSE 'N' END, 'N'
    )                                                                       AS "Is Void",
    ''                                                                      AS "Has Post Submission Change",
    COALESCE(CASE WHEN c.has_assessment IS TRUE THEN 'Yes' WHEN c.has_assessment IS FALSE THEN 'No' END, '') AS "Assessed Flag",
    -- SABC AMENDMENT COSTS
    COALESCE(a.created_by_user_id, '')                                      AS "Assessed By User ID",
    COALESCE(a.updated_by_user_id, '')                                      AS "Assessment Updated By User ID",
    COALESCE(a.created_on::text, '')                                        AS "Assessment Date Time",
    COALESCE(a.updated_on::text, '')                                        AS "Assessment Update Date Time",
    COALESCE(a.net_waiting_costs_amount::text, '')                          AS "Assessed Net Waiting Costs Amount",
    -- SABC TOTAL COSTING
    COALESCE(
            CASE
                WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                ELSE NULL
                END, '')                                                    AS "Initial Calculated Claim Value",
    COALESCE(a.allowed_total_vat::text, '')                                 AS "Allowed Total VAT",
    COALESCE(a.allowed_total_incl_vat::text, '')                            AS "Allowed Total Inc VAT",
    COALESCE(a.assessed_total_vat::text, '')                                AS "Assessed Total VAT",
    COALESCE(a.assessed_total_incl_vat::text, '')                           AS "Assessed Total Inc VAT",
    CASE
        WHEN COALESCE(a.allowed_total_incl_vat::text, '') <> '' THEN COALESCE(a.allowed_total_incl_vat::text, '')
        ELSE COALESCE(
                CASE
                    WHEN calc.total_amount::text ~ '^[\s+-]?\d+(\.\d+)?$' THEN ROUND(calc.total_amount, 2)::text
                    ELSE NULL
                    END, '')
        END                                                                 AS "Final Claim Value",
    claims.convert_string_to_title_case(COALESCE(c.status::text, ''))       AS "Claim Status"
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
  AND c.status IN ('VALID', 'VOID')
  AND bs.created_on >= (CURRENT_DATE - INTERVAL '3 years')