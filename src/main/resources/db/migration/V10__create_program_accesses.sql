CREATE TABLE program_accesses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    program_id UUID NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_program_accesses_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_program_accesses_program
        FOREIGN KEY (program_id)
        REFERENCES programs(id),

    CONSTRAINT chk_program_accesses_expiration
        CHECK (
            expires_at IS NULL
            OR expires_at > starts_at
       )
);

CREATE INDEX idx_program_accesses_user
    ON program_accesses(user_id);

CREATE INDEX idx_program_accesses_program
    ON program_accesses(program_id);

CREATE INDEX idx_program_accesses_user_program
    ON program_accesses(user_id, program_id);