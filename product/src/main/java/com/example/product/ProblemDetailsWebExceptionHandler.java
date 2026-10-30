package com.example.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WebFlux-native exception handler that returns RFC 7807 Problem Details for
 * validation and request errors. Implements {@link WebExceptionHandler} so it
 * catches exceptions at the lowest level (including {@code @Valid @RequestBody}
 * validation failures that occur before the controller method is invoked).
 * <p>
 * Registering this bean displaces Spring Boot's {@code DefaultErrorWebExceptionHandler},
 * ensuring all 400 responses use {@code application/problem+json} content type.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProblemDetailsWebExceptionHandler implements WebExceptionHandler {

    private final ObjectMapper objectMapper;

    public ProblemDetailsWebExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        Map<String, Object> problem = buildProblem(ex, exchange);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(problem);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private Map<String, Object> buildProblem(Throwable ex, ServerWebExchange exchange) {
        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "https://example.com/probs/validation-error");
        problem.put("title", "Validation error");
        problem.put("status", HttpStatus.BAD_REQUEST.value());
        problem.put("detail", ex.getMessage() != null ? ex.getMessage() : "Request validation failed");
        problem.put("instance", exchange.getRequest().getPath().value());
        return problem;
    }
}
