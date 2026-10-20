package com.fitnessplatform.program;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProgramWeekWorkoutDeleteExceptionHandler {

    @ExceptionHandler(
            ProgramWeekWorkoutInUseException.class
    )
    public ResponseEntity<ProblemDetail>
    handleProgramWeekWorkoutInUse(
            ProgramWeekWorkoutInUseException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Program week workout is in use"
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problem);
    }
}