package com.fitnessplatform.auth.passwordreset;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final Duration expiration;
    private final SecureRandom secureRandom;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            @Value("${security.password-reset.expiration}")
            Duration expiration
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.expiration = expiration;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public String requestReset(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElse(null);

        if (user == null) {
            return null;
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);

        tokenRepository.deleteByUserId(user.getId());

        PasswordResetToken resetToken =
                new PasswordResetToken(
                        user,
                        tokenHash,
                        Instant.now().plus(expiration)
                );

        tokenRepository.save(resetToken);

        return rawToken;
    }

    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword
    ) {
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken =
                tokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(
                                InvalidPasswordResetTokenException::new
                        );

        Instant now = Instant.now();

        if (resetToken.isExpired(now)) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = resetToken.getUser();

        String passwordHash =
                passwordEncoder.encode(newPassword);

        user.changePasswordHash(passwordHash);

        tokenRepository.delete(resetToken);
    }

    String hashToken(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String normalizeEmail(String email) {
        return email.strip()
                .toLowerCase(Locale.ROOT);
    }
}