package com.fitnessplatform.access;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgramAccessExceptionHandler{

    @ExceptionHandler(
            ProgramAccessNotFoundException.class
    )
    ProblemDetail handleProgramAccessNotFound(
            ProgramAccessNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Program access not found");

        return problem;
    }

    @ExceptionHandler(
            ProgramAccessInvalidPeriodException.class
    )
    ProblemDetail handleInvalidPeriod(
            ProgramAccessInvalidPeriodException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );

        problem.setTitle(
                "Invalid program access period"
        );

        return problem;
    }

    @ExceptionHandler(
            ProgramAccessDeniedException.class
    )
    ProblemDetail handleProgramAccessDenied(
            ProgramAccessDeniedException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        exception.getMessage()
                );

        problem.setTitle("Program access denied");

        return problem;
    }
}
