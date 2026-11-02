package com.devalgas.productpage.api;

import com.devalgas.productpage.domain.AggregationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request");
        problem.setProperty("code", "INVALID_REQUEST");
        return problem;
    }

    @ExceptionHandler(AggregationException.class)
    ProblemDetail aggregationFailure(AggregationException exception) {
        HttpStatus status = switch (exception.kind()) {
            case GLOBAL_DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            case UPSTREAM_FAILURE -> HttpStatus.BAD_GATEWAY;
            case INTERRUPTED -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle("Product page aggregation failed");
        problem.setProperty("code", exception.kind().name());
        return problem;
    }
}
