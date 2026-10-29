# Private media storage (Phase A)

Cloudflare R2 is accessed only by the backend through the AWS SDK's S3-compatible client. The bucket **must remain private** (no public `r2.dev` access or public custom domain). Existing external Exercise `videoUrl` and Resource `url` values still work without R2 configuration. This phase adds no frontend upload controls.

## Configuration

Supply environment variables to the backend (never to Vite or the browser):

| Variable | Purpose/default |
| --- | --- |
| `R2_ACCOUNT_ID` | Cloudflare account ID; endpoint is `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` |
| `R2_ACCESS_KEY_ID` | Bucket-scoped S3 API access key |
| `R2_SECRET_ACCESS_KEY` | Its secret |
| `R2_BUCKET_NAME` | Private bucket name |
| `R2_UPLOAD_TTL` | ISO-8601 duration, default `PT5M` |
| `R2_GET_TTL` | ISO-8601 duration, default `PT10M` |

TTLs are constrained to 1 second–15 minutes. Use a least-privilege R2 token restricted to this bucket with object read/write/delete permissions (HEAD, GET, PUT, COPY and DELETE). No bucket administration permission is needed. The SDK uses region `auto` and path-style addressing. Without complete configuration, storage operations return a sanitized `503 UNAVAILABLE`; external URL functionality still works. Do not enable HTTP wire/request logging or log signed URLs. They are temporary bearer credentials. SigV4 URLs necessarily contain the access-key identifier in `X-Amz-Credential`; the secret access key is never returned. Configuration and signed-URL DTO string representations are redacted; infrastructure access logs and wire/debug logging must also be configured to redact query strings.

## Upload protocol (Admin + existing cookie/CSRF protection)

1. `POST /api/admin/media/uploads` with JSON:
   ```json
   {"purpose":"EXERCISE_VIDEO","originalFilename":"lesson.mp4","contentType":"video/mp4","sizeBytes":123456}
   ```
   Returns `201` with `uploadId`, `uploadUrl`, `requiredHeaders` and `expiresAt`. No client-provided storage key is accepted. Names are display metadata only; random keys use `exercise-videos/` or `program-resources/` prefixes. Responses have `Cache-Control: no-store`.
2. Browser PUTs the file to `uploadUrl` **with exactly the returned required headers** (`Content-Type` and `x-amz-meta-upload-id`). Do not forward application cookies, CSRF headers or backend credentials to R2. The browser sends the binary body directly to R2; this API is not multipart.
3. `POST /api/admin/media/uploads/{uploadId}/complete` (no body) before authorization expires. The issuing Admin must complete it. Backend HEAD verifies existence, exact size, content type and upload-reference metadata. A conditional COPY freezes the inspected ETag under a separate final key; a second HEAD verifies the copy. The PUT URL can never overwrite that final key. Returns verified metadata (`id`, `sourceType: UPLOADED`, `originalFilename`, `contentType`, `sizeBytes`). Repeated completion of a verified/attached upload is idempotent.
4. Use `videoMediaId` in existing Admin Exercise create/update requests, or `mediaId` in existing Program Resource create/update requests. Pass the returned verified upload ID, not a key or URL. Only the issuing Admin may attach a new upload, only to its declared purpose, within 24 hours of issuance, and only to one entity. Other Admins may retain/edit an already attached reference on its existing entity. Admin responses add `uploadedMedia` with metadata; external fields stay null for uploaded media. No private keys or signed preview links are exposed in Admin responses.

Exercise accepts external `videoUrl`, uploaded `videoMediaId`, or neither. To remove uploaded Exercise media, send both as null. Resource requires exactly one of external `url` or uploaded `mediaId`. For updates retaining uploaded media, send its existing ID. Existing external URL request bodies need no additional fields. Sending an external URL and uploaded reference together is rejected.

| Purpose | Accepted exact MIME types | Maximum size |
| --- | --- | --- |
| `EXERCISE_VIDEO` | `video/mp4`, `video/webm` | 100 MiB = 104,857,600 bytes |
| `PROGRAM_RESOURCE` | `application/pdf`, `image/jpeg`, `image/png`, `image/webp` | 20 MiB = 20,971,520 bytes |

Size must be positive; limits include the boundary. The PUT grant does not bind Content-Length or enforce an ingress byte ceiling: an Admin holding the grant can temporarily store a larger staging object, but completion rejects and attempts to delete it, and it cannot be attached or signed for Member access. This is a staging cost/abuse limit, not a bypass of accepted-media limits. Declared size is checked before authorization and compared with actual R2 object length at completion/attachment. HEAD validates stored metadata, not file contents; this foundation does not provide malware scanning or codec/content inspection. Unsupported types/invalid sizes never receive a PUT grant.

