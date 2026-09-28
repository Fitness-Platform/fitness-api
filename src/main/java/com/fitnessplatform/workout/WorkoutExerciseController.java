package com.fitnessplatform.workout;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workouts/{workoutId}/exercises")
public class WorkoutExerciseController {

    private final WorkoutExerciseService workoutExerciseService;

    public WorkoutExerciseController(
            WorkoutExerciseService workoutExerciseService
    ) {
        this.workoutExerciseService =
                workoutExerciseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutExerciseResponse create(
            @PathVariable UUID workoutId,
            @Valid @RequestBody
            WorkoutExerciseCreateRequest request
    ) {
        WorkoutExercise workoutExercise =
                workoutExerciseService.create(
                        workoutId,
                        request
                );

        return WorkoutExerciseResponse.from(
                workoutExercise
        );
    }

    @GetMapping
    public List<WorkoutExerciseResponse> findAll(
            @PathVariable UUID workoutId
    ) {
        return workoutExerciseService
                .findAll(workoutId)
                .stream()
                .map(WorkoutExerciseResponse::from)
                .toList();
    }

    @GetMapping("/{workoutExerciseId}")
    public WorkoutExerciseResponse findById(
            @PathVariable UUID workoutId,
            @PathVariable UUID workoutExerciseId
    ) {
        return WorkoutExerciseResponse.from(
                workoutExerciseService.findById(
                        workoutId,
                        workoutExerciseId
                )
        );
    }

    @PutMapping("/{workoutExerciseId}")
    public WorkoutExerciseResponse update(
            @PathVariable UUID workoutId,
            @PathVariable UUID workoutExerciseId,
            @Valid @RequestBody
            WorkoutExerciseUpdateRequest request
    ) {
        WorkoutExercise workoutExercise =
                workoutExerciseService.update(
                        workoutId,
                        workoutExerciseId,
                        request
                );

        return WorkoutExerciseResponse.from(
                workoutExercise
        );
    }

    @DeleteMapping("/{workoutExerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID workoutId,
            @PathVariable UUID workoutExerciseId
    ) {
        workoutExerciseService.delete(
                workoutId,
                workoutExerciseId
        );
    }
}