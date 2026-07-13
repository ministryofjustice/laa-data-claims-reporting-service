-- Bulk Submissions
INSERT INTO bulk_submission (
    id, status, error_code, error_description, created_by_user_id, created_on, updated_by_user_id, updated_on, authorised_offices
) VALUES (
             '11111111-1111-1111-1111-111111111111',
             'READY_FOR_PARSING',
             NULL,
             NULL,
             'test_user',
             '2025-11-21 05:00:00',
             NULL,
             NULL,
             'OfficeA,OfficeB'
         );

INSERT INTO bulk_submission (
    id, status, error_code, error_description, created_by_user_id, created_on, updated_by_user_id, updated_on, authorised_offices
) VALUES (
             '11111111-1111-1111-1111-111111111112',
             'READY_FOR_PARSING',
             NULL,
             NULL,
             'test_user',
             '2025-11-21 05:00:00',
             NULL,
             NULL,
             'OfficeA,OfficeB'
         );

-- Submission
INSERT INTO submission (
    id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
    previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
) VALUES (
             '22222222-2222-2222-2222-222222222222',
             '11111111-1111-1111-1111-111111111111',
             'OA001',
             'APR-2025',
             'CRIME_LOWER',
             'VALIDATION_SUCCEEDED',
             'CSN001',
             NULL,
             FALSE,
             3,
             NULL,
             'test_user',
             '2025-11-21 05:00:00',
          'test provider user'
         ),
      (
             '22222222-2222-2222-2222-222222222223',
             '11111111-1111-1111-1111-111111111112',
             'OA001',
             'MAY-2025',
             'CRIME_LOWER',
             'VALIDATION_SUCCEEDED',
             'CSN001',
             NULL,
             FALSE,
             1,
             NULL,
             'test_user',
             '2025-11-21 05:00:00',
             'test provider user'
         ),
      (
             '22222222-2222-2222-2222-222222222224',
             '11111111-1111-1111-1111-111111111112',
             'OA001',
             'MAY-2025',
             'LEGAL_HELP',
             'VALIDATION_SUCCEEDED',
             'CSN002',
             NULL,
             FALSE,
             1,
             NULL,
             'test_user',
             '2025-11-21 05:00:00',
             'test provider user'
         );

-- Matter Start
INSERT INTO matter_start (
    id, submission_id, schedule_reference, category_code, procurement_area_code,
    access_point_code, delivery_location, created_by_user_id, created_on,
    updated_by_user_id, updated_on, number_of_matter_starts
) VALUES (
             'aaaaaaa1-1111-1111-1111-111111111111',
             '22222222-2222-2222-2222-222222222222',
             'MSCH-001',
             'CAT-001',
             'PA-001',
             'AP-001',
             'DL-001',
             'test_user',
             '2025-11-21 05:00:00',
             NULL,
             NULL,
             17
         );

