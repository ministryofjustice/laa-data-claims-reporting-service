ALTER TABLE calculated_fee_detail
    RENAME COLUMN detention_and_waiting_costs_amount
    TO detention_travel_and_waiting_costs_amount;

ALTER TABLE calculated_fee_detail
    ADD COLUMN bolt_on_substantive_hearing_fee NUMERIC;