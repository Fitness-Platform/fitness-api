CREATE TABLE program_access_exercise_loads (
    id UUID PRIMARY KEY,

    program_access_id UUID NOT NULL,
    workout_exercise_id UUID NOT NULL,

    weight_lb NUMERIC(8, 2) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_program_access_exercise_load_program_access
        FOREIGN KEY (program_access_id)
        REFERENCES program_accesses(id),

    CONSTRAINT fk_program_access_exercise_load_workout_exercise
        FOREIGN KEY (workout_exercise_id)
        REFERENCES workout_exercises(id),

    CONSTRAINT uk_program_access_exercise_load
        UNIQUE (
           program_access_id,
           workout_exercise_id
        ),

    CONSTRAINT chk_program_access_exercise_load_weight
        CHECK (weight_lb >= 0)
);

CREATE INDEX idx_program_access_exercise_load_program_access
    ON program_access_exercise_loads(program_access_id);

CREATE INDEX idx_program_access_exercise_load_workout_exercise
    ON program_access_exercise_loads(workout_exercise_id);