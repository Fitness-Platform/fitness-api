CREATE TABLE program_access_workouts (
    id UUID PRIMARY KEY,

    program_access_id UUID NOT NULL,
    program_week_workout_id UUID NOT NULL,

    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_program_access_workouts_program_access
        FOREIGN KEY (program_access_id)
        REFERENCES program_accesses(id),

    CONSTRAINT fk_program_access_workouts_program_week_workout
        FOREIGN KEY (program_week_workout_id)
        REFERENCES program_week_workouts(id),

    CONSTRAINT uk_program_access_workout
        UNIQUE (
            program_access_id,
            program_week_workout_id
        ),

    CONSTRAINT chk_program_access_workout_completion
        CHECK (
            completed_at IS NULL
            OR completed_at >= started_at
        )
);

CREATE INDEX idx_program_access_workouts_program_access
    ON program_access_workouts(program_access_id);

CREATE INDEX idx_program_access_workouts_program_week_workout
    ON program_access_workouts(program_week_workout_id);


CREATE TABLE program_access_workout_sets (
     id UUID PRIMARY KEY,

     program_access_workout_id UUID NOT NULL,
     workout_exercise_id UUID NOT NULL,

     set_number INTEGER NOT NULL,
     reps INTEGER NOT NULL,
     weight_lb NUMERIC(8, 2) NOT NULL,

     completed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT fk_program_access_workout_sets_program_access_workout
         FOREIGN KEY (program_access_workout_id)
         REFERENCES program_access_workouts(id),

     CONSTRAINT fk_program_access_workout_sets_workout_exercise
         FOREIGN KEY (workout_exercise_id)
         REFERENCES workout_exercises(id),

     CONSTRAINT uk_program_access_workout_set
         UNIQUE (
            program_access_workout_id,
            workout_exercise_id,
            set_number
         ),

     CONSTRAINT chk_program_access_workout_set_number
         CHECK (set_number > 0),

     CONSTRAINT chk_program_access_workout_set_reps
         CHECK (reps >= 0),

     CONSTRAINT chk_program_access_workout_set_weight
         CHECK (weight_lb >= 0)
);

CREATE INDEX idx_program_access_workout_sets_workout
    ON program_access_workout_sets(program_access_workout_id);

CREATE INDEX idx_program_access_workout_sets_exercise
    ON program_access_workout_sets(workout_exercise_id);