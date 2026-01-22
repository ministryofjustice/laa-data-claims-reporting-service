CREATE OR REPLACE FUNCTION claims.normalise_area_of_law(area_of_law text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
SELECT CASE
           WHEN UPPER(area_of_law) IN ('CRIME LOWER','CRIME_LOWER') THEN 'CRIME LOWER'
           WHEN UPPER(area_of_law) IN ('LEGAL HELP','LEGAL_HELP') THEN 'CIVIL'
           ELSE UPPER(area_of_law)
           END;
$$;

CREATE OR REPLACE FUNCTION claims.submission_is_not_replaced(submission_id UUID)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
SELECT NOT EXISTS (
    SELECT 1
    FROM claims.submission newer
    WHERE newer.previous_submission_id = submission_id
);
$$;

