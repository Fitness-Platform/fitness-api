CREATE TABLE media_uploads (
    id UUID PRIMARY KEY,
    uploader_id UUID NOT NULL REFERENCES users(id),
    purpose VARCHAR(30) NOT NULL CHECK (purpose IN ('EXERCISE_VIDEO', 'PROGRAM_RESOURCE')),
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    staging_key VARCHAR(255) NOT NULL UNIQUE,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    state VARCHAR(20) NOT NULL CHECK (state IN ('PENDING', 'VERIFIED', 'ATTACHED', 'RETIRED')),
    staging_cleaned BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    upload_expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_media_uploads_cleanup ON media_uploads(state, created_at);
CREATE INDEX idx_media_staging_cleanup ON media_uploads(upload_expires_at)
    WHERE state = 'ATTACHED' AND NOT staging_cleaned;

ALTER TABLE exercises ADD COLUMN video_media_id UUID UNIQUE REFERENCES media_uploads(id);
ALTER TABLE exercises ADD CONSTRAINT chk_exercise_media_source
    CHECK (video_media_id IS NULL OR video_url IS NULL);

ALTER TABLE program_resources ALTER COLUMN url DROP NOT NULL;
ALTER TABLE program_resources ADD COLUMN media_id UUID UNIQUE REFERENCES media_uploads(id);
ALTER TABLE program_resources ADD CONSTRAINT chk_resource_media_source
    CHECK ((url IS NOT NULL AND media_id IS NULL) OR (url IS NULL AND media_id IS NOT NULL));
