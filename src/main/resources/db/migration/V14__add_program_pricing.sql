ALTER TABLE programs
    ADD COLUMN price_cents BIGINT,
    ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD';

ALTER TABLE programs
    ADD CONSTRAINT chk_programs_price_cents
        CHECK (
            price_cents IS NULL
                OR price_cents > 0
            );

ALTER TABLE programs
    ADD CONSTRAINT chk_programs_currency
        CHECK (currency = 'USD');