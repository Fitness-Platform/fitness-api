package com.fitnessplatform.program;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.ProgramAccessRepository;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.purchase.PurchaseRepository;
import com.fitnessplatform.user.User;
import com.fitnessplatform.user.UserRepository;
import com.fitnessplatform.user.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicProgramControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProgramRepository programRepository;
    @Autowired private ProgramAccessRepository programAccessRepository;
    @Autowired private PurchaseRepository purchaseRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        programAccessRepository.deleteAll();
        purchaseRepository.deleteAll();
        programRepository.deleteAll();
    }

    @Test
    void shouldReturnEmptyCatalogAnonymouslyWithoutCsrf() throws Exception {
        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldListOnlyPublishedProgramsWithPersistedPublicFields() throws Exception {
        Program published = saveProgram("Database product", 12345L, true);
        saveProgram("Private draft", 9900L, false);
        entityManager.clear();

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]", aMapWithSize(5)))
                .andExpect(jsonPath("$[0].id").value(published.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("Database product"))
                .andExpect(jsonPath("$[0].description").value("Description for Database product"))
                .andExpect(jsonPath("$[0].priceCents").value(12345))
                .andExpect(jsonPath("$[0].currency").value("USD"));
    }

    @Test
    void shouldReturnPublishedProgramAnonymouslyWithOnlyPublicFields() throws Exception {
        Program published = saveProgram("Strength product", 7654L, true);
        entityManager.clear();

        mockMvc.perform(get("/api/programs/{programId}", published.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.id").value(published.getId().toString()))
                .andExpect(jsonPath("$.name").value("Strength product"))
                .andExpect(jsonPath("$.description").value("Description for Strength product"))
                .andExpect(jsonPath("$.priceCents").value(7654))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist())
                .andExpect(jsonPath("$.weeks").doesNotExist());
    }

    @Test
    void shouldHideDraftProgramWithExistingNotFoundResponse() throws Exception {
        Program draft = saveProgram("Private draft", 8900L, false);
        expectNotFound(draft.getId());
    }

    @Test
    void shouldReturnSameNotFoundConventionForMissingProgram() throws Exception {
        expectNotFound(UUID.randomUUID());
    }

    @Test
    void shouldImmediatelyHideProgramAfterItReturnsToDraft() throws Exception {
        Program program = saveProgram("Published then withdrawn", 8900L, true);
        mockMvc.perform(get("/api/programs/{programId}", program.getId()))
                .andExpect(status().isOk());

        program.moveToDraft();
        programRepository.saveAndFlush(program);
        entityManager.clear();

        expectNotFound(program.getId());
        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnUpdatedDatabasePriceWithoutHardCoding() throws Exception {
        Program program = saveProgram("Price changes", 8900L, true);
        program.updatePrice(11399L);
        programRepository.saveAndFlush(program);
        entityManager.clear();

        mockMvc.perform(get("/api/programs/{programId}", program.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(11399))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    void shouldPreserveNullablePriceAndDescriptionForPublishedPrograms() throws Exception {
        Program program = new Program("Published without price", null);
        program.publish();
        programRepository.saveAndFlush(program);
        entityManager.clear();

        mockMvc.perform(get("/api/programs/{programId}", program.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", aMapWithSize(5)))
                .andExpect(jsonPath("$.priceCents").value(nullValue()))
                .andExpect(jsonPath("$.description").value(nullValue()));
        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldListNewestFirstWithIdAsStableTieBreaker() throws Exception {
        Program older = saveProgram("Older", 1000L, true);
        Program first = saveProgram("Newer A", 2000L, true);
        Program second = saveProgram("Newer B", 3000L, true);
        ReflectionTestUtils.setField(older, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(first, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));
        ReflectionTestUtils.setField(second, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));
        // createdAt is not updatable, so use persisted database timestamps for this ordering fixture.
        entityManager.createNativeQuery("update programs set created_at = :createdAt where id = :id")
                .setParameter("createdAt", older.getCreatedAt()).setParameter("id", older.getId()).executeUpdate();
        for (Program newer : new Program[]{first, second}) {
            entityManager.createNativeQuery("update programs set created_at = :createdAt where id = :id")
                    .setParameter("createdAt", newer.getCreatedAt()).setParameter("id", newer.getId()).executeUpdate();
        }
        entityManager.clear();
        // UUID ordering in PostgreSQL is byte/unsigned order, equivalent to its canonical text order.
        String firstId = first.getId().toString().compareTo(second.getId().toString()) < 0
                ? first.getId().toString() : second.getId().toString();
        String secondId = firstId.equals(first.getId().toString())
                ? second.getId().toString() : first.getId().toString();

        mockMvc.perform(get("/api/programs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firstId))
                .andExpect(jsonPath("$[1].id").value(secondId))
                .andExpect(jsonPath("$[2].id").value(older.getId().toString()));
    }

    @Test
    void shouldNotPermitAnonymousCheckoutOrOtherProgramOperations() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(post("/api/programs/{programId}/checkout", id).with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/programs/{programId}/checkout", id))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/programs/{programId}/checkout", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/programs/{programId}/weeks", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/programs").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/programs/{programId}", id).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldKeepAuthenticatedCheckoutCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/programs/{programId}/checkout", UUID.randomUUID())
                        .cookie(cookie(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldKeepAdminAndMemberRoutesProtected() throws Exception {
        mockMvc.perform(get("/api/admin/programs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/programs").cookie(cookie(UserRole.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/programs").cookie(cookie(UserRole.ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/me/programs"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/me/purchases"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotExposeDraftsEvenToAdminThroughPublicRoutes() throws Exception {
        Program draft = saveProgram("Private admin draft", 8900L, false);
        Cookie admin = cookie(UserRole.ADMIN);
        mockMvc.perform(get("/api/programs").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mockMvc.perform(get("/api/programs/{programId}", draft.getId()).cookie(admin))
                .andExpect(status().isNotFound());
    }

    private void expectNotFound(UUID id) throws Exception {
        mockMvc.perform(get("/api/programs/{programId}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Program not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Program not found with: " + id));
    }

    private Program saveProgram(String name, Long priceCents, boolean published) {
        Program program = new Program(name, "Description for " + name);
        program.updatePrice(priceCents);
        if (published) program.publish();
        return programRepository.saveAndFlush(program);
    }

    private Cookie cookie(UserRole role) {
        User user = userRepository.saveAndFlush(new User(
                UUID.randomUUID() + "@example.com", "unused-password-hash", role
        ));
        return new Cookie("AUTH_TOKEN", jwtService.generateToken(user.getId()));
    }
}
