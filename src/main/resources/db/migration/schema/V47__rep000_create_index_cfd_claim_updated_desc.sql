-- Supports lateral lookup for latest calculated fee detail per claim
CREATE INDEX IF NOT EXISTS idx_cfd_claim_updated_desc
ON claims.calculated_fee_detail (claim_id, updated_on DESC, created_on DESC);
