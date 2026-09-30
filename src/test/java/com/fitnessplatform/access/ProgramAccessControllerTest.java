package com.fitnessplatform.access;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
import com.fitnessplatform.program.Program;
import com.fitnessplatform.program.ProgramRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProgramAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramAccessRepository programAccessRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        programAccessRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();

        programRepository.deleteAll();
    }

    @Test
    void shouldGrantProgramAccessAsAdmin()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T00:00:00Z",
                                          "expiresAt": "2027-04-01T00:00:00Z"
                                        }
                                        """.formatted(
                                                member.getId(),
                                                program.getId()
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.id")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.startsAt")
                                .value(
                                        "2026-10-01T00:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .value(
                                        "2027-04-01T00:00:00Z"
                                )
                )
                .andExpect(
                        jsonPath("$.revokedAt")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.updatedAt")
                                .isNotEmpty()
                );

        assertTrue(
                programAccessRepository
                        .findAllByUserIdAndProgramIdOrderByCreatedAtDesc(
                                member.getId(),
                                program.getId()
                        )
                        .size()
                        == 1
        );
    }

    @Test
    void shouldRejectInvalidProgramAccessRequest()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": null,
                                          "programId": null,
                                          "startsAt": null,
                                          "expiresAt": null
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                );

        assertTrue(
                programAccessRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnNotFoundWhenGrantingAccessToMissingUser()
            throws Exception {

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T00:00:00Z",
                                          "expiresAt": null
                                        }
                                        """.formatted(
                                                UUID.randomUUID(),
                                                program.getId()
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "User not found"
                                )
                );

        assertTrue(
                programAccessRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldReturnNotFoundWhenGrantingAccessToMissingProgram()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T00:00:00Z",
                                          "expiresAt": null
                                        }
                                        """.formatted(
                                                member.getId(),
                                                UUID.randomUUID()
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program not found"
                                )
                );

        assertTrue(
                programAccessRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectInvalidProgramAccessPeriod()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T00:00:00Z",
                                          "expiresAt": "2026-10-01T00:00:00Z"
                                        }
                                        """.formatted(
                                                member.getId(),
                                                program.getId()
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Invalid program access period"
                                )
                );

        assertTrue(
                programAccessRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void shouldRejectUserAccessToProgramAccessManagement()
            throws Exception {

        Cookie userCookie =
                authenticatedCookie(
                        UserRole.USER
                );

        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                                .cookie(userCookie)
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void shouldRejectUnauthenticatedAccessToProgramAccessManagement()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void shouldRejectProgramAccessMutationWithoutCsrf()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createProgram(
                        "Six Week Strength"
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses"
                        )
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "userId": "%s",
                                          "programId": "%s",
                                          "startsAt": "2026-10-01T00:00:00Z",
                                          "expiresAt": null
                                        }
                                        """.formatted(
                                                member.getId(),
                                                program.getId()
                                        )
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        assertTrue(
                programAccessRepository
                        .findAll()
                        .isEmpty()
        );
    }

    private User createUser(
            String email,
            UserRole role
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                )
        );
    }

    private Program createProgram(
            String name
    ) {
        return programRepository.saveAndFlush(
                new Program(
                        name,
                        null
                )
        );
    }

    private Cookie authenticatedCookie(
            UserRole role
    ) {
        User user =
                new User(
                        role.name()
                                .toLowerCase()
                                + "@example.com",
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                );

        user =
                userRepository.saveAndFlush(
                        user
                );

        String token =
                jwtService.generateToken(
                        user.getId()
                );

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }

    @Test
    void shouldReturnProgramAccessByIdAsAdmin()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        "2026-10-01T00:00:00Z",
                        "2027-04-01T00:00:00Z"
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses/{accessId}",
                                access.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        access.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(
                                        program.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldReturnNotFoundWhenProgramAccessDoesNotExist()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses/{accessId}",
                                UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access not found"
                                )
                );
    }

    @Test
    void shouldFilterProgramAccessesAsAdmin()
            throws Exception {

        User userA =
                createUser(
                        "user-a@example.com",
                        UserRole.USER
                );

        User userB =
                createUser(
                        "user-b@example.com",
                        UserRole.USER
                );

        Program programA =
                createProgram(
                        "Program A"
                );

        Program programB =
                createProgram(
                        "Program B"
                );

        createAccess(
                userA,
                programA,
                "2026-10-01T00:00:00Z",
                null
        );

        createAccess(
                userA,
                programB,
                "2026-10-02T00:00:00Z",
                null
        );

        createAccess(
                userB,
                programA,
                "2026-10-03T00:00:00Z",
                null
        );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        // All accesses
        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(3)
                );

        // User A
        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                                .param(
                                        "userId",
                                        userA.getId()
                                                .toString()
                                )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                );

        // Program A
        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                                .param(
                                        "programId",
                                        programA.getId()
                                                .toString()
                                )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                );

        // User A + Program A
        mockMvc.perform(
                        get(
                                "/api/admin/program-accesses"
                        )
                                .param(
                                        "userId",
                                        userA.getId()
                                                .toString()
                                )
                                .param(
                                        "programId",
                                        programA.getId()
                                                .toString()
                                )
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].userId")
                                .value(
                                        userA.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].programId")
                                .value(
                                        programA.getId()
                                                .toString()
                                )
                );
    }

    @Test
    void shouldRevokeProgramAccessAsAdmin()
            throws Exception {

        User member =
                createUser(
                        "member@example.com",
                        UserRole.USER
                );

        Program program =
                createProgram(
                        "Strength Program"
                );

        ProgramAccess access =
                createAccess(
                        member,
                        program,
                        "2026-10-01T00:00:00Z",
                        null
                );

        UUID accessId =
                access.getId();

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses/{accessId}/revoke",
                                accessId
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        accessId.toString()
                                )
                )
                .andExpect(
                        jsonPath("$.revokedAt")
                                .isNotEmpty()
                );

        ProgramAccess persisted =
                programAccessRepository
                        .findById(
                                accessId
                        )
                        .orElseThrow();

        assertNotNull(
                persisted.getRevokedAt()
        );

        assertTrue(
                programAccessRepository.existsById(
                        accessId
                )
        );
    }

    @Test
    void shouldReturnNotFoundWhenRevokingMissingProgramAccess()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        post(
                                "/api/admin/program-accesses/{accessId}/revoke",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program access not found"
                                )
                );
    }

    private ProgramAccess createAccess(
            User user,
            Program program,
            String startsAt,
            String expiresAt
    ) {
        return programAccessRepository.saveAndFlush(
                new ProgramAccess(
                        user,
                        program,
                        java.time.Instant.parse(startsAt),
                        expiresAt == null
                                ? null
                                : java.time.Instant.parse(expiresAt)
                )
        );
    }
}