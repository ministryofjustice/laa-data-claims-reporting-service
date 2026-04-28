-- Speeds up the 3-year filter on bulk submissions
CREATE INDEX IF NOT EXISTS idx_bulk_submission_created
ON claims.bulk_submission (created_on);
