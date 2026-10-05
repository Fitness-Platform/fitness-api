package com.fitnessplatform.access;

import com.fitnessplatform.workout.WorkoutExercise;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProgramAccessExerciseLoadService {

    private final ProgramAccessExerciseLoadRepository
            programAccessExerciseLoadRepository;

    public ProgramAccessExerciseLoadService(
            ProgramAccessExerciseLoadRepository
                    programAccessExerciseLoadRepository
    ) {
        this.programAccessExerciseLoadRepository =
                programAccessExerciseLoadRepository;
    }

    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> resolveCurrentWeights(
            ProgramAccess access,
            List<WorkoutExercise> workoutExercises
    ) {
        if (workoutExercises.isEmpty()) {
            return Map.of();
        }

        List<UUID> workoutExerciseIds =
                workoutExercises.stream()
                        .map(
                                WorkoutExercise::getId
                        )
                        .toList();

        Map<UUID, BigDecimal> overrides =
                new HashMap<>();

        programAccessExerciseLoadRepository
                .findAllByProgramAccessIdAndWorkoutExerciseIdIn(
                        access.getId(),
                        workoutExerciseIds
                )
                .forEach(
                        load ->
                                overrides.put(
                                        load
                                                .getWorkoutExercise()
                                                .getId(),
                                        load.getWeightLb()
                                )
                );

        Map<UUID, BigDecimal> currentWeights =
                new HashMap<>();

        for (
                WorkoutExercise workoutExercise
                : workoutExercises
        ) {
            BigDecimal currentWeight =
                    overrides.get(
                            workoutExercise.getId()
                    );

            if (currentWeight == null) {
                currentWeight =
                        workoutExercise
                                .getSuggestedWeightLb();
            }

            currentWeights.put(
                    workoutExercise.getId(),
                    currentWeight
            );
        }

        return currentWeights;
    }

    @Transactional
    public ProgramAccessExerciseLoad setWeight(
            ProgramAccess access,
            WorkoutExercise workoutExercise,
            BigDecimal weightLb
    ) {
        return programAccessExerciseLoadRepository
                .findByProgramAccessIdAndWorkoutExerciseId(
                        access.getId(),
                        workoutExercise.getId()
                )
                .map(
                        existingLoad -> {
                            existingLoad.updateWeight(
                                    weightLb
                            );

                            return existingLoad;
                        }
                )
                .orElseGet(
                        () ->
                                programAccessExerciseLoadRepository
                                        .save(
                                                new ProgramAccessExerciseLoad(
                                                        access,
                                                        workoutExercise,
                                                        weightLb
                                                )
                                        )
                );
    }
}