package com.example.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "RFC 9457 Problem Detail Response")
public class ProblemDetailResponse {

    @Schema(description = "A URI reference that identifies the problem type", example = "https://api.example.com/errors/validation")
    private URI type;

    @Schema(description = "A short, human-readable summary of the problem type", example = "Validation Failed")
    private String title;

    @Schema(description = "The HTTP status code", example = "400")
    private int status;

    @Schema(description = "A human-readable explanation specific to this occurrence", example = "Invalid request parameters")
    private String detail;

    @Schema(description = "A URI reference that identifies the specific occurrence of the problem", example = "/api/products")
    private URI instance;

    @Schema(description = "Timestamp when the error occurred", example = "2024-01-15T10:30:00Z")
    private Instant timestamp;

    @Schema(description = "Additional extension properties")
    private Map<String, Object> properties = new HashMap<>();

    public ProblemDetailResponse addProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProblemDetailResponse response = new ProblemDetailResponse();

        public Builder type(java.net.URI type) {
            response.setType(type);
            return this;
        }

        public Builder title(String title) {
            response.setTitle(title);
            return this;
        }

        public Builder status(int status) {
            response.setStatus(status);
            return this;
        }

        public Builder detail(String detail) {
            response.setDetail(detail);
            return this;
        }

        public Builder instance(java.net.URI instance) {
            response.setInstance(instance);
            return this;
        }

        public Builder timestamp(java.time.Instant timestamp) {
            response.setTimestamp(timestamp);
            return this;
        }

        public Builder addProperty(String key, Object value) {
            response.addProperty(key, value);
            return this;
        }

        public ProblemDetailResponse build() {
            return response;
        }
    }
}