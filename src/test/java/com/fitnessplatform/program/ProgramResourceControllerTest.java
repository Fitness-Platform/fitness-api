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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class ProgramResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private ProgramResourceRepository programResourceRepository;

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
        programResourceRepository.deleteAll();
        programRepository.deleteAll();

        passwordResetTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateProgramResourceAsAdmin()
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
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "  Nutrition Guide  ",
                                          "description": "  Supporting material.  ",
                                          "url": "  https://example.com/guide.pdf  ",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Nutrition Guide")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Supporting material.")
                )
                .andExpect(
                        jsonPath("$.url")
                                .value("https://example.com/guide.pdf")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                )
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertEquals(
                1,
                programResourceRepository.findAll().size()
        );
    }

    @Test
    void shouldRejectInvalidProgramResource()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "   ",
                                          "url": "   ",
                                          "position": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());

        assertTrue(
                programResourceRepository.findAll().isEmpty()
        );
    }

    @Test
    void shouldReturnNotFoundWhenCreatingResourceForMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/resources",
                                UUID.randomUUID()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Nutrition Guide",
                                          "url": "https://example.com/guide",
                                          "position": 1
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
    void shouldListResourcesOrderedByPosition()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        programResourceRepository.saveAndFlush(
                new ProgramResource(
                        program,
                        "Checklist",
                        null,
                        "https://example.com/checklist",
                        2
                )
        );

        programResourceRepository.saveAndFlush(
                new ProgramResource(
                        program,
                        "Nutrition Guide",
                        null,
                        "https://example.com/guide",
                        1
                )
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].title")
                                .value("Nutrition Guide")
                )
                .andExpect(
                        jsonPath("$[0].position")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[1].title")
                                .value("Checklist")
                )
                .andExpect(
                        jsonPath("$[1].position")
                                .value(2)
                );
    }

    @Test
    void shouldReturnEmptyListForProgramWithoutResources()
            throws Exception {

        Program program =
                createProgram("Empty Program");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenListingResourcesForMissingProgram()
            throws Exception {

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources",
                                UUID.randomUUID()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program not found")
                );
    }

    @Test
    void shouldReturnProgramResourceById()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        ProgramResource resource =
                createResource(
                        program,
                        "Nutrition Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                program.getId(),
                                resource.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(resource.getId().toString())
                )
                .andExpect(
                        jsonPath("$.programId")
                                .value(program.getId().toString())
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Nutrition Guide")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                );
    }

    @Test
    void shouldRejectResourceFromDifferentProgramOnGet()
            throws Exception {

        Program programA =
                createProgram("Program A");

        Program programB =
                createProgram("Program B");

        ProgramResource resource =
                createResource(
                        programA,
                        "Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                programB.getId(),
                                resource.getId()
                        )
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program resource not found")
                );
    }

    @Test
    void shouldRejectDuplicatePositionOnCreate()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        createResource(
                program,
                "Nutrition Guide",
                1
        );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Checklist",
                                          "url": "https://example.com/checklist",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program resource position conflict"
                                )
                );
    }

    @Test
    void shouldUpdateProgramResource()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        ProgramResource resource =
                createResource(
                        program,
                        "Old Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                program.getId(),
                                resource.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated Guide",
                                          "description": "Updated description",
                                          "url": "https://example.com/updated",
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.title")
                                .value("Updated Guide")
                )
                .andExpect(
                        jsonPath("$.description")
                                .value("Updated description")
                )
                .andExpect(
                        jsonPath("$.url")
                                .value("https://example.com/updated")
                )
                .andExpect(
                        jsonPath("$.position")
                                .value(2)
                );
    }

    @Test
    void shouldAllowResourceToKeepItsOwnPosition()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        ProgramResource resource =
                createResource(
                        program,
                        "Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                program.getId(),
                                resource.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Updated Guide",
                                          "url": "https://example.com/updated",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.position")
                                .value(1)
                );
    }

    @Test
    void shouldRejectDuplicatePositionOnUpdate()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        createResource(
                program,
                "Guide",
                1
        );

        ProgramResource checklist =
                createResource(
                        program,
                        "Checklist",
                        2
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                program.getId(),
                                checklist.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Checklist",
                                          "url": "https://example.com/checklist",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.title")
                                .value(
                                        "Program resource position conflict"
                                )
                );
    }

    @Test
    void shouldRejectUpdatingResourceThroughDifferentProgram()
            throws Exception {

        Program programA =
                createProgram("Program A");

        Program programB =
                createProgram("Program B");

        ProgramResource resource =
                createResource(
                        programA,
                        "Original Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        put(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                programB.getId(),
                                resource.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Changed Guide",
                                          "url": "https://example.com/changed",
                                          "position": 2
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program resource not found")
                );

        ProgramResource persisted =
                programResourceRepository
                        .findById(resource.getId())
                        .orElseThrow();

        assertEquals(
                "Original Guide",
                persisted.getTitle()
        );

        assertEquals(
                1,
                persisted.getPosition()
        );
    }

    @Test
    void shouldDeleteProgramResource()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        ProgramResource resource =
                createResource(
                        program,
                        "Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                program.getId(),
                                resource.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNoContent());

        assertFalse(
                programResourceRepository.existsById(
                        resource.getId()
                )
        );
    }

    @Test
    void shouldRejectDeletingResourceThroughDifferentProgram()
            throws Exception {

        Program programA =
                createProgram("Program A");

        Program programB =
                createProgram("Program B");

        ProgramResource resource =
                createResource(
                        programA,
                        "Guide",
                        1
                );

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        delete(
                                "/api/admin/programs/{programId}/resources/{resourceId}",
                                programB.getId(),
                                resource.getId()
                        )
                                .with(csrf())
                                .cookie(adminCookie)
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.title")
                                .value("Program resource not found")
                );

        assertTrue(
                programResourceRepository.existsById(
                        resource.getId()
                )
        );
    }

    @Test
    void shouldDeleteResourcesWhenProgramIsDeleted() {

        Program program =
                createProgram("Strength Program");

        ProgramResource resource =
                createResource(
                        program,
                        "Guide",
                        1
                );

        UUID resourceId =
                resource.getId();

        programRepository.delete(program);
        programRepository.flush();

        assertFalse(
                programResourceRepository.existsById(
                        resourceId
                )
        );
    }

    @Test
    void shouldRejectUserAccessToProgramResources()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        Cookie userCookie =
                authenticatedCookie(UserRole.USER);

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .cookie(userCookie)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectUnauthenticatedAccessToProgramResources()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/admin/programs/{programId}/resources",
                                UUID.randomUUID()
                        )
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectProgramResourceMutationWithoutCsrf()
            throws Exception {

        Program program =
                createProgram("Strength Program");

        Cookie adminCookie =
                authenticatedCookie(UserRole.ADMIN);

        mockMvc.perform(
                        post(
                                "/api/admin/programs/{programId}/resources",
                                program.getId()
                        )
                                .cookie(adminCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "Guide",
                                          "url": "https://example.com/guide",
                                          "position": 1
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());

        assertTrue(
                programResourceRepository.findAll().isEmpty()
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

    private ProgramResource createResource(
            Program program,
            String title,
            Integer position
    ) {
        return programResourceRepository.saveAndFlush(
                new ProgramResource(
                        program,
                        title,
                        null,
                        "https://example.com/"
                                + title.toLowerCase()
                                .replace(" ", "-"),
                        position
                )
        );
    }

    private Cookie authenticatedCookie(
            UserRole role
    ) {
        User user =
                new User(
                        role.name().toLowerCase()
                                + "@example.com",
                        passwordEncoder.encode(
                                "StrongPassword123!"
                        ),
                        role
                );

        user =
                userRepository.saveAndFlush(user);

        String token =
                jwtService.generateToken(
                        user.getId()
                );

        return new Cookie(
                "AUTH_TOKEN",
                token
        );
    }
}