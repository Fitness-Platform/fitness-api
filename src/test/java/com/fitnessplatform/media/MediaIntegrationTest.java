package com.fitnessplatform.media;

import com.fitnessplatform.TestcontainersConfiguration;
import com.fitnessplatform.access.*;
import com.fitnessplatform.auth.JwtService;
import com.fitnessplatform.exercise.*;
import com.fitnessplatform.program.*;
import com.fitnessplatform.user.*;
import com.fitnessplatform.workout.*;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MediaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired MediaService media;
    @Autowired MediaUploadRepository uploads;
    @Autowired ExerciseService exercises;
    @Autowired ExerciseRepository exerciseRepository;
    @Autowired ProgramResourceService resources;
    @Autowired ProgramResourceRepository resourceRepository;
    @Autowired ProgramService programs;
    @Autowired ProgramRepository programRepository;
    @Autowired ProgramWeekRepository weeks;
    @Autowired WorkoutRepository workouts;
    @Autowired WorkoutExerciseRepository workoutExercises;
    @Autowired ProgramWeekWorkoutRepository associations;
    @Autowired ProgramAccessRepository accesses;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean MediaStorage storage;
    private User admin;
    private User member;
    private Program program;

    @BeforeEach void setup() {
        admin = users.saveAndFlush(new User("admin-media@example.test", "not-used", UserRole.ADMIN));
        member = users.saveAndFlush(new User("member-media@example.test", "not-used", UserRole.USER));
        program = new Program("Media program", "Private content");
        program.publish();
        program = programRepository.saveAndFlush(program);
        when(storage.presignPut(anyString(), anyString(), anyString(), any()))
                .thenReturn(new MediaStorage.UploadUrl("https://r2.example.test/temporary-put", Map.of("Content-Type", "video/mp4")));
        when(storage.inspect(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return uploads.findAll().stream()
                    .filter(m -> m.getStorageKey().equals(key) || m.getStagingKey().equals(key))
                    .map(m -> new MediaStorage.ObjectMetadata(m.getSizeBytes(), m.getContentType(), m.getId().toString(), "etag"))
                    .findFirst();
        });
    }
    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.execute("TRUNCATE TABLE users, programs, workouts, exercises, media_uploads CASCADE");
    }
    Cookie cookie(User user) { return new Cookie("AUTH_TOKEN", jwt.generateToken(user.getId())); }
    void asAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getId(), null, List.of()));
    }
    MediaUpload verified(MediaPurpose purpose) {
        var response = media.authorize(admin.getId(), new MediaUploadRequest(purpose,
                purpose == MediaPurpose.EXERCISE_VIDEO ? "lesson.mp4" : "guide.pdf",
                purpose == MediaPurpose.EXERCISE_VIDEO ? "video/mp4" : "application/pdf", 128L));
        media.complete(admin.getId(), response.uploadId());
        return uploads.findById(response.uploadId()).orElseThrow();
    }
    Exercise attachVideo(MediaUpload upload) {
        asAdmin();
        return exercises.create(new ExerciseRequest("Squat", null, "Dumbbells", null, upload.getId()));
    }
    String authorizeBody() {
        return """ 
                {"purpose":"EXERCISE_VIDEO","originalFilename":"lesson.mp4","contentType":"video/mp4","sizeBytes":128}
                """;
    }

    @Test void flywayUpgradePreservesExternalRowsAndEnforcesExclusiveSources() {
        String schema = "media_upgrade";
        javax.sql.DataSource dataSource = jdbc.getDataSource();
        try {
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema)
                    .target("19").load().migrate();
            UUID exerciseId = UUID.randomUUID();
            UUID programId = UUID.randomUUID();
            UUID resourceId = UUID.randomUUID();
            jdbc.update("INSERT INTO media_upgrade.exercises(id,name,video_url) VALUES (?,?,?)",
                    exerciseId, "Existing exercise", "https://external.test/original-video");
            jdbc.update("INSERT INTO media_upgrade.programs(id,name) VALUES (?,?)", programId, "Existing program");
            jdbc.update("INSERT INTO media_upgrade.program_resources(id,program_id,title,url,position) VALUES (?,?,?,?,?)",
                    resourceId, programId, "Existing PDF", "https://external.test/original.pdf", 1);
            org.flywaydb.core.Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            assertEquals("https://external.test/original-video", jdbc.queryForObject(
                    "SELECT video_url FROM media_upgrade.exercises WHERE id=?", String.class, exerciseId));
            assertEquals("https://external.test/original.pdf", jdbc.queryForObject(
                    "SELECT url FROM media_upgrade.program_resources WHERE id=?", String.class, resourceId));
            assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                    () -> jdbc.update("UPDATE media_upgrade.program_resources SET url=NULL WHERE id=?", resourceId));
            assertEquals("20", jdbc.queryForObject(
                    "SELECT version FROM media_upgrade.flyway_schema_history WHERE success AND version='20'", String.class));
        } finally {
            jdbc.execute("DROP SCHEMA IF EXISTS media_upgrade CASCADE");
        }
    }
    @Test void uploadEndpointsRequireAdminAndCsrfAndNeverReturnCredentials() throws Exception {
        mvc.perform(post("/api/admin/media/uploads").with(csrf()).contentType("application/json").content(authorizeBody()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(member)).with(csrf())
                        .contentType("application/json").content(authorizeBody())).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin))
                        .contentType("application/json").content(authorizeBody())).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin)).with(csrf())
                        .contentType("application/json").content(authorizeBody()))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.uploadId").exists()).andExpect(jsonPath("$.uploadUrl").exists())
                .andExpect(jsonPath("$.requiredHeaders").isMap()).andExpect(jsonPath("$.expiresAt").exists())
                .andExpect(jsonPath("$.secretAccessKey").doesNotExist()).andExpect(jsonPath("$.storageKey").doesNotExist());
    }
    @Test void completionRequiresAdminCsrfAndUploadOwner() throws Exception {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        String path = "/api/admin/media/uploads/" + upload.getId() + "/complete";
        mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(post(path).cookie(cookie(member)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(path).cookie(cookie(admin))).andExpect(status().isForbidden());
        var other = users.saveAndFlush(new User("other-admin@example.test", "unused", UserRole.ADMIN));
        mvc.perform(post(path).cookie(cookie(other)).with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(post(path).cookie(cookie(admin)).with(csrf())).andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("UPLOADED")).andExpect(jsonPath("$.originalFilename").value("lesson.mp4"));
    }
    @Test void invalidMetadataAndProviderErrorsHaveSafeResponses() throws Exception {
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content(authorizeBody().replace("video/mp4", "text/html")))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_TYPE"));
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content(authorizeBody().replace("128", "104857601")))
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content(authorizeBody().replace("128", "0"))).andExpect(status().isBadRequest());
        when(storage.presignPut(any(), any(), any(), any())).thenThrow(new MediaException(MediaException.Reason.UNAVAILABLE));
        mvc.perform(post("/api/admin/media/uploads").cookie(cookie(admin)).with(csrf()).contentType("application/json").content(authorizeBody()))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("UNAVAILABLE"));
    }
    @Test void pendingAndWrongPurposeCannotAttachAndBothSourcesRejected() throws Exception {
        var pending = media.authorize(admin.getId(), new MediaUploadRequest(MediaPurpose.EXERCISE_VIDEO, "x.mp4", "video/mp4", 128L));
        String body = "{\"name\":\"Squat\",\"videoMediaId\":\"" + pending.uploadId() + "\"}";
        mvc.perform(post("/api/admin/exercises").cookie(cookie(admin)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("NOT_COMPLETED"));
        var wrong = verified(MediaPurpose.PROGRAM_RESOURCE);
        mvc.perform(post("/api/admin/exercises").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content(body.replace(pending.uploadId().toString(), wrong.getId().toString())))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/exercises").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content(body.replace("\"name\":", "\"videoUrl\":\"https://external.test/video\",\"name\":")))
                .andExpect(status().isBadRequest());
    }
    @Test void exerciseReferenceStoredAndAdminReceivesOnlyMetadata() throws Exception {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        mvc.perform(post("/api/admin/exercises").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Squat\",\"videoMediaId\":\"" + upload.getId() + "\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.videoUrl").value(nullValue()))
                .andExpect(jsonPath("$.uploadedMedia.id").value(upload.getId().toString()))
                .andExpect(jsonPath("$.uploadedMedia.contentType").value("video/mp4"))
                .andExpect(jsonPath("$.uploadedMedia.storageKey").doesNotExist());
        assertEquals(MediaUpload.State.ATTACHED, uploads.findById(upload.getId()).orElseThrow().getState());
        mvc.perform(post("/api/admin/exercises").cookie(cookie(admin)).with(csrf()).contentType("application/json")
                        .content("{\"name\":\"Other\",\"videoMediaId\":\"" + upload.getId() + "\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void replacingMediaDeletesOldOnlyAfterCommitAndRollbackPreservesIt() {
        var old = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(old);
        var replacement = verified(MediaPurpose.EXERCISE_VIDEO);
        clearInvocations(storage);
        var tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status -> {
            exercises.update(exercise.getId(), new ExerciseRequest("Squat", null, null, null, replacement.getId()));
            verify(storage, never()).delete(old.getStorageKey());
            status.setRollbackOnly();
        });
        verify(storage, never()).delete(old.getStorageKey());
        assertEquals(old.getId(), exerciseRepository.findById(exercise.getId()).orElseThrow().getVideoMedia().getId());
        assertEquals(MediaUpload.State.VERIFIED, uploads.findById(replacement.getId()).orElseThrow().getState());
        tx.executeWithoutResult(status -> {
            exercises.update(exercise.getId(), new ExerciseRequest("Squat", null, null, null, replacement.getId()));
            verify(storage, never()).delete(old.getStorageKey());
        });
        verify(storage).delete(old.getStorageKey());
        assertEquals(replacement.getId(), exerciseRepository.findById(exercise.getId()).orElseThrow().getVideoMedia().getId());
    }
    @Test void removalAndExerciseDeletionCleanOwnedObjectsWhileExternalUrlsNeverDeleteR2() {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(upload);
        clearInvocations(storage);
        asAdmin();
        exercises.update(exercise.getId(), new ExerciseRequest("Squat", null, null, null));
        assertNull(exerciseRepository.findById(exercise.getId()).orElseThrow().getVideoMedia());
        verify(storage).delete(upload.getStorageKey());
        clearInvocations(storage);
        var external = exercises.create(new ExerciseRequest("External", null, null, "https://external.test/video"));
        exercises.delete(external.getId());
        verify(storage, never()).delete(any());
        var another = verified(MediaPurpose.EXERCISE_VIDEO);
        var owned = attachVideo(another);
        exercises.delete(owned.getId());
        verify(storage).delete(another.getStorageKey());
    }
    @Test void failedExerciseDeletionDoesNotDeleteMedia() {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(upload);
        var workout = workouts.saveAndFlush(new Workout("Training", null));
        workoutExercises.saveAndFlush(new WorkoutExercise(workout, exercise, 1, "10", null, 30, null, 1));
        clearInvocations(storage);
        assertThrows(ExerciseInUseException.class, () -> exercises.delete(exercise.getId()));
        verify(storage, never()).delete(any());
        assertEquals(MediaUpload.State.ATTACHED, uploads.findById(upload.getId()).orElseThrow().getState());
    }
    @Test void resourceExternalAndUploadedModesReplaceAndDeleteSafely() throws Exception {
        var external = resources.create(program.getId(), new ProgramResourceRequest("Guide", null, "https://external.test/guide", 1));
        var upload = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        resources.update(program.getId(), external.getId(), new ProgramResourceRequest("Guide", null, null, 1, upload.getId()));
        mvc.perform(get("/api/admin/programs/" + program.getId() + "/resources").cookie(cookie(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].uploadedMedia.originalFilename").value("guide.pdf"))
                .andExpect(jsonPath("$[0].url").value(nullValue()));
        clearInvocations(storage);
        resources.update(program.getId(), external.getId(), new ProgramResourceRequest("Guide", null, "https://external.test/new", 1));
        verify(storage).delete(upload.getStorageKey());
        clearInvocations(storage);
        resources.delete(program.getId(), external.getId());
        verify(storage, never()).delete(any());
        var owned = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        var resource = resources.create(program.getId(), new ProgramResourceRequest("PDF", null, null, 1, owned.getId()));
        resources.delete(program.getId(), resource.getId());
        verify(storage).delete(owned.getStorageKey());
    }
    @Test void resourcePositionConflictRollsBackNewReferenceAndPreservesOldObject() {
        var old = verified(MediaPurpose.PROGRAM_RESOURCE);
        var replacement = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        var resource = resources.create(program.getId(), new ProgramResourceRequest("PDF", null, null, 1, old.getId()));
        resources.create(program.getId(), new ProgramResourceRequest("Other", null, "https://external.test", 2));
        clearInvocations(storage);
        assertThrows(ProgramResourcePositionConflictException.class,
                () -> resources.update(program.getId(), resource.getId(), new ProgramResourceRequest("PDF", null, null, 2, replacement.getId())));
        verify(storage, never()).delete(any());
        assertEquals(MediaUpload.State.ATTACHED, uploads.findById(old.getId()).orElseThrow().getState());
        assertEquals(MediaUpload.State.VERIFIED, uploads.findById(replacement.getId()).orElseThrow().getState());
    }
    @Test void deletingProgramCleansItsOwnedResourceObjectsAfterCommit() {
        var upload = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        resources.create(program.getId(), new ProgramResourceRequest("PDF", null, null, 1, upload.getId()));
        clearInvocations(storage);
        programs.delete(program.getId());
        verify(storage).delete(upload.getStorageKey());
        assertTrue(resourceRepository.findAll().isEmpty());
    }
    @Test void cleanupFailureDoesNotUndoDomainUpdateAndRemainsRetryable() {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(upload);
        doThrow(new MediaException(MediaException.Reason.UNAVAILABLE)).when(storage).delete(upload.getStorageKey());
        exercises.update(exercise.getId(), new ExerciseRequest("Squat", null, null, null));
        assertNull(exerciseRepository.findById(exercise.getId()).orElseThrow().getVideoMedia());
        assertEquals(MediaUpload.State.RETIRED, uploads.findById(upload.getId()).orElseThrow().getState());
    }
    @Test void authorizedMemberGetsSignedResourcesDeniedOrDraftAccessCannotSign() throws Exception {
        var upload = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        resources.create(program.getId(), new ProgramResourceRequest("Private", null, null, 1, upload.getId()));
        resources.create(program.getId(), new ProgramResourceRequest("External", null, "https://external.test/guide", 2));
        clearInvocations(storage);
        String path = "/api/me/programs/" + program.getId();
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
        var access = accesses.saveAndFlush(new ProgramAccess(member, program, Instant.now().minusSeconds(60), null));
        when(storage.presignGet(upload.getStorageKey(), Duration.ofMinutes(10))).thenReturn("https://r2.example.test/private-signed-get");
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(jsonPath("$.resources[0].url").value("https://r2.example.test/private-signed-get"))
                .andExpect(jsonPath("$.resources[1].url").value("https://external.test/guide"))
                .andExpect(jsonPath("$.resources[0].storageKey").doesNotExist())
                .andExpect(jsonPath("$.resources[0].uploadedMedia").doesNotExist());
        clearInvocations(storage);
        access.revoke(Instant.now()); accesses.saveAndFlush(access);
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
    }
    @Test void exerciseMemberSigningFollowsAccessAndWeekUnlockChecks() throws Exception {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(upload);
        var external = exercises.create(new ExerciseRequest("External", null, null, "https://external.test/video"));
        var week = weeks.saveAndFlush(new ProgramWeek(program, "Week 1", null, 1));
        var lockedWeek = weeks.saveAndFlush(new ProgramWeek(program, "Week 2", null, 2));
        var workout = workouts.saveAndFlush(new Workout("Training", null));
        var association = associations.saveAndFlush(new ProgramWeekWorkout(week, workout, 1));
        var locked = associations.saveAndFlush(new ProgramWeekWorkout(lockedWeek, workout, 1));
        workoutExercises.saveAndFlush(new WorkoutExercise(workout, exercise, 1, "10", null, 30, null, 1));
        workoutExercises.saveAndFlush(new WorkoutExercise(workout, external, 1, "10", null, 30, null, 2));
        String base = "/api/me/programs/" + program.getId() + "/weeks/";
        String path = base + week.getId() + "/workouts/" + association.getId();
        clearInvocations(storage);
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
        accesses.saveAndFlush(new ProgramAccess(member, program, Instant.now().minusSeconds(60), null));
        mvc.perform(get(base + lockedWeek.getId() + "/workouts/" + locked.getId()).cookie(cookie(member)))
                .andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
        when(storage.presignGet(upload.getStorageKey(), Duration.ofMinutes(10))).thenReturn("https://r2.example.test/video-signed-get");
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].videoUrl").value("https://r2.example.test/video-signed-get"))
                .andExpect(jsonPath("$.exercises[1].videoUrl").value("https://external.test/video"))
                .andExpect(jsonPath("$.exercises[0].storageKey").doesNotExist());
    }

    @Test void oversizedStoredObjectCannotCompleteOrAttach() throws Exception {
        var authorization = media.authorize(admin.getId(), new MediaUploadRequest(
                MediaPurpose.EXERCISE_VIDEO, "small.mp4", "video/mp4", 128L));
        var upload = uploads.findById(authorization.uploadId()).orElseThrow();
        when(storage.inspect(upload.getStagingKey())).thenReturn(Optional.of(new MediaStorage.ObjectMetadata(
                104857601L, "video/mp4", upload.getId().toString(), "oversized-etag")));
        mvc.perform(post("/api/admin/media/uploads/" + upload.getId() + "/complete")
                        .cookie(cookie(admin)).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VERIFICATION_FAILED"));
        verify(storage).delete(upload.getStagingKey());
        verify(storage, never()).copyVerified(any(), any(), any());
        asAdmin();
        assertEquals(MediaException.Reason.NOT_COMPLETED,
                assertThrows(MediaException.class, () -> exercises.create(
                        new ExerciseRequest("Squat", null, null, null, upload.getId()))).reason());
        assertTrue(exerciseRepository.findAll().isEmpty());
    }

    @Test void accessToProgramADoesNotSignProgramBAndExpiredOrDraftAccessCannotSign() throws Exception {
        var upload = verified(MediaPurpose.PROGRAM_RESOURCE);
        asAdmin();
        resources.create(program.getId(), new ProgramResourceRequest("Private B", null, null, 1, upload.getId()));
        var programA = new Program("Program A", null);
        programA.publish();
        programA = programRepository.saveAndFlush(programA);
        accesses.saveAndFlush(new ProgramAccess(member, programA, Instant.now().minusSeconds(60), null));
        clearInvocations(storage);
        String path = "/api/me/programs/" + program.getId();
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
        var expired = accesses.saveAndFlush(new ProgramAccess(member, program,
                Instant.now().minusSeconds(3600), Instant.now().minusSeconds(60)));
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
        accesses.delete(expired);
        accesses.saveAndFlush(new ProgramAccess(member, program, Instant.now().minusSeconds(60), null));
        programs.moveToDraft(program.getId());
        mvc.perform(get(path).cookie(cookie(member))).andExpect(status().isForbidden());
        verify(storage, never()).presignGet(any(), any());
    }

    @Test void concurrentAttachmentConsumesVerifiedReferenceOnlyOnce() throws Exception {
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> attach = () -> {
                asAdmin();
                try {
                    ready.countDown();
                    assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    try {
                        exercises.create(new ExerciseRequest("Concurrent", null, null, null, upload.getId()));
                        return true;
                    } catch (MediaException exception) {
                        assertEquals(MediaException.Reason.INVALID_REFERENCE, exception.reason());
                        return false;
                    }
                } finally { SecurityContextHolder.clearContext(); }
            };
            var first = workers.submit(attach);
            var second = workers.submit(attach);
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            assertNotEquals(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(20, java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(1, exerciseRepository.count());
        assertEquals(MediaUpload.State.ATTACHED, uploads.findById(upload.getId()).orElseThrow().getState());
    }

    @Test void concurrentReplacementsKeepOnlyFinalReferenceAttached() throws Exception {
        var old = verified(MediaPurpose.EXERCISE_VIDEO);
        var exercise = attachVideo(old);
        var firstUpload = verified(MediaPurpose.EXERCISE_VIDEO);
        var secondUpload = verified(MediaPurpose.EXERCISE_VIDEO);
        var ready = new java.util.concurrent.CountDownLatch(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        clearInvocations(storage);
        try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.function.Function<UUID, java.util.concurrent.Callable<Void>> replace = id -> () -> {
                asAdmin();
                try {
                    ready.countDown();
                    assertTrue(start.await(10, java.util.concurrent.TimeUnit.SECONDS));
                    exercises.update(exercise.getId(), new ExerciseRequest("Squat", null, null, null, id));
                    return null;
                } finally { SecurityContextHolder.clearContext(); }
            };
            var first = workers.submit(replace.apply(firstUpload.getId()));
            var second = workers.submit(replace.apply(secondUpload.getId()));
            assertTrue(ready.await(10, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            second.get(20, java.util.concurrent.TimeUnit.SECONDS);
        }
        UUID finalId = exerciseRepository.findById(exercise.getId()).orElseThrow().getVideoMedia().getId();
        assertTrue(Set.of(firstUpload.getId(), secondUpload.getId()).contains(finalId));
        var attached = uploads.findAll().stream().filter(m -> m.getState() == MediaUpload.State.ATTACHED).toList();
        assertEquals(1, attached.size());
        assertEquals(finalId, attached.getFirst().getId());
        var displaced = finalId.equals(firstUpload.getId()) ? secondUpload : firstUpload;
        verify(storage).delete(old.getStorageKey());
        verify(storage).delete(displaced.getStorageKey());
        verify(storage, never()).delete(attached.getFirst().getStorageKey());
    }

    @Test void exerciseCanSwitchExternalToUploadedAndBackWithoutDeletingExternalUrls() {
        var exercise = exercises.create(new ExerciseRequest("External", null, null, "https://external.test/original"));
        var upload = verified(MediaPurpose.EXERCISE_VIDEO);
        asAdmin();
        clearInvocations(storage);
        exercises.update(exercise.getId(), new ExerciseRequest("Uploaded", null, null, null, upload.getId()));
        var uploaded = exerciseRepository.findById(exercise.getId()).orElseThrow();
        assertNull(uploaded.getVideoUrl());
        assertEquals(upload.getId(), uploaded.getVideoMedia().getId());
        verify(storage, never()).delete(any());
        exercises.update(exercise.getId(), new ExerciseRequest("External again", null, null, "https://external.test/new"));
        var external = exerciseRepository.findById(exercise.getId()).orElseThrow();
        assertEquals("https://external.test/new", external.getVideoUrl());
        assertNull(external.getVideoMedia());
        verify(storage).delete(upload.getStorageKey());
    }
}
