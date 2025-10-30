-- Bulk Submissions
INSERT INTO bulk_submission (
    id, status, error_code, error_description, created_by_user_id, created_on, updated_by_user_id, updated_on, authorised_offices
) VALUES (
             '11111111-1111-1111-1111-111111111111',
             'READY_FOR_PARSING',
             NULL,
             NULL,
             'test_user',
             NOW(),
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
             NOW(),
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
             '2025-04',
             'Crime',
             'VALIDATION_SUCCEEDED',
             'CSN001',
             NULL,
             FALSE,
             1,
             NULL,
             'test_user',
             NOW(),
          'test provider user'
         );

-- Submission with no claims
INSERT INTO submission (
    id, bulk_submission_id, office_account_number, submission_period, area_of_law, status, crime_lower_schedule_number,
    previous_submission_id, is_nil_submission, number_of_claims, error_messages, created_by_user_id, created_on, provider_user_id
) VALUES (
             '22222222-2222-2222-2222-222222222223',
             '11111111-1111-1111-1111-111111111112',
             'OA001',
             '2025-04',
             'Crime',
             'VALIDATION_SUCCEEDED',
             'CSN001',
             NULL,
             FALSE,
             1,
             NULL,
             'test_user',
             NOW(),
             'test provider user'
         );

-- Claim
INSERT INTO claim (
    id, submission_id, status, line_number, matter_type_code, created_by_user_id, created_on, updated_on
) VALUES (
             '33333333-3333-3333-3333-333333333333',
             '22222222-2222-2222-2222-222222222222',
             'VALID',
             1,
             'MT001',
             'test_user',
             now() - interval '1 day',
             now() - interval '1 day'
         );

INSERT INTO claim (
    id, submission_id, status, line_number, matter_type_code, created_by_user_id, created_on
) VALUES (
             '33333333-3333-3333-3333-333333333334',
             '22222222-2222-2222-2222-222222222222',
             'VALID',
             1,
             'MT001',
             'test_user',
             now() - interval '1 day'
         );

-- Client
INSERT INTO client (
    id, claim_id, client_forename, client_surname, client_date_of_birth, unique_client_number, client_postcode,
    gender_code, ethnicity_code, disability_code, is_legally_aided, client_type_code, home_office_client_number,
    cla_reference_number, cla_exemption_code, created_by_user_id, created_on, updated_on
) VALUES (
             '44444444-4444-4444-4444-444444444444',
             '33333333-3333-3333-3333-333333333333',
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
             now() - interval '1 day',
             now() - interval '1 day'
         );

INSERT INTO client (
    id, claim_id, client_forename, client_surname, client_date_of_birth, unique_client_number, client_postcode,
    gender_code, ethnicity_code, disability_code, is_legally_aided, client_type_code, home_office_client_number,
    cla_reference_number, cla_exemption_code, created_by_user_id, created_on
) VALUES (
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
             now() - interval '1 day'
         );

-- Claim Case
INSERT INTO claim_case (
    id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code, created_by_user_id, created_on
) VALUES (
             '55555555-5555-5555-5555-555555555555',
             '33333333-3333-3333-3333-333333333333',
             'CASE001',
             'UCASE001',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             'test_user',
             now() - interval '1 day'
         );

INSERT INTO claim_case (
    id, claim_id, case_id, unique_case_id, case_stage_code, stage_reached_code, outcome_code, created_by_user_id, created_on
) VALUES (
             '55555555-5555-5555-5555-555555555556',
             '33333333-3333-3333-3333-333333333334',
             'CASE001',
             'UCASE001',
             'STAGE1',
             'REACHED1',
             'SUCCESS',
             'test_user',
             now() - interval '1 day'
         );

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
             now() - interval '1 day', now() - interval '1 day'
         );

INSERT INTO claim_summary_fee (
    id, claim_id, advice_time, travel_time, waiting_time, net_profit_costs_amount, net_disbursement_amount,
    net_counsel_costs_amount, disbursements_vat_amount, travel_waiting_costs_amount, net_waiting_costs_amount,
    is_vat_applicable, is_tolerance_applicable, created_by_user_id, created_on, updated_on
) VALUES (
             '66666666-6666-6666-6666-666666666667',
             '33333333-3333-3333-3333-333333333334',
             60, 30, 15, 1000, 200,
          500, 100, 50, 20,
          TRUE, FALSE, 'test_user',
             now() - interval '2 day', now() - interval '1 day'
         );

-- Calculated Fee Detail (3 rows)
INSERT INTO calculated_fee_detail (
    id, claim_summary_fee_id, claim_id, fee_code, fee_type, created_by_user_id, created_on, updated_by_user_id, updated_on,
    fee_code_description, category_of_law, total_amount
) VALUES
      ('77777777-7777-7777-7777-777777777777', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE001', 'TypeA', 'test_user', '2025-10-20 09:00:00+00', 'test_user', '2025-10-20 09:30:00+00', 'Description 1', 'Crime', 1500),
      ('88888888-8888-8888-8888-888888888888', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE002', 'TypeB', 'test_user', '2025-10-21 10:00:00+00', 'test_user', '2025-10-21 10:30:00+00', 'Description 2', 'Crime', 2000),
      ('88888888-8888-8888-8888-888888888889', '66666666-6666-6666-6666-666666666667', '33333333-3333-3333-3333-333333333334',
       'FEE002', 'TypeB', 'test_user', '2025-10-21 10:00:00+00', 'test_user', '2025-10-21 10:30:00+00', 'Description 2', 'Crime', 2000),
      ('99999999-9999-9999-9999-999999999999', '66666666-6666-6666-6666-666666666666', '33333333-3333-3333-3333-333333333333',
       'FEE003', 'TypeC', 'test_user', '2025-10-22 11:00:00+00', 'test_user', '2025-10-22 11:30:00+00', 'Description 3', 'Crime', 2500);