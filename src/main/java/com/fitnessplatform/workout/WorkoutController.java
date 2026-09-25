package com.fitnessplatform.workout;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(
            WorkoutService workoutService
    ) {
        this.workoutService = workoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse create(
            @Valid @RequestBody WorkoutRequest request
    ) {
        Workout workout =
                workoutService.create(request);

        return WorkoutResponse.from(workout);
    }

    @GetMapping
    public List<WorkoutResponse> findAll() {
        return workoutService.findAll()
                .stream()
                .map(WorkoutResponse::from)
                .toList();
    }

    @GetMapping("/{workoutId}")
    public WorkoutResponse findById(
            @PathVariable UUID workoutId
    ) {
        return WorkoutResponse.from(
                workoutService.findById(workoutId)
        );
    }

    @PutMapping("/{workoutId}")
    public WorkoutResponse update(
            @PathVariable UUID workoutId,
            @Valid @RequestBody WorkoutRequest request
    ) {
        Workout workout = workoutService.update(
                workoutId,
                request
        );

        return WorkoutResponse.from(workout);
    }

    @DeleteMapping("/{workoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID workoutId
    ) {
        workoutService.delete(workoutId);
    }
}