## Browser CORS on the private bucket

Set allowed origins to the exact deployed frontend origin and local development origin as needed (e.g. `http://localhost:5173`). Allow `PUT`, `GET`, `HEAD`; allow request headers `Content-Type`, `x-amz-meta-upload-id`, `Range`; expose `ETag`, `Content-Length`, `Content-Type`, `Content-Range`, `Accept-Ranges` as needed for playback. Test real browser PUT and video range playback against the configured bucket before deployment. CORS does not grant public access and cannot replace signatures. Avoid wildcard origins for deployment.

## Protected reads and lifecycle

Existing Member access, published-program, parent-ID and week-unlock checks run **before** URL generation. Uploaded Exercise media is returned through existing `videoUrl`; uploaded Resources through existing `url`. External values remain unchanged. Member JSON and signed object responses are marked private/no-store. No separate raw-key field or upload metadata is added to Member DTOs. A signed GET URL necessarily includes its object path, but that path alone grants no access to the private bucket. Signed links expire; refetch protected content for a fresh URL. Already issued links remain valid until their short TTL even if access is subsequently revoked.

V20 adds a durable `media_uploads` lifecycle (PENDING → VERIFIED → ATTACHED → RETIRED), owner/purpose/metadata/generated keys/expiry. Nullable unique FKs connect Exercise and Resource to it. Exercise sources are mutually exclusive; Resource URL becomes nullable with exactly-one-source constraint. Existing rows and URLs are preserved.

Entity changes and lifecycle changes share a transaction; row locks prevent double attachment and conflicting replacements. Old private objects are retired only with a successful entity update, then deleted **after commit**. Failed validation/rollback leaves old media intact and a new upload reusable until expiry. External URLs never trigger storage deletion. Resource/Exercise deletion cleans owned media; deleting a Program also retires its resources' media. Storage cleanup failure does not undo a committed domain change: durable RETIRED rows are retried hourly.

Pending/unattached verified uploads older than 24 hours are cleaned hourly. Staging objects remain tracked until the PUT grant plus one hour has elapsed, covering normal grant reuse and in-flight uploads. The one-hour grace is not a provider-enforced upper bound on transfer duration: a very late-finishing PUT could recreate staging after cleanup; the staging-prefix lifecycle rules are the operational backstop. Attached final objects are never removed by the abandoned-upload sweep. Configure bucket lifecycle rules to expire the `exercise-videos/pending/` and `program-resources/pending/` prefixes as an additional operational backstop; do **not** apply that rule to verified objects. Normal domain writes/deletes must go through services so lifecycle events are maintained.

Errors use ProblemDetail with stable `code`: `UNSUPPORTED_TYPE` (415), `TOO_LARGE` (413), `INVALID_SIZE` / `INVALID_REFERENCE` / `VERIFICATION_FAILED` / `SOURCE_CONFLICT` / `SOURCE_REQUIRED` (400), `NOT_COMPLETED` / `EXPIRED` (409), `MISSING` (404), `UNAVAILABLE` (503). Provider details/credentials are discarded.

## Validation

Run `./mvnw clean test` with Java 21 and Docker. Storage is mocked in integration tests; no Cloudflare credentials/network are required. Offline presigner tests check endpoint, headers and TTLs. PostgreSQL tests cover authorization/CSRF, references, immutable verification, rollback/post-commit cleanup, Member access and compatibility. A Flyway upgrade test starts at V19 with existing external URL rows and migrates to V20. Live R2 bucket policy, CORS and provider compatibility need deployment-environment validation; automated tests do not provision or change Cloudflare infrastructure.

## Security review boundaries and failure cases

The review found no application endpoint accepting an object key for signing/deletion, no USER upload route, and no pre-verification attachment path. Runtime behavior was left unchanged during the review; only regression tests and this documentation were extended. All 46 scoped files are retained: 35 are required runtime/schema files and 11 support tests/documentation/configuration. No clearly unnecessary implementation artifact was found.

### Authorization and request surface

There are exactly two new application endpoints, both under the unchanged `/api/admin/**` ADMIN rule and normal CSRF protection:

