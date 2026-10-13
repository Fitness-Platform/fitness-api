ALTER TABLE program_accesses
    ADD COLUMN purchase_id UUID;

ALTER TABLE program_accesses
    ADD CONSTRAINT fk_program_accesses_purchase
        FOREIGN KEY (purchase_id)
            REFERENCES purchases(id);

ALTER TABLE program_accesses
    ADD CONSTRAINT uk_program_accesses_purchase
        UNIQUE (purchase_id);