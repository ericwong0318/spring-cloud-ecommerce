package com.example.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Schema(description = "RFC 9457 Problem Detail Response")
public record ProblemDetailResponse(
    @Schema(description = "A URI reference that identifies the problem type", example = "https://api.example.com/errors/validation")
    URI type,

    @Schema(description = "A short, human-readable summary of the problem type", example = "Validation Failed")
    String title,

    @Schema(description = "The HTTP status code", example = "400")
    int status,

    @Schema(description = "A human-readable explanation specific to this occurrence", example = "Invalid request parameters")
    String detail,

    @Schema(description = "A URI reference that identifies the specific occurrence of the problem", example = "/api/products")
    URI instance,

    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    Instant timestamp,

    @Schema(description = "Additional extension properties")
    Map<String, Object> properties
) {
    public ProblemDetailResponse {
        properties = properties != null ? Collections.unmodifiableMap(new HashMap<>(properties)) : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI type;
        private String title;
        private int status;
        private String detail;
        private URI instance;
        private Instant timestamp;
        private final Map<String, Object> properties = new HashMap<>();

        public Builder type(URI type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(URI instance) {
            this.instance = instance;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder addProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public ProblemDetailResponse build() {
            return new ProblemDetailResponse(type, title, status, detail, instance, timestamp, properties);
        }
    }
}