package com.example.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public ProblemDetailResponse() {
    }

    public ProblemDetailResponse(URI type, String title, int status, String detail,
                                 URI instance, Instant timestamp, Map<String, Object> properties) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.timestamp = timestamp;
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public URI getType() {
        return type;
    }

    public void setType(URI type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public URI getInstance() {
        return instance;
    }

    public void setInstance(URI instance) {
        this.instance = instance;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public ProblemDetailResponse addProperty(String key, Object value) {
        this.properties.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProblemDetailResponse that = (ProblemDetailResponse) o;
        return status == that.status &&
                Objects.equals(type, that.type) &&
                Objects.equals(title, that.title) &&
                Objects.equals(detail, that.detail) &&
                Objects.equals(instance, that.instance) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, title, status, detail, instance, timestamp, properties);
    }

    @Override
    public String toString() {
        return "ProblemDetailResponse{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", detail='" + detail + '\'' +
                ", instance=" + instance +
                ", timestamp=" + timestamp +
                ", properties=" + properties +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ProblemDetailResponse response = new ProblemDetailResponse();

        public Builder type(URI type) {
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

        public Builder instance(URI instance) {
            response.setInstance(instance);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
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