-- Claim
INSERT INTO claim (
    id, submission_id, status, line_number, matter_type_code, fee_code, created_by_user_id, dscc_number, created_on, updated_on
) VALUES (
             '33333333-3333-3333-3333-333333333333',
             '22222222-2222-2222-2222-222222222222',
             'VALID',
             1,
             'MT001',
             'FEE002',
             'test_user',
             NULL,
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
      (
             '33333333-3333-3333-3333-333333333334',
             '22222222-2222-2222-2222-222222222222',
             'VALID',
             1,
             'MT001',
             'FEE002',
             'test_user',
             'DSCC123456',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             NULL
         ),
      (
             '33333333-3333-3333-3333-333333333335',
             '22222222-2222-2222-2222-222222222224',
             'VALID',
             1,
             'MT001',
             'FEE003',
             'test_user',
             NULL,
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             NULL
         ),
         (
             '33333333-3333-3333-3333-333333333336',
             '22222222-2222-2222-2222-222222222224',
             'VOID',
             2,
             'MT001',
             'FEE004',
             'test_user',
             NULL,
             TIMESTAMP '2025-11-21 06:00:00' - interval '1 day',
             TIMESTAMP '2025-11-22 12:03:32'
         ),
         (
             '33333333-3333-3333-3333-333333333337',
             '22222222-2222-2222-2222-222222222222',
             'VALID',
             1,
             'MT001',
             'FEE002',
             'test_user',
             NULL,
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         );

-- Client
INSERT INTO client (
    id, claim_id, client_forename, client_surname, client_date_of_birth, unique_client_number, client_postcode,
    gender_code, ethnicity_code, disability_code, is_legally_aided, client_type_code, home_office_client_number,
    cla_reference_number, cla_exemption_code, created_by_user_id, created_on, updated_on
) VALUES (
             '44444444-4444-4444-4444-444444444444',
             '33333333-3333-3333-3333-333333333333',
             'John, Mr. S',
             'Doe',
             '1980-01-01',
             'UCN001',
             'AB12 3CD',
             'M',
             'White',
             NULL,
             TRUE,
             'Type1',
             'HO123',
             'CLA001',
             'EX001',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             NULL
       ),
      (
             '44444444-4444-4444-4444-444444444445',
             '33333333-3333-3333-3333-333333333334',
             'John',
             'Doe',
             '1980-01-01',
             'UCN001',
             'AB12 3CD',
             'M',
             'White',
             NULL,
             TRUE,
             'Type1',
             'HO123',
             'CLA001',
             'EX001',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
         (
             '44444444-4444-4444-4444-444444444446',
             '33333333-3333-3333-3333-333333333336',
             'Joan',
             'Deer',
             '1993-01-01',
             'UCN0S2',
             'AB3 1LF',
             'F',
             'White',
             NULL,
             TRUE,
             'Type2',
             'HO124',
             'CLA002',
             'EX001',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
         (
             '44444444-4444-4444-4444-44444444444a',
             '33333333-3333-3333-3333-333333333337',
             'John, Mr. S',
             'Doe',
             '1980-01-01',
             'UCN001',
             'AB12 3CD',
             'M',
             'White',
             NULL,
             TRUE,
             'Type1',
             'HO123',
             'CLA001',
             'EX001',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day',
             NULL
         );

-- Claim Case
INSERT INTO claim_case (
    id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code, mental_health_tribunal_reference, created_by_user_id, created_on
) VALUES (
             '55555555-5555-5555-5555-555555555555',
             '33333333-3333-3333-3333-333333333333',
             'CASE001',
             'UCASE001',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             null,
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
      (
             '55555555-5555-5555-5555-555555555556',
             '33333333-3333-3333-3333-333333333334',
             'CASE001',
             'UCASE001',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             'XKDL-3214-EXAMPLE',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
         (
             '55555555-5555-5555-5555-555555555557',
             '33333333-3333-3333-3333-333333333335',
             'CASE002',
             'UCASE002',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             null,
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
         (
             '55555555-5555-5555-5555-555555555558',
             '33333333-3333-3333-3333-333333333336',
             'CASE003',
             'UCASE003',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             null,
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
        (
             '55555555-5555-5555-5555-555555555559',
             '33333333-3333-3333-3333-333333333337',
             'CASE001',
             'UCASE001',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             'LKD-EXAMPLE2-1979',
             'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day');

-- Claim Summary Fee
INSERT INTO claim_summary_fee (
    id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
    net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
    is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on
) VALUES (
             '66666666-6666-6666-6666-666666666666',
             '33333333-3333-3333-3333-333333333333',
             60, 30, 15, 1000, 200,
             500, 100, 50, 20,
             TRUE, FALSE, 'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
      (
             '66666666-6666-6666-6666-666666666667',
             '33333333-3333-3333-3333-333333333334',
             60, 30, 15, 1000, 200,
          500, 100, 50, 20,
          TRUE, FALSE, 'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         ),
         (
             '66666666-6666-6666-6666-666666666668',
             '33333333-3333-3333-3333-333333333335',
             70, 40, 18, 1010, 202,
             503, 104, 55, 26,
             TRUE, FALSE, 'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', NULL
         ),
         (
             '66666666-6666-6666-6666-666666666669',
             '33333333-3333-3333-3333-333333333336',
             80, 50, 28, 1020, 212,
             513, 114, 65, 36,
             TRUE, FALSE, 'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', NULL
         ),
        (
             '66666666-6666-6666-6666-666666666670',
             '33333333-3333-3333-3333-333333333337',
             60, 30, 15, 1000, 200,
             500, 100, 50, 20,
             TRUE, FALSE, 'test_user',
             TIMESTAMP '2025-11-21 05:00:00' - interval '1 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'
         );

-- Calculated Fee Detail (4 rows)
INSERT INTO calculated_fee_detail (
    id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
    fee_code_description, category_of_law, total_amount
) VALUES
      ('77777777-7777-7777-7777-777777777777', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE001', 'TypeA', 'test_user', '2025-10-20 09:00:00+00', 'test_user', '2025-04-20 09:30:00+00',
       'Description 1', 'INVEST', 1502),
      ('88888888-8888-8888-8888-888888888888', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE002', 'TypeB', 'test_user', '2025-10-21 10:00:00+00', 'test_user', '2025-04-21 10:30:00+00',
       'Description 2', 'IMMAS', 2100),
      ('88888888-8888-8888-8888-888888888889', '66666666-6666-6666-6666-666666666667', '33333333-3333-3333-3333-333333333334',
       'FEE002', 'TypeB', 'test_user', '2025-10-21 10:00:00+00', 'test_user', '2025-05-21 10:30:00+00',
       'Description 2', 'AAP', 2000),
      ('99999999-9999-9999-9999-999999999999', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE003', 'TypeC', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-04-19 11:30:00+00',
       'Description 3', 'HOU', 2500),
      ('99999999-9999-9999-9999-99999999999a', '66666666-6666-6666-6666-666666666668', '33333333-3333-3333-3333-333333333334',
       'FEE003', 'TypeC', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-04-19 11:30:00+00',
       'Description 3', 'HOU', 3500),
      ('99999999-9999-9999-9999-99999999999b', '66666666-6666-6666-6666-666666666669', '33333333-3333-3333-3333-333333333336',
       'FEE004', 'TypeA', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-04-19 11:30:00+00',
       'Description 4', 'HOU', 4500),
       ('77777777-7777-7777-7777-77777777777a', '66666666-6666-6666-6666-666666666666',
        '33333333-3333-3333-3333-333333333337',
        'FEE001', 'TypeA', 'test_user', '2025-10-20 09:00:00+00', 'test_user', '2025-04-20 09:30:00+00',
        'Description 1', 'INVEST', 1502),
       ('88888888-8888-8888-8888-88888888888a', '66666666-6666-6666-6666-666666666666',
        '33333333-3333-3333-3333-333333333337',
        'FEE002', 'TypeB', 'test_user', '2025-10-21 10:00:00+00', 'test_user', '2025-04-21 10:30:00+00',
        'Description 2', 'IMMAS', 2100),
       ('99999999-9999-9999-9999-99999999999c', '66666666-6666-6666-6666-666666666666',
        '33333333-3333-3333-3333-333333333337',
        'FEE003', 'TypeC', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-04-19 11:30:00+00',
        'Description 3', 'HOU', 2500);


INSERT INTO claims.assessment (
  		id, claim_id, claim_summary_fee_id, assessment_outcome, fixed_fee_amount, net_travel_costs_amount,
  		net_waiting_costs_amount, net_profit_costs_amount, disbursement_amount, disbursement_vat_amount,
  		net_cost_of_counsel_amount, detention_travel_and_waiting_costs_amount, is_vat_applicable,
  		bolt_on_adjourned_hearing_fee, jr_form_filling_amount, bolt_on_cmrh_oral_fee, bolt_on_cmrh_telephone_fee,
  		bolt_on_substantive_hearing_fee, bolt_on_home_office_interview_fee, assessed_total_vat, assessed_total_incl_vat,
  		allowed_total_vat, allowed_total_incl_vat, assessment_type, assessment_reason, created_by_user_id, created_on, updated_by_user_id, updated_on
  		) VALUES
        ('12345555-7777-7777-7777-777777777778', '33333333-3333-3333-3333-333333333337', '66666666-6666-6666-6666-666666666666',
        'REDUCED_TO_FIXED_FEE', 33.23, 44.43, 43.44, 40.20, 12.0, 33.12, 12.33, 3.12, TRUE, 12.3, 45.6, 3.86, 0.90, 33.30, 44.4,
        33.12, 2.94, 3.33, 33.12, 'ESCAPE_FEE_ASSESSMENT', 'Provider request', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-04-19 11:30:00+00'),
       ('12345555-7777-7777-7777-777777777777', '33333333-3333-3333-3333-333333333337', '66666666-6666-6666-6666-666666666666',
        'PAID_IN_FULL', 233.33, 43.44, 44.43, 2.40, 0.12, 123.33, 33.12, 12.3, TRUE, 3.12, 6.45, 86.3, 9.00, 3.33, 4.44,
        12.33, 94.2, 33.3, 120.33, 'ESCAPE_FEE_ASSESSMENT', 'Escape Fee Case Assessment', 'test_user', '2025-10-22 11:01:00+00', 'test_user', '2025-04-19 11:30:00+00'),
       ('12345555-7777-7777-7777-777777777779', '33333333-3333-3333-3333-333333333336', '66666666-6666-6666-6666-666666666669',
        'NILLED', 0, 0, 0, 0, 0, 0, 0, 0, TRUE, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 'VOID', 'Provider request', 'test_user', '2025-10-23 12:34:04+00', 'test_user', '2025-04-19 11:30:00+00');
