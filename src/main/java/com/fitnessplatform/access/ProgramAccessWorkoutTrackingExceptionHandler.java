package com.fitnessplatform.access;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgramAccessWorkoutTrackingExceptionHandler {

    @ExceptionHandler(
            InvalidProgramAccessWorkoutSetException.class
    )
    public ResponseEntity<ProblemDetail> handleInvalidWorkoutSet(
            InvalidProgramAccessWorkoutSetException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle(
                "Invalid workout set"
        );

        return ResponseEntity
                .badRequest()
                .body(problem);
    }

    @ExceptionHandler(
            ProgramAccessWorkoutCompletedException.class
    )
    public ResponseEntity<ProblemDetail> handleCompletedWorkout(
            ProgramAccessWorkoutCompletedException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Workout already completed"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }

    @ExceptionHandler(
            ProgramAccessWorkoutIncompleteException.class
    )
    public ResponseEntity<ProblemDetail> handleIncompleteWorkout(
            ProgramAccessWorkoutIncompleteException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Workout is incomplete"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }
}