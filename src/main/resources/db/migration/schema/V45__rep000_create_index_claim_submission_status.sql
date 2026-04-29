-- Faster submission → claim JOIN and status filtering
CREATE INDEX IF NOT EXISTS idx_claim_submission_status
ON claims.claim (submission_id, status);
