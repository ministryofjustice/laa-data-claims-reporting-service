ALTER TABLE claims.client
    ADD COLUMN is_means_tested BOOLEAN;

ALTER TABLE claims.calculated_fee_detail
    ADD COLUMN is_inquest BOOLEAN;