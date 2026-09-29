CREATE TABLE program_week_workouts (
    id UUID PRIMARY KEY,
    program_week_id UUID NOT NULL,
    workout_id UUID NOT NULL,
    position INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_program_week_workouts_program_week
        FOREIGN KEY (program_week_id)
        REFERENCES program_weeks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_program_week_workouts_workout
        FOREIGN KEY (workout_id)
        REFERENCES workouts(id),

    CONSTRAINT uq_program_week_workouts_position
        UNIQUE (program_week_id, position),

    CONSTRAINT chk_program_week_workouts_position
        CHECK (position > 0)
);