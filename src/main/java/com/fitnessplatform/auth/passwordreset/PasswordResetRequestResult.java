package com.fitnessplatform.auth.passwordreset;

public record PasswordResetRequestResult(
        String recipientEmail,
        String rawToken
) {
}
