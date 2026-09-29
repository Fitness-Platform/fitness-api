package com.fitnessplatform.program;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgramExceptionHandler {

    @ExceptionHandler(ProgramNotFoundException.class)
    ProblemDetail handleProgramNotFound(
            ProgramNotFoundException exception
    ) {
        ProblemDetail problem=
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Program not found");

        return problem;
    }
}
