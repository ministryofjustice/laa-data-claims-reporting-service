-- Runs ONLY in local profile
-- Safe mock data for replication health checks

INSERT INTO claims.replication_summary (
    summary_date,
    table_name,
    record_count,
    updated_count,
    wal_lsn,
    created_on
) VALUES
      (current_date - 1, 'claims.claim', 4, 2, '2CE/FFFFFFE0', current_date - 1),
      (current_date - 1, 'claims.assessment', 7, 1, '2CE/FFFFFFE0', current_date - 1)
    ON CONFLICT (table_name, summary_date) DO NOTHING;;

INSERT INTO submission (
    id,
    bulk_submission_id,
    office_account_number,
    submission_period,
    area_of_law,
    status,
    provider_user_id,
    created_by_user_id,
    created_on,
    updated_on
) VALUES
      ('22222222-2222-2222-2222-222222222222', '11111111-aaaa-bbbb-cccc-111111111111', 'OFFICE001', '2023-01', 'CRIME', 'CREATED', 'test provider user', 'user1', '2023-10-01 10:00:00', '2023-10-01 12:00:00'),
      ('44444444-4444-4444-4444-444444444444', '22222222-bbbb-cccc-dddd-222222222222', 'OFFICE002', '2023-02', 'CIVIL', 'READY_FOR_VALIDATION', 'test provider user','user2', '2023-10-02 11:00:00', '2023-10-02 13:00:00'),
      ('66666666-6666-6666-6666-666666666666', '33333333-cccc-dddd-eeee-333333333333', 'OFFICE003', '2023-03', 'CRIME', 'VALIDATION_SUCCEEDED', 'test provider user','user3', '2023-10-03 12:00:00', '2023-10-03 14:00:00'),
      ('88888888-8888-8888-8888-888888888888', '44444444-dddd-eeee-ffff-444444444444', 'OFFICE004', '2023-04', 'CIVIL', 'VALIDATION_FAILED', 'test provider user', 'user4', '2023-10-04 13:00:00', '2023-10-04 15:00:00')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO claim (
    id,
    submission_id,
    status,
    line_number,
    matter_type_code,
    created_by_user_id,
    created_on,
    updated_on
) VALUES
      ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'READY_TO_PROCESS', 1, 'MT001', 'user1', '2023-10-01 10:00:00', CURRENT_DATE - 1),
      ('33333333-3333-3333-3333-333333333333', '44444444-4444-4444-4444-444444444444', 'VALID', 2, 'MT002', 'user2', '2023-10-02 11:00:00', CURRENT_DATE - 1),
      ('55555555-5555-5555-5555-555555555555', '66666666-6666-6666-6666-666666666666', 'INVALID', 3, 'MT003', 'user3', '2023-10-03 12:00:00', '2023-10-03 14:00:00'),
      ('77777777-7777-7777-7777-777777777777', '88888888-8888-8888-8888-888888888888', 'READY_TO_PROCESS', 4, 'MT004', 'user4', '2023-10-04 13:00:00', '2023-10-04 15:00:00')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO claim_summary_fee (id, claim_id, created_by_user_id, created_on, updated_on
) VALUES
      ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','11111111-1111-1111-1111-111111111111','test_user',TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day'),
      ('ffffffff-ffff-ffff-ffff-ffffffffffff','33333333-3333-3333-3333-333333333333','test_user',TIMESTAMP '2025-11-21 05:00:00' - interval '2 day', TIMESTAMP '2025-11-21 05:00:00' - interval '1 day')
    ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment (
    id,
    claim_id,
    claim_summary_fee_id,
    assessment_outcome,
    assessed_total_vat,
    assessed_total_incl_vat,
    allowed_total_vat,
    allowed_total_incl_vat,
    created_by_user_id,
    created_on,
    updated_on
) VALUES
      ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'PAID_IN_FULL', 100.00, 1200.00, 90.00, 1100.00, 'user1', '2023-10-01 10:00:00', CURRENT_DATE - 1),
      ('cccccccc-cccc-cccc-cccc-cccccccccccc', '11111111-1111-1111-1111-111111111111', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'REDUCED_TO_FIXED_FEE', 50.00, 600.00, 45.00, 550.00, 'user2', '2023-10-02 11:00:00', '2023-10-02 13:00:00'),
      ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '33333333-3333-3333-3333-333333333333', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'REDUCED_STILL_ESCAPED', 75.00, 800.00, 70.00, 750.00, 'user3', '2023-10-03 12:00:00', '2023-10-03 14:00:00'),
      ('11111111-2222-3333-4444-555555555555', '55555555-5555-5555-5555-555555555555', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'NILLED', 0.00, 0.00, 0.00, 0.00, 'user4', '2023-10-04 13:00:00', '2023-10-04 15:00:00'),
      ('77777777-8888-9999-aaaa-bbbbbbbbbbbb', '77777777-7777-7777-7777-777777777777', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'PAID_IN_FULL', 120.00, 1300.00, 110.00, 1250.00, 'user5', '2023-10-05 14:00:00', '2023-10-05 16:00:00'),
      ('99999999-aaaa-bbbb-cccc-dddddddddddd', '33333333-3333-3333-3333-333333333333', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'REDUCED_TO_FIXED_FEE', 60.00, 700.00, 55.00, 650.00, 'user6', '2023-10-06 15:00:00', '2023-10-06 17:00:00'),
      ('bbbbbbbb-cccc-dddd-eeee-ffffffffffff', '55555555-5555-5555-5555-555555555555', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'REDUCED_STILL_ESCAPED', 80.00, 900.00, 75.00, 850.00, 'user7', '2023-10-07 16:00:00', '2023-10-07 18:00:00')
    ON CONFLICT (id) DO NOTHING;