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

    @ExceptionHandler(ProgramWeekNotFoundException.class)
    ProblemDetail handleProgramWeekNotFound(
            ProgramWeekNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle("Program week not found");

        return problem;
    }

    @ExceptionHandler(ProgramWeekPositionConflictException.class)
    ProblemDetail handleProgramWeekConflict(
            ProgramWeekPositionConflictException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle("Program week position conflict");

        return problem;
    }

    @ExceptionHandler(
            ProgramWeekWorkoutNotFoundException.class
    )
    ProblemDetail handleProgramWeekWorkoutNotFound(
            ProgramWeekWorkoutNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle(
                "Program week workout not found"
        );

        return problem;
    }

    @ExceptionHandler(
            ProgramWeekWorkoutPositionConflictException.class
    )
    ProblemDetail handleProgramWeekWorkoutPositionConflict(
            ProgramWeekWorkoutPositionConflictException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Program week workout position conflict"
        );

        return problem;
    }

    @ExceptionHandler(
            ProgramResourceNotFoundException.class
    )
    ProblemDetail handleProgramResourceNotFound(
            ProgramResourceNotFoundException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND,
                        exception.getMessage()
                );

        problem.setTitle(
                "Program resource not found"
        );

        return problem;
    }

    @ExceptionHandler(
            ProgramResourcePositionConflictException.class
    )
    ProblemDetail handleProgramResourcePositionConflict(
            ProgramResourcePositionConflictException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        exception.getMessage()
                );

        problem.setTitle(
                "Program resource position conflict"
        );

        return problem;
    }

    @ExceptionHandler(
            ProgramWeekLockedException.class
    )
    ProblemDetail handleProgramWeekLocked(
            ProgramWeekLockedException exception
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        exception.getMessage()
                );

        problem.setTitle("Program week locked");

        problem.setProperty(
                "unlocksAt",
                exception.getUnlocksAt()
        );

        return problem;
    }
}
