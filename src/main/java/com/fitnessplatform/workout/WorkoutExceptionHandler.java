package com.fitnessplatform.workout;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class WorkoutExceptionHandler {

    @ExceptionHandler(WorkoutNotFoundException.class)
    ProblemDetail handleWorkoutNotFound(
            WorkoutNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Workout not found");

        return problem;
    }

    @ExceptionHandler(WorkoutExerciseNotFoundException.class)
    ProblemDetail handleWorkoutExerciseNotFound(
            WorkoutExerciseNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle(
                "Workout exercise not found"
        );

        return problem;
    }

    @ExceptionHandler(
            WorkoutExercisePositionConflictException.class
    )
    ProblemDetail handleWorkoutExercisePositionConflict(
            WorkoutExercisePositionConflictException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Workout exercise position conflict"
        );

        return problem;
    }
}