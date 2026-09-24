package com.fitnessplatform.auth.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthenticationEmailService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthenticationEmailService.class);

    private final Resend resend;
    private final String from;
    private final String frontendBaseUrl;

    public AuthenticationEmailService(
            Resend resend,
            @Value("${email.auth.from}") String from,
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.resend = resend;
        this.from = from;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendPasswordResetEmail(
            String recipientEmail,
            String rawToken
    ) {
        String resetUrl = buildResetUrl(rawToken);

        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(from)
                .to(recipientEmail)
                .subject("Reset your Fitness Platform password")
                .html(buildPasswordResetHtml(resetUrl))
                .build();

        try {
            resend.emails().send(email);

        } catch (ResendException exception) {
            log.error(
                    "Failed to send password reset email",
                    exception
            );
        }
    }

    private String buildResetUrl(String rawToken) {
        return UriComponentsBuilder
                .fromUriString(frontendBaseUrl)
                .path("/reset-password")
                .queryParam("token", rawToken)
                .build()
                .toUriString();
    }

    private String buildPasswordResetHtml(String resetUrl) {
        return """
                <p>You requested a password reset for your Fitness Platform account.</p>
                <p>
                    <a href="%s">Reset your password</a>
                </p>
                <p>If you did not request this, you can ignore this email.</p>
                """.formatted(resetUrl);
    }
}
