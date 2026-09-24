package com.fitnessplatform.auth.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationEmailServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Resend resend;

    private AuthenticationEmailService authenticationEmailService;

    @BeforeEach
    void setUp() {
        authenticationEmailService =
                new AuthenticationEmailService(
                        resend,
                        "Fitness Platform <onboarding@resend.dev>",
                        "http://localhost:5173"
                );
    }

    @Test
    void shouldSendPasswordResetEmail() throws Exception {
        var resendEmails = resend.emails();

        authenticationEmailService.sendPasswordResetEmail(
                "samuel@example.com",
                "raw-reset-token"
        );

        ArgumentCaptor<CreateEmailOptions> captor =
                ArgumentCaptor.forClass(
                        CreateEmailOptions.class
                );

        verify(resendEmails)
                .send(captor.capture());

        CreateEmailOptions email = captor.getValue();

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode payload =
                objectMapper.valueToTree(email);

        assertEquals(
                "Fitness Platform <onboarding@resend.dev>",
                payload.get("from").asText()
        );

        assertEquals(
                "samuel@example.com",
                payload.get("to").get(0).asText()
        );

        assertEquals(
                "Reset your Fitness Platform password",
                payload.get("subject").asText()
        );

        String html = payload.get("html").asText();

        assertTrue(
                html.contains(
                        "http://localhost:5173/reset-password"
                )
        );

        assertTrue(
                html.contains(
                        "token=raw-reset-token"
                )
        );
    }

    @Test
    void shouldNotPropagateResendFailure() throws Exception {
        var resendEmails = resend.emails();

        doThrow(mock(ResendException.class))
                .when(resendEmails)
                .send(any(CreateEmailOptions.class));

        assertDoesNotThrow(
                () -> authenticationEmailService
                        .sendPasswordResetEmail(
                                "samuel@example.com",
                                "raw-reset-token"
                        )
        );
    }

    @Test
    void shouldSendWelcomeEmail() throws Exception {
        var resendEmails = resend.emails();

        authenticationEmailService.sendWelcomeEmail(
                "samuel@example.com"
        );

        ArgumentCaptor<CreateEmailOptions> captor =
                ArgumentCaptor.forClass(
                        CreateEmailOptions.class
                );

        verify(resendEmails)
                .send(captor.capture());

        CreateEmailOptions email = captor.getValue();

        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode payload =
                objectMapper.valueToTree(email);

        assertEquals(
                "Fitness Platform <onboarding@resend.dev>",
                payload.get("from").asText()
        );

        assertEquals(
                "samuel@example.com",
                payload.get("to").get(0).asText()
        );

        assertEquals(
                "Welcome to Fitness Platform",
                payload.get("subject").asText()
        );

        String html = payload.get("html").asText();

        assertTrue(
                html.contains("Welcome to Fitness Platform")
        );

        assertTrue(
                html.contains("http://localhost:5173")
        );
    }

    @Test
    void shouldNotPropagateWelcomeEmailFailure() throws Exception {
        var resendEmails = resend.emails();

        doThrow(mock(ResendException.class))
                .when(resendEmails)
                .send(any(CreateEmailOptions.class));

        assertDoesNotThrow(
                () -> authenticationEmailService
                        .sendWelcomeEmail(
                                "samuel@example.com"
                        )
        );
    }
}