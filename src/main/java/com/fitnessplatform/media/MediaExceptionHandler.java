package com.fitnessplatform.media;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MediaExceptionHandler {
    @ExceptionHandler(MediaException.class)
    ProblemDetail handle(MediaException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.reason().status(), exception.getMessage());
        problem.setTitle("Media request failed");
        problem.setProperty("code", exception.reason().name());
        return problem;
    }
}
