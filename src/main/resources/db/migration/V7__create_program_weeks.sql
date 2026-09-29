CREATE TABLE program_weeks (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_program_weeks_program
        FOREIGN KEY (program_id)
        REFERENCES programs(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_program_weeks_position
        UNIQUE (program_id, position),

    CONSTRAINT chk_program_weeks_position
        CHECK (position > 0)
);