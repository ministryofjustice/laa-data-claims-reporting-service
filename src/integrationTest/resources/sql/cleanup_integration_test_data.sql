DELETE FROM claims.assessment            WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.calculated_fee_detail WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.claim_summary_fee     WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.client                WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.claim_case            WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.claim                 WHERE created_by_user_id = 'integration_test_user';
DELETE FROM claims.submission            WHERE created_by_user_id = 'integration_test_user';