| Method and route | Body | Response | Authorization |
| --- | --- | --- | --- |
| `POST /api/admin/media/uploads` | `purpose`, `originalFilename`, `contentType`, `sizeBytes` | 201: `uploadId`, `uploadUrl`, `requiredHeaders`, `expiresAt` | Authenticated ADMIN; uploader ID comes from the JWT principal |
| `POST /api/admin/media/uploads/{uploadId}/complete` | None | 200: `id`, `sourceType`, `originalFilename`, `contentType`, `sizeBytes` | ADMIN plus issuing-uploader ownership check |

Existing `POST /api/admin/exercises` and `PUT /api/admin/exercises/{exerciseId}` accept optional `videoMediaId`; existing `POST /api/admin/programs/{programId}/resources` and `PUT /api/admin/programs/{programId}/resources/{programResourceId}` accept optional `mediaId`. These retain ADMIN/CSRF requirements and their existing domain response fields plus `uploadedMedia` metadata. Existing domain DELETE endpoints retire only their database-owned media; no generic object-delete or sign-by-key endpoint exists.

Exercise flow: ADMIN authorizes → bearer PUT to a random staging key → same ADMIN completes → HEAD/conditional COPY/HEAD → same ADMIN attaches a verified EXERCISE_VIDEO reference → authenticated Member requests the workout → active user/program access, published status, week/program relationship, week unlock and workout/week relationship checks → signed GET in `videoUrl`.

Resource flow: ADMIN authorizes → bearer PUT → same ADMIN completes/verifies → same ADMIN attaches a verified PROGRAM_RESOURCE reference → Member requests the program → active user/program access and published status checks → signed GET in the resource `url`. Resources remain program-level content under the existing contract; they do not gain a week lock. R2 validates the temporary bearer signature rather than the application user role on direct transfers. Sharing a valid signed URL shares its temporary capability.

### Keys, limits and validation

Keys are exactly `<purpose-prefix>/pending/<random UUID>` and `<purpose-prefix>/verified/<different random UUID>`, where the prefix is `exercise-videos` or `program-resources`. Original filenames never become paths. HEAD always uses the row's backend-generated key, never a client key. Completion checks presence, exact declared byte length, exact allowed content-type metadata, upload-ID metadata and ETag; purpose is fixed and validated at authorization and checked again at attachment. A conditional source ETag COPY plus destination HEAD prevents reuse of the PUT URL from mutating an attached final object. Metadata is not a file-signature/content scanner.

| Failure case | Persistence and cleanup |
| --- | --- |
| Grant never used | PENDING row; hourly sweep removes it after 24 hours (missing-object DELETE is idempotent) |
| Uploaded, no completion | PENDING row and staging object; same 24-hour sweep |
| Metadata/size verification fails | Remains PENDING; offending staging or copied final object deletion attempted; failed deletion retried by abandoned-upload cleanup |
| COPY/provider failure or DB rollback during completion | Row remains PENDING; a possibly-created final object remains tracked by its deterministic row key until retry or abandonment cleanup |
| Domain update fails after verification | Domain reference and media state roll back; new object stays VERIFIED and can be retried within the 24-hour attachment window, otherwise swept |
| Old-object cleanup fails after commit | Successful domain change stays committed; RETIRED row remains for hourly retry |
| Process unavailable | Cleanup waits for a running scheduler/storage recovery; expiry is not immediate deletion |

There is no atomic transaction spanning R2 and PostgreSQL. Extremely late provider writes after the grace/sweep, manual deletion of tracking rows, or inconsistent storage/database restores can leave untracked objects; no general bucket-inventory reconciliation is implemented in this phase. Staging lifecycle rules cover late staging writes. Final-key orphans outside normal service-managed flows require operational reconciliation; do not expire all final keys because that would delete valid protected content.

Already-issued GET links remain usable until their TTL after access revocation. The application never makes the bucket public and does not require public access, but does not query/enforce the live bucket public-access setting: bucket privacy, least privilege and browser CORS require deployment validation. No live Cloudflare account was used for automated tests.

### Database and concurrency

`media_uploads` adds `id`, `uploader_id`, `purpose`, `original_filename`, `content_type`, `size_bytes`, `staging_key`, `storage_key`, `state`, `staging_cleaned`, `created_at`, `upload_expires_at`. All are non-null; `staging_cleaned` defaults false. The primary key, uploader FK, unique staging/final keys, positive size and purpose/state allowlists constrain rows. Indexes cover `(state, created_at)` and attached/un-cleaned staging expiry. `exercises.video_media_id` and `program_resources.media_id` are nullable unique FKs. Exercise forbids both sources; Resource URL becomes nullable and its check requires exactly one source. All prior rows initially have null media references, so previously valid external URL records satisfy the new checks without rewriting their URLs. V19-data upgrade and clean PostgreSQL startup are tested; DDL still takes normal PostgreSQL deployment locks and is not claimed to be zero-downtime on an unmeasured production database.

