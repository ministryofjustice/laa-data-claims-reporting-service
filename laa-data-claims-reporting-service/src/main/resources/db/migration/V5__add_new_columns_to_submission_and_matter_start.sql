ALTER TABLE matter_start
    ADD COLUMN mediation_type TEXT;

ALTER TABLE submission
    ADD COLUMN provider_user_id TEXT NOT NULL;
