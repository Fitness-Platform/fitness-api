package com.fitnessplatform.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, UUID> {

    List<WorkoutExercise>
    findAllByWorkoutIdOrderByPositionAsc(
            UUID workoutId
    );

    Optional<WorkoutExercise>
    findByIdAndWorkoutId(
            UUID id,
            UUID workoutId
    );

    boolean existsByWorkoutIdAndPosition(
            UUID workoutId,
            Integer position
    );

    boolean existsByWorkoutIdAndPositionAndIdNot(
            UUID workoutId,
            Integer position,
            UUID id
    );
}
