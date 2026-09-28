package com.fitnessplatform.exercise;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExerciseExceptionHandler {

    @ExceptionHandler(ExerciseNotFoundException.class)
    ProblemDetail handleExerciseNotFound(
            ExerciseNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Exercise not found");

        return problem;
    }

    @ExceptionHandler(ExerciseInUseException.class)
    ProblemDetail handleExerciseInUse(
            ExerciseInUseException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Exercise is in use"
        );

        return problem;
    }
}
