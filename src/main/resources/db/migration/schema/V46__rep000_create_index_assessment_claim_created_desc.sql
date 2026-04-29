-- Supports lateral lookup for latest assessment per claim. Prevents scanning of the whole assessment table
CREATE INDEX IF NOT EXISTS idx_assessment_claim_created_desc
ON claims.assessment (claim_id, created_on DESC);
