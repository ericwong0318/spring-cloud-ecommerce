package com.example.order.config;

import com.example.common.exception.ProblemDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux-compatible RFC 9457 (problem+json) error handler for order-service.
 *
 * <p>The shared {@code com.example.common.exception.GlobalExceptionHandler} is servlet-based
 * (its methods accept {@code HttpServletRequest}) and therefore cannot serve the reactive
 * stack. This advice provides the documented error contract for request decoding and bean
 * validation failures, so clients receive {@code application/problem+json} instead of the
 * framework default {@code application/json}.
 */
@ControllerAdvice
@Order(-1)
public class OrderWebFluxExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderWebFluxExceptionHandler.class);

    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(WebExchangeBindException ex,
                                                                  ServerWebExchange exchange) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });

        log.warn("Request validation failed: {}", errors);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/" + HttpStatus.BAD_REQUEST.value()))
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Invalid request parameters")
                .instance(URI.create(exchange.getRequest().getPath().value()))
                .timestamp(Instant.now())
                .addProperty("errors", errors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ProblemDetailResponse> handleUnreadable(ServerWebInputException ex,
                                                                  ServerWebExchange exchange) {
        log.warn("Malformed request body: {}", ex.getMessage());

        ProblemDetailResponse problem = problem(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Request body is malformed or missing", exchange);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(DataBufferLimitException.class)
    public ResponseEntity<ProblemDetailResponse> handlePayloadTooLarge(DataBufferLimitException ex,
                                                                      ServerWebExchange exchange) {
        log.warn("Request payload exceeded the in-memory limit: {}", ex.getMessage());

        ProblemDetailResponse problem = problem(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Request body is too large", exchange);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetailResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                                      ServerWebExchange exchange) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetailResponse problem = problem(HttpStatus.BAD_REQUEST, "Invalid Argument",
                ex.getMessage(), exchange);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(problem);
    }

    private ProblemDetailResponse problem(HttpStatus status, String title, String detail, ServerWebExchange exchange) {
        return ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/" + status.value()))
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(URI.create(exchange.getRequest().getPath().value()))
                .timestamp(Instant.now())
                .build();
    }
}
