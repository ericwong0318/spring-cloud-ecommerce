package com.example.gateway.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(-200)
    public WebFilter corsPreflightFilter() {
        return new CorsPreflightWebFilter();
    }

    @Bean
    @Order(-199)
    public WebFilter requestLoggingFilter() {
        return new RequestLoggingWebFilter();
    }

    @Bean
    @Order(-198)
    public WebFilter openApiRequestValidationFilter(OpenAPI openAPI, ObjectMapper objectMapper) {
        return new OpenApiRequestValidationWebFilter(openAPI, objectMapper);
    }

    private static class CorsPreflightWebFilter implements WebFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            var request = exchange.getRequest();
            var response = exchange.getResponse();
            var origin = request.getHeaders().getOrigin();

            // Add CORS headers to all responses
            if (origin != null) {
                response.getHeaders().add("Access-Control-Allow-Origin", origin);
                response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
                response.getHeaders().add("Access-Control-Allow-Headers", "*");
                response.getHeaders().add("Access-Control-Allow-Credentials", "true");
                response.getHeaders().add("Access-Control-Max-Age", "3600");
            }

            // Handle preflight OPTIONS requests
            if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
                System.out.println(">>> CORS Preflight: " + request.getMethod() + " " + request.getPath() + " origin=" + origin);
                return response.setComplete();
            }

            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return -200;
        }
    }

    private static class RequestLoggingWebFilter implements WebFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            System.out.println(">>> Gateway Request: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath() + " uri=" + exchange.getRequest().getURI());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                System.out.println(">>> Gateway Response: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath() + " status=" + exchange.getResponse().getStatusCode());
            }));
        }

        @Override
        public int getOrder() {
            return -199;
        }
    }

    /**
     * Validates incoming requests against the aggregated OpenAPI specification.
     * Only validates requests (not responses) to minimize latency impact.
     * Skips validation for actuator, swagger-ui, and OpenAPI doc endpoints.
     * Uses networknt JSON Schema Validator for request body validation.
     */
    private static class OpenApiRequestValidationWebFilter implements WebFilter, Ordered {

        private final Map<String, JsonSchema> pathMethodToSchema;
        private final ObjectMapper objectMapper;
        private final JsonSchemaFactory schemaFactory;

        OpenApiRequestValidationWebFilter(OpenAPI openAPI, ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.pathMethodToSchema = buildSchemaMap(openAPI);
        }

        private Map<String, JsonSchema> buildSchemaMap(OpenAPI openAPI) {
            if (openAPI.getPaths() == null) {
                return Map.of();
            }

            return openAPI.getPaths().entrySet().stream()
                    .flatMap(pathEntry -> {
                        String path = pathEntry.getKey();
                        var pathItem = pathEntry.getValue();
                        return pathItem.readOperationsMap().entrySet().stream()
                                .filter(entry -> entry.getValue().getRequestBody() != null)
                                .map(entry -> {
                                    String method = entry.getKey().name();
                                    var op = entry.getValue();
                                    RequestBody requestBody = op.getRequestBody();
                                    Content content = requestBody.getContent();
                                    if (content != null) {
                                        io.swagger.v3.oas.models.media.MediaType mediaType = content.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                                        if (mediaType != null && mediaType.getSchema() != null) {
                                            Schema<?> schema = mediaType.getSchema();
                                            try {
                                                JsonNode schemaNode = objectMapper.valueToTree(schema);
                                                JsonSchema jsonSchema = schemaFactory.getSchema(schemaNode);
                                                return Map.entry(path + ":" + method, jsonSchema);
                                            } catch (Exception e) {
                                                // Skip schemas that can't be converted
                                            }
                                        }
                                    }
                                    return null;
                                })
                                .filter(java.util.Objects::nonNull);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            // Skip validation for internal/management endpoints
            if (shouldSkipValidation(path)) {
                return chain.filter(exchange);
            }

            // Only validate requests with body (POST, PUT, PATCH)
            if (!List.of("POST", "PUT", "PATCH").contains(method)) {
                return chain.filter(exchange);
            }

            // Only validate JSON content
            String contentType = exchange.getRequest().getHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith(org.springframework.http.MediaType.APPLICATION_JSON_VALUE)) {
                return chain.filter(exchange);
            }

            // Find matching schema
            String key = findMatchingSchema(path, method);
            if (key == null) {
                return chain.filter(exchange);
            }

            JsonSchema schema = pathMethodToSchema.get(key);
            if (schema == null) {
                return chain.filter(exchange);
            }

            return exchange.getRequest().getBody()
                    .reduce((a, b) -> {
                        if (b.readableByteCount() > 0) {
                            byte[] bytes = new byte[b.readableByteCount()];
                            b.read(bytes);
                            a.write(bytes);
                        }
                        DataBufferUtils.release(b);
                        return a;
                    })
                    .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);

                        try {
                            JsonNode requestBody = objectMapper.readTree(bytes);
                            Set<ValidationMessage> errors = schema.validate(requestBody);
                            if (!errors.isEmpty()) {
                                String errorMsg = errors.stream()
                                        .map(ValidationMessage::getMessage)
                                        .reduce((a, b) -> a + "; " + b)
                                        .orElse("Validation failed");
                                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAPI validation failed: " + errorMsg));
                            }
                        } catch (Exception e) {
                            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAPI validation error: " + e.getMessage(), e));
                        }
                        return Mono.empty();
                    })
                    .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)))
                    .onErrorResume(ResponseStatusException.class, Mono::error)
                    .then(chain.filter(exchange));
        }

        private String findMatchingSchema(String requestPath, String method) {
            // Exact match first
            String exactKey = requestPath + ":" + method;
            if (pathMethodToSchema.containsKey(exactKey)) {
                return exactKey;
            }

            // Pattern match for path variables (e.g., /products/{id} matches /products/123)
            for (String schemaKey : pathMethodToSchema.keySet()) {
                String[] parts = schemaKey.split(":", 2);
                String schemaPath = parts[0];
                String schemaMethod = parts[1];

                if (!schemaMethod.equals(method)) {
                    continue;
                }

                if (matchPathPattern(schemaPath, requestPath)) {
                    return schemaKey;
                }
            }

            return null;
        }

        private boolean matchPathPattern(String pattern, String path) {
            String[] patternParts = pattern.split("/");
            String[] pathParts = path.split("/");

            if (patternParts.length != pathParts.length) {
                return false;
            }

            for (int i = 0; i < patternParts.length; i++) {
                if (patternParts[i].startsWith("{") && patternParts[i].endsWith("}")) {
                    continue; // Path variable matches anything
                }
                if (!patternParts[i].equals(pathParts[i])) {
                    return false;
                }
            }

            return true;
        }

        private boolean shouldSkipValidation(String path) {
            return path.startsWith("/actuator")
                    || path.startsWith("/swagger-ui")
                    || path.startsWith("/v3/api-docs")
                    || path.startsWith("/webjars")
                    || path.equals("/favicon.ico")
                    || path.equals("/error");
        }

        @Override
        public int getOrder() {
            return -198;
        }
    }
}