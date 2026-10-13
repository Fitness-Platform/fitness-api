package com.fitnessplatform.purchase.stripe;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StripeWebhookEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public StripeWebhookEventRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean insertIfAbsent(
            String eventId,
            String eventType
    ) {
        int insertedRows = jdbcTemplate.update(
                """
                INSERT INTO stripe_webhook_events (
                    event_id,
                    event_type
                )
                VALUES (?, ?)
                ON CONFLICT (event_id) DO NOTHING
                """,
                eventId,
                eventType
        );

        return insertedRows == 1;
    }
}