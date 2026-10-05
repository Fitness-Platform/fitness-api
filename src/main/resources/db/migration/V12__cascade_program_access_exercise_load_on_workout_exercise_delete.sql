ALTER TABLE program_access_exercise_loads
DROP CONSTRAINT fk_program_access_exercise_load_workout_exercise;

ALTER TABLE program_access_exercise_loads
    ADD CONSTRAINT fk_program_access_exercise_load_workout_exercise
        FOREIGN KEY (workout_exercise_id)
            REFERENCES workout_exercises(id)
            ON DELETE CASCADE;