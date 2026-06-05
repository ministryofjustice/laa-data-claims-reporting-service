ALTER TABLE calculated_fee_detail
    ADD COLUMN claim_amendment_id UUID,
    ADD COLUMN is_price_changed BOOLEAN;