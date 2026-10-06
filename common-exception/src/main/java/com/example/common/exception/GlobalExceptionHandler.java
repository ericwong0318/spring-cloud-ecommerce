package com.example.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {} - {}", ex.getResourceName(), ex.getResourceId());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/not-found"))
                .title("Resource Not Found")
                .status(HttpStatus.NOT_FOUND.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("resourceName", ex.getResourceName())
                .addProperty("resourceId", ex.getResourceId().toString())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ProblemDetailResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {

        log.warn("Insufficient stock: {}", ex.getMessage());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/insufficient-stock"))
                .title("Insufficient Stock")
                .status(HttpStatus.CONFLICT.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("productId", ex.getProductId())
                .addProperty("available", ex.getAvailable())
                .addProperty("requested", ex.getRequested())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetailResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business exception: {} (code: {})", ex.getMessage(), ex.getErrorCode());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/business"))
                .title("Business Rule Violation")
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("errorCode", ex.getErrorCode())
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/validation"))
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Invalid request parameters")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("errors", errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Constraint violation: {}", errors);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/validation"))
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Invalid request parameters")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("errors", errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetailResponse> handleWebExchangeBindException(
            WebExchangeBindException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Web exchange validation failed: {}", errors);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/validation"))
                .title("Validation Failed")
                .status(HttpStatus.BAD_REQUEST.value())
                .detail("Invalid request parameters")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .addProperty("errors", errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetailResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Method not supported: {}", ex.getMessage());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/method-not-allowed"))
                .title("Method Not Allowed")
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(problem);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetailResponse> handleMediaTypeNotSupported(
            org.springframework.web.HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        log.warn("Media type not supported: {}", ex.getMessage());

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/unsupported-media-type"))
                .title("Unsupported Media Type")
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .detail(ex.getMessage())
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error: ", ex);

        ProblemDetailResponse problem = ProblemDetailResponse.builder()
                .type(URI.create("https://api.example.com/errors/internal"))
                .title("Internal Server Error")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail("An unexpected error occurred")
                .instance(URI.create(request.getRequestURI()))
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}