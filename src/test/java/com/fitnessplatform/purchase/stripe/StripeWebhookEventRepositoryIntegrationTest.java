package com.fitnessplatform.purchase.stripe;

import com.fitnessplatform.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class StripeWebhookEventRepositoryIntegrationTest {

    @Autowired
    private StripeWebhookEventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update(
                "DELETE FROM stripe_webhook_events"
        );
    }

    @Test
    void shouldInsertNewStripeEvent() {
        boolean inserted = eventRepository.insertIfAbsent(
                "evt_test_123",
                "checkout.session.completed"
        );

        assertTrue(inserted);

        assertEquals(
                1L,
                countEvents()
        );
    }

    @Test
    void shouldNotInsertDuplicateStripeEvent() {
        boolean firstInsert = eventRepository.insertIfAbsent(
                "evt_test_123",
                "checkout.session.completed"
        );

        boolean secondInsert = eventRepository.insertIfAbsent(
                "evt_test_123",
                "checkout.session.completed"
        );

        assertTrue(firstInsert);
        assertFalse(secondInsert);

        assertEquals(
                1L,
                countEvents()
        );
    }

    @Test
    void shouldAllowDifferentStripeEvents() {
        boolean firstInsert = eventRepository.insertIfAbsent(
                "evt_test_123",
                "checkout.session.completed"
        );

        boolean secondInsert = eventRepository.insertIfAbsent(
                "evt_test_456",
                "checkout.session.expired"
        );

        assertTrue(firstInsert);
        assertTrue(secondInsert);

        assertEquals(
                2L,
                countEvents()
        );
    }

    private long countEvents() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stripe_webhook_events",
                Long.class
        );

        return count == null ? 0 : count;
    }
}