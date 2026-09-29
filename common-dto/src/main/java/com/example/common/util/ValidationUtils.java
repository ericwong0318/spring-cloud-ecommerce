package com.example.common.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;
import java.util.stream.Collectors;

public class ValidationUtils {

    private static final Validator VALIDATOR;

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        VALIDATOR = factory.getValidator();
    }

    private ValidationUtils() {
        // Utility class
    }

    /**
     * Validate an object and throw exception if invalid
     */
    public static <T> void validate(T object) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + message);
        }
    }

    /**
     * Validate an object with specific groups
     */
    public static <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object, groups);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Validation failed: " + message);
        }
    }

    /**
     * Get validation violations without throwing
     */
    public static <T> Set<ConstraintViolation<T>> getViolations(T object) {
        return VALIDATOR.validate(object);
    }

    /**
     * Check if object is valid
     */
    public static <T> boolean isValid(T object) {
        return VALIDATOR.validate(object).isEmpty();
    }
}
