ALTER TABLE submission
    RENAME COLUMN crime_schedule_number
    TO crime_lower_schedule_number;

ALTER TABLE submission
    RENAME COLUMN civil_submission_reference
    TO legal_help_submission_reference;