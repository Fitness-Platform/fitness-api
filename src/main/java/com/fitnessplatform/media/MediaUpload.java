package com.fitnessplatform.media;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_uploads")
public class MediaUpload {
    public enum State { PENDING, VERIFIED, ATTACHED, RETIRED }
    @Id private UUID id;
    @Column(name = "uploader_id", nullable = false) private UUID uploaderId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MediaPurpose purpose;
    @Column(name = "original_filename", nullable = false, length = 255) private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 100) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "staging_key", nullable = false, unique = true) private String stagingKey;
    @Column(name = "storage_key", nullable = false, unique = true) private String storageKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private State state;
    @Column(name = "staging_cleaned", nullable = false) private boolean stagingCleaned;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "upload_expires_at", nullable = false) private Instant uploadExpiresAt;

    protected MediaUpload() {}
    public MediaUpload(UUID uploaderId, MediaPurpose purpose, String filename, String contentType,
                       long sizeBytes, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.uploaderId = uploaderId;
        this.purpose = purpose;
        this.originalFilename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.stagingKey = purpose.prefix() + "/pending/" + UUID.randomUUID();
        this.storageKey = purpose.prefix() + "/verified/" + UUID.randomUUID();
        this.state = State.PENDING;
        this.createdAt = now;
        this.uploadExpiresAt = expiresAt;
    }
    public UUID getId() { return id; }
    public UUID getUploaderId() { return uploaderId; }
    public MediaPurpose getPurpose() { return purpose; }
    public String getOriginalFilename() { return originalFilename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStagingKey() { return stagingKey; }
    public String getStorageKey() { return storageKey; }
    public State getState() { return state; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUploadExpiresAt() { return uploadExpiresAt; }
    public boolean isStagingCleaned() { return stagingCleaned; }
    void markStagingCleaned() { stagingCleaned = true; }
    void verified() { state = State.VERIFIED; }
    void attach() { state = State.ATTACHED; }
    void retire() { state = State.RETIRED; }
}