Completion is idempotent for VERIFIED/ATTACHED rows and restricted to the issuing Admin. A pessimistic upload-row lock plus ATTACHED state prevents a verified reference being consumed by two entities. Existing owning-entity row locks serialize replacements/removal; unique FKs provide a second constraint. Program deletion locks parent/resources before retirement. New real-PostgreSQL concurrency tests verify one winner for simultaneous attachment and one surviving attached reference after simultaneous replacements; no distributed lock infrastructure is added.

Configuration intentionally stays optional for existing external-only/local installations: absent or incomplete/invalid account configuration disables the provider and media operations return sanitized 503, rather than blocking all application startup. TTL validation fails binding for out-of-range durations. No production-required switch or bucket readiness probe is added; deployments requiring uploads must validate configuration with a real storage smoke test. CORS origins/headers/methods are specified above.

### Test responsibility

- `MediaServiceTest`: exact MIME/size boundaries, random keys, short TTLs, owner/purpose/state checks, missing/mismatched metadata, immutable verified copy, replay/attachment reuse, signed/external URL behavior, deferred retirement and configuration validation.
- `MediaCleanupTest`: attached-object retention, abandoned upload deletion, PUT-expiry grace, durable retry, one-time staging cleanup and idempotency.
- `R2MediaStorageTest`: offline private-endpoint signing, required headers, TTLs, URL/secret redaction and optional-configuration failure.
- `MediaIntegrationTest`: real security/CSRF, controller contracts, Flyway V19 preservation, domain attachment/replacement/removal, rollback/after-commit behavior, storage failure, oversized stored objects, external/uploaded mode transitions, Member allow/deny/revocation/expiry/draft/cross-program and week-lock checks, plus simultaneous attachment/replacement.
- Updated `ExerciseServiceTest`, `ProgramResourceServiceTest`, `ProgramServiceTest`: preserve existing external-URL/CRUD regressions while accounting for media dependency injection and mutation row locks.

### Exact file scope

`Required` means runtime behavior/schema needed for this foundation; `Supporting` means test, documentation or deployment configuration. Paths are relative to the backend repository. No file is classified unnecessary.

| Status | Classification | File |
| --- | --- | --- |
| Modified | Supporting | `.env.example` |
| Modified | Supporting | `pom.xml` |
| Modified | Required | `src/main/java/com/fitnessplatform/exercise/Exercise.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/exercise/ExerciseRepository.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/exercise/ExerciseRequest.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/exercise/ExerciseResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/exercise/ExerciseService.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberProgramController.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberProgramDetailResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberProgramResourceResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberProgramService.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberProgramWorkoutDetailResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/MemberWorkoutExerciseResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramRepository.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramResource.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramResourceRepository.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramResourceRequest.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramResourceResponse.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramResourceService.java` |
| Modified | Required | `src/main/java/com/fitnessplatform/program/ProgramService.java` |
| Modified | Supporting | `src/main/resources/application.properties` |
| Modified | Supporting | `src/test/java/com/fitnessplatform/exercise/ExerciseServiceTest.java` |
| Modified | Supporting | `src/test/java/com/fitnessplatform/program/ProgramResourceServiceTest.java` |
| Modified | Supporting | `src/test/java/com/fitnessplatform/program/ProgramServiceTest.java` |
| New | Supporting | `docs/media-storage.md` |
| New | Required | `src/main/java/com/fitnessplatform/media/AdminMediaController.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaCleanup.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaCleanupListener.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaConfiguration.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaException.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaExceptionHandler.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaMetadataResponse.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaPurpose.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaService.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaStorage.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaUpload.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaUploadRepository.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaUploadRequest.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/MediaUploadResponse.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/R2MediaStorage.java` |
| New | Required | `src/main/java/com/fitnessplatform/media/R2Properties.java` |
| New | Required | `src/main/resources/db/migration/V20__add_private_media_storage.sql` |
| New | Supporting | `src/test/java/com/fitnessplatform/media/MediaCleanupTest.java` |
| New | Supporting | `src/test/java/com/fitnessplatform/media/MediaIntegrationTest.java` |
| New | Supporting | `src/test/java/com/fitnessplatform/media/MediaServiceTest.java` |
| New | Supporting | `src/test/java/com/fitnessplatform/media/R2MediaStorageTest.java` |
