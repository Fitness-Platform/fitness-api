CREATE TABLE workout_exercises (
    id UUID PRIMARY KEY,

    workout_id UUID NOT NULL,
    exercise_id UUID NOT NULL,

    sets INTEGER NOT NULL,
    reps VARCHAR(50) NOT NULL,
    suggested_weight_lb NUMERIC(8, 2),
    rest_seconds INTEGER,
    notes TEXT,
    position INTEGER NOT NULL,

    CONSTRAINT fk_workout_exercises_workout
        FOREIGN KEY (workout_id)
        REFERENCES workouts(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_workout_exercises_exercise
        FOREIGN KEY (exercise_id)
        REFERENCES exercises(id),

    CONSTRAINT uq_workout_exercises_position
        UNIQUE (workout_id, position),

    CONSTRAINT chk_workout_exercises_sets
        CHECK (sets > 0),

    CONSTRAINT chk_workout_exercises_rest_seconds
        CHECK (
            rest_seconds IS NULL
            OR rest_seconds >= 0
        ),

    CONSTRAINT chk_workout_exercises_suggested_weight
        CHECK (
            suggested_weight_lb IS NULL
            OR suggested_weight_lb >= 0
        ),

    CONSTRAINT chk_workout_exercises_position
        CHECK (position > 0)
);