ALTER TABLE purchases
    ADD COLUMN stripe_payment_intent_id VARCHAR(255);

ALTER TABLE purchases
    ADD CONSTRAINT uk_purchases_stripe_payment_intent_id
        UNIQUE (stripe_payment_intent_id);