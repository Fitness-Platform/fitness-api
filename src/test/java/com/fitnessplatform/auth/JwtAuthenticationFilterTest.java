package com.fitnessplatform.auth;

import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtService,
                userRepository,
                "AUTH_TOKEN"
        );

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueUnauthenticatedWhenCookieIsMissing()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> assertNull(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
        );

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldAuthenticateWhenCookieContainsValidToken()
            throws Exception {

        UUID userId = UUID.randomUUID();

        User user = mock(User.class);

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(user.getId())
                .thenReturn(userId);

        when(user.getRole())
                .thenReturn(UserRole.USER);

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie("AUTH_TOKEN", "valid-token")
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    Authentication authentication =
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication();

                    assertNotNull(authentication);
                    assertEquals(
                            userId,
                            authentication.getPrincipal()
                    );

                    assertTrue(
                            authentication.getAuthorities()
                                    .stream()
                                    .anyMatch(authority ->
                                            authority.getAuthority()
                                                    .equals("ROLE_USER")
                                    )
                    );
                }
        );
    }

    @Test
    void shouldContinueUnauthenticatedWhenTokenIsInvalid()
            throws Exception {

        when(jwtService.extractUserId("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie("AUTH_TOKEN", "invalid-token")
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> assertNull(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldContinueUnauthenticatedWhenUserNoLongerExists()
            throws Exception {

        UUID userId = UUID.randomUUID();

        when(jwtService.extractUserId("valid-token"))
                .thenReturn(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.setCookies(
                new Cookie("AUTH_TOKEN", "valid-token")
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> assertNull(
                        SecurityContextHolder
                                .getContext()
                                .getAuthentication()
                )
        );
    }
}