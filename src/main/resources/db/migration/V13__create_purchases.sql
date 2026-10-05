CREATE TABLE purchases (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,
    program_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL,

    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,

    paid_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_purchases_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_purchases_program
        FOREIGN KEY (program_id)
        REFERENCES programs(id),

    CONSTRAINT chk_purchases_status
        CHECK (
           status IN (
                'PENDING',
                'PAID',
                'EXPIRED'
           )
        ),

    CONSTRAINT chk_purchases_amount_cents
        CHECK (amount_cents > 0),

    CONSTRAINT chk_purchases_currency
        CHECK (char_length(currency) = 3)
);

CREATE INDEX idx_purchases_user_id
    ON purchases(user_id);

CREATE INDEX idx_purchases_program_id
    ON purchases(program_id);

CREATE INDEX idx_purchases_user_program
    ON purchases(user_id, program_id);