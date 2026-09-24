package com.fitnessplatform.auth;

import com.fitnessplatform.auth.passwordreset.ForgotPasswordRequest;
import com.fitnessplatform.auth.passwordreset.PasswordResetService;
import com.fitnessplatform.auth.passwordreset.ResetPasswordRequest;
import com.fitnessplatform.user.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    private final String cookieName;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    private final PasswordResetService passwordResetService;

    public AuthController(
            RegistrationService registrationService,
            AuthenticationService authenticationService,
            JwtService jwtService,
            PasswordResetService passwordResetService,
            @Value("${security.auth.cookie.name}") String cookieName,
            @Value("${security.auth.cookie.secure}") boolean cookieSecure,
            @Value("${security.auth.cookie.same-site}") String cookieSameSite
    ) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.cookieName = cookieName;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return RegisterResponse.from(
                registrationService.register(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        User user = authenticationService.authenticate(request);

        String token = jwtService.generateToken(user.getId());

        ResponseCookie cookie = ResponseCookie
                .from(cookieName, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(jwtService.getExpiration())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(LoginResponse.from(user));
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();

        User user = authenticationService
                .getAuthenticatedUser(userId);

        return MeResponse.from(user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cookie = ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        passwordResetService.resetPassword(
                request.token(),
                request.newPassword()
        );
    }
}
