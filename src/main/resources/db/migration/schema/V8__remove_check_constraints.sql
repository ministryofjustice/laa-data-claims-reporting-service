ALTER TABLE bulk_submission DROP CONSTRAINT chk_bulk_submission_status;

ALTER TABLE submission DROP CONSTRAINT chk_submission_status;

ALTER TABLE claim DROP CONSTRAINT chk_claim_status;