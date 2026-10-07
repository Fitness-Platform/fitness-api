ALTER TABLE purchases
    ADD COLUMN stripe_checkout_session_id VARCHAR(255);

ALTER TABLE purchases
    ADD CONSTRAINT uq_purchases_stripe_checkout_session_id
        UNIQUE (stripe_checkout_session_id);