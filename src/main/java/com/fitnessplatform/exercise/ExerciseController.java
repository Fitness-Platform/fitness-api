package com.fitnessplatform.exercise;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(
            ExerciseService exerciseService
    ) {
        this.exerciseService = exerciseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseResponse create(
            @Valid @RequestBody ExerciseRequest request
    ) {
        Exercise exercise =
                exerciseService.create(request);

        return ExerciseResponse.from(exercise);
    }

    @GetMapping
    public List<ExerciseResponse> findAll() {
        return exerciseService
                .findAll()
                .stream()
                .map(ExerciseResponse::from)
                .toList();
    }

    @GetMapping("/{exerciseId}")
    public ExerciseResponse findById(
            @PathVariable UUID exerciseId
    ) {
        return ExerciseResponse.from(
                exerciseService.findById(exerciseId)
        );
    }

    @PutMapping("/{exerciseId}")
    public ExerciseResponse update(
            @PathVariable UUID exerciseId,
            @Valid @RequestBody ExerciseRequest request
    ) {
        Exercise exercise =
                exerciseService.update(
                        exerciseId,
                        request
                );

        return ExerciseResponse.from(exercise);
    }

    @DeleteMapping("/{exerciseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID exerciseId
    ) {
        exerciseService.delete(exerciseId);
    }
}
