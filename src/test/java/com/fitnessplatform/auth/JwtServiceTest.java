package com.fitnessplatform.auth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String DIFFERENT_SECRET =
            "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    @Test
    void shouldGenerateTokenAndExtractUserId() {
        JwtService jwtService = new JwtService(
                SECRET,
                Duration.ofHours(1)
        );

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId);

        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals(userId, extractedUserId);
    }

    @Test
    void shouldRejectTokenSignedWithDifferentKey() {
        JwtService jwtService = new JwtService(
                SECRET,
                Duration.ofHours(1)
        );

        JwtService otherJwtService = new JwtService(
                DIFFERENT_SECRET,
                Duration.ofHours(1)
        );

        String token = jwtService.generateToken(UUID.randomUUID());

        assertThrows(
                JwtException.class,
                () -> otherJwtService.extractUserId(token)
        );
    }

    @Test
    void shouldRejectMalformedToken() {
        JwtService jwtService = new JwtService(
                SECRET,
                Duration.ofHours(1)
        );

        assertThrows(
                JwtException.class,
                () -> jwtService.extractUserId("not-a-valid-jwt")
        );
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtService jwtService = new JwtService(
                SECRET,
                Duration.ofSeconds(-1)
        );

        String token = jwtService.generateToken(UUID.randomUUID());

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractUserId(token)
        );
    }
}