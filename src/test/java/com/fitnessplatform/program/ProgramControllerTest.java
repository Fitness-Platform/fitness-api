package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.auth.passwordreset.PasswordResetTokenRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProgramControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
        programRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateProgramAsDraft() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/programs")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "  Six Week Strength  ",
                                          "description": "  Strength-focused program.  "
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.name")
                                .value("Six Week Strength")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Strength-focused program.")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                )
                .andExpect(
                        jsonPath("$.priceCents").value(
                                nullValue()
                        )
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertTrue(
                programRepository.findAll()
                        .stream()
                        .anyMatch(program ->
                                program.getStatus()
                                        == ProgramStatus.DRAFT
                        )
        );
    }

    @Test
    void shouldRejectInvalidProgram() throws Exception {
        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/programs")
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "   ",
                                          "description": "Program"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(programRepository.findAll().isEmpty());
    }

    @Test
    void shouldListProgramsAsAdmin() throws Exception {
        programRepository.saveAndFlush(
                new Program(
                        "Program A",
                        "Description A"
                )
        );

        programRepository.saveAndFlush(
                new Program(
                        "Program B",
                        "Description B"
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get("/api/admin/programs")
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].name").isNotEmpty()
                )
                .andExpect(
                        jsonPath("$[1].name").isNotEmpty()
                );
    }

    @Test
    void shouldReturnProgramByIdAsAdmin()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Six Week Strength",
                                "Program description"
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}",
                                program.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Six Week Strength")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );
    }

    @Test
    void shouldReturnNotFoundWhenProgramDoesNotExist()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}",
                                UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void shouldUpdateProgramWithoutChangingStatus()
            throws Exception {

        Program program =
                new Program(
                        "Old Program",
                        "Old description"
                );

        program.publish();

        program =
                programRepository.saveAndFlush(program);

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Updated Program",
                                          "description": "Updated description"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.name")
                                .value("Updated Program")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Updated description")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PUBLISHED")
                );
    }

    @Test
    void shouldPublishProgram()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/publish",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("PUBLISHED")
                );

        Program persisted =
                programRepository
                        .findById(program.getId())
                        .orElseThrow();

        assertTrue(
                persisted.getStatus()
                        == ProgramStatus.PUBLISHED
        );
    }

    @Test
    void shouldMovePublishedProgramBackToDraft()
            throws Exception {

        Program program =
                new Program(
                        "Strength Program",
                        null
                );

        program.publish();

        program =
                programRepository.saveAndFlush(program);

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/draft",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        Program persisted =
                programRepository
                        .findById(program.getId())
                        .orElseThrow();

        assertTrue(
                persisted.getStatus()
                        == ProgramStatus.DRAFT
        );
    }

    @Test
    void shouldDeleteProgramAsAdmin()
            throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Strength Program",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                programRepository.existsById(
                        program.getId()
                )
        );
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Strength Program",
                                          "description": null
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                );
    }

    @Test
    void shouldReturnNotFoundWhenPublishingMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/publish",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                );
    }

    @Test
    void shouldRejectUserAccessToProgramManagement()
            throws Exception {

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get("/api/admin/programs")
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToProgramManagement()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/programs")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProgramMutationWithoutCsrf()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post("/api/admin/programs")
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Strength Program"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        assertTrue(
                programRepository.findAll().isEmpty()
        );
    }

    @Test
    void shouldUpdateProgramPricing() throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Six Week Strength",
                                "Program description"
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/pricing",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "priceCents": 4999
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.priceCents")
                                .value(4999)
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                );

        Program persisted =
                programRepository
                        .findById(
                                program.getId()
                        )
                        .orElseThrow();

        assertEquals(
                4999L,
                persisted.getPriceCents()
        );

        assertEquals(
                "USD",
                persisted.getCurrency()
        );
    }

    @Test
    void shouldAllowClearingProgramPricing() throws Exception {

        Program program =
                new Program(
                        "Six Week Strength",
                        "Program description"
                );

        program.updatePrice(
                4999L
        );

        program =
                programRepository.saveAndFlush(
                        program
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/pricing",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "priceCents": null
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.priceCents")
                                .value(nullValue())
                )
                .andExpect(
                        jsonPath("$.currency")
                                .value("USD")
                );

        Program persisted =
                programRepository
                        .findById(
                                program.getId()
                        )
                        .orElseThrow();

        assertNull(
                persisted.getPriceCents()
        );
    }

    @Test
    void shouldRejectZeroProgramPrice() throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Six Week Strength",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/pricing",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "priceCents": 0
                                    }
                                    """)
                )
                .andExpect(
                        status().isBadRequest()
                );

        Program persisted =
                programRepository
                        .findById(
                                program.getId()
                        )
                        .orElseThrow();

        assertNull(
                persisted.getPriceCents()
        );
    }

    @Test
    void shouldRejectNegativeProgramPrice() throws Exception {

        Program program =
                programRepository.saveAndFlush(
                        new Program(
                                "Six Week Strength",
                                null
                        )
                );

        Cookie adminCookie =
                authenticatedCookie(
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/pricing",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                    {
                                      "priceCents": -100
                                    }
                                    """)
                )
                .andExpect(
                        status().isBadRequest()
                );

        Program persisted =
                programRepository
                        .findById(
                                program.getId()
                        )
                        .orElseThrow();

        assertNull(
                persisted.getPriceCents()
        );
    }

    @Test
    void shouldRejectNonPositiveProgramPriceAtDatabaseLevel() {

        Program program =
                new Program(
                        "Six Week Strength",
                        null
                );

        program.updatePrice(
                0L
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                        programRepository.saveAndFlush(
                                program
                        )
        );
    }

    private Cookie authenticatedCookie(
            UserRole role
    ) {
        User user = new User(
                role.name().toLowerCase()
                        + "@example.com",
                passwordEncoder.encode(
                        "StrongPassword123!"
                ),
                role
        );

        user = userRepository.saveAndFlush(user);

        String token =
                jwtService.generateToken(user.getId());

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }
}