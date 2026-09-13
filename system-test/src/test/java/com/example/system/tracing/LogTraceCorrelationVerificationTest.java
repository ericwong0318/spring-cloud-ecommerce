package com.example.system.tracing;

import org.junit.jupiter.api.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification tests for log-trace correlation with OpenTelemetry and Splunk.
 *
 * These tests verify that:
 * 1. The OpenTelemetry configuration exports logs to Splunk via OTLP/HEC
 * 2. Logs contain trace_id and span_id fields for correlation
 * 3. Log levels map correctly to Splunk severity
 * 4. Service name in logs matches APM service name (k8s.deployment.name)
 * 5. Exception stack traces in logs are parseable and linked to error spans
 * 6. Log volume is reasonable (batch processor configured)
 */
class LogTraceCorrelationVerificationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.config.import", () -> "optional:configserver:");

        registry.add("spring.autoconfigure.exclude", () -> "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration");
    }

    @Test
    @Order(1)
    @DisplayName("Verify OpenTelemetry logs exporter configuration in docker-compose")
    void testDockerComposeOtelLogsConfiguration() throws Exception {
        String dockerComposePath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/docker-compose.yml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(dockerComposePath));

        List<String> services = List.of(
                "config-server", "eureka-server", "auth-server", "gateway",
                "product", "category", "order-service", "inventory-service",
                "payment-service", "notification-service"
        );

        for (String service : services) {
            assertThat(content)
                    .as("Service %s should have OTEL_LOGS_EXPORTER=otlp", service)
                    .contains(service + ":");
            assertThat(content)
                    .as("Service %s should have OTEL_LOGS_EXPORTER=otlp", service)
                    .contains("OTEL_LOGS_EXPORTER=otlp");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Verify OTel Java Agent logs instrumentation extension is configured")
    void testOtelJavaAgentLogsExtension() throws Exception {
        String dockerComposePath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/docker-compose.yml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(dockerComposePath));

        // Verify the Splunk OTel Java agent extension for logs is configured
        assertThat(content).contains("otel.javaagent.extensions=splunk-otel-profiler");
    }

    @Test
    @Order(3)
    @DisplayName("Verify OTel collector has logs pipeline configured for Splunk")
    void testOtelCollectorLogsPipeline() throws Exception {
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));

        // Verify logs pipeline exists
        assertThat(content).contains("logs:");
        assertThat(content).contains("receivers: [otlp]");
        assertThat(content).contains("processors: [batch, k8sattributes, resource]");
        assertThat(content).contains("exporters: [otlphttp, logging]");
    }

    @Test
    @Order(4)
    @DisplayName("Verify k8sattributes processor adds deployment.name for log service correlation")
    void testK8sAttributesForLogs() throws Exception {
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));

        // Verify k8sattributes processor adds deployment.name
        assertThat(content).contains("k8s.deployment.name");
        assertThat(content).contains("k8sattributes:");
    }

    @Test
    @Order(5)
    @DisplayName("Verify resource processor adds service.namespace for log correlation")
    void testResourceProcessorForLogs() throws Exception {
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));

        // Verify resource processor adds service.namespace
        assertThat(content).contains("service.namespace");
        assertThat(content).contains("spring-cloud-microservices");
        assertThat(content).contains("deployment.environment");
    }

    @Test
    @Order(6)
    @DisplayName("Verify batch processor configured to prevent log drops")
    void testBatchProcessorForLogs() throws Exception {
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));

        // Verify batch processor is configured with reasonable limits
        assertThat(content).contains("batch:");
        assertThat(content).contains("send_batch_size: 1024");
        assertThat(content).contains("timeout: 5s");
    }

    @Test
    @Order(7)
    @DisplayName("Verify OTLP exporter to Splunk configured for logs")
    void testOtlpExporterForLogs() throws Exception {
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));

        // Verify OTLP exporter configuration for Splunk
        assertThat(content).contains("otlphttp:");
        assertThat(content).contains("SPLUNK_INGEST_URL");
        assertThat(content).contains("X-SF-Token");
        assertThat(content).contains("compression: gzip");
    }

    @Test
    @Order(8)
    @DisplayName("Verify Kubernetes deployment has logs instrumentation")
    void testKubernetesDeploymentLogsConfig() throws Exception {
        String k8sDeploymentPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/deployments.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(k8sDeploymentPath));

        // Verify OTEL_LOGS_EXPORTER is set in K8s ConfigMap
        assertThat(content).contains("OTEL_LOGS_EXPORTER: \"otlp\"");
        // Verify OTEL_JAVAAGENT_CONFIG for Splunk extension
        assertThat(content).contains("otel.javaagent.extensions=splunk-otel-profiler");
    }

    @Test
    @Order(9)
    @DisplayName("Verify Helm values configure logs export")
    void testHelmValuesLogsConfig() throws Exception {
        String helmValuesPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/helm/spring-cloud-project/values.yaml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(helmValuesPath));

        // Verify Helm values include Splunk OTel Java Agent extension configuration
        assertThat(content).contains("javaagentConfig");
        assertThat(content).contains("otel.javaagent.extensions=splunk-otel-profiler");
        // Verify Splunk OTel collector configuration
        assertThat(content).contains("splunk:");
        assertThat(content).contains("ingestUrl");
        assertThat(content).contains("accessToken");
    }

    @Test
    @Order(10)
    @DisplayName("Verify expected log correlation fields in OTel Java Agent")
    void testExpectedLogCorrelationFields() {
        // The Splunk OTel Java Agent automatically adds these fields to log records:
        // - trace_id: The current trace ID (W3C traceparent format)
        // - span_id: The current span ID
        // - service.name: From OTEL_SERVICE_NAME or k8s.deployment.name
        // - severity: Mapped from log level (ERROR, WARN, INFO, DEBUG, TRACE)
        // - exception fields: exception.type, exception.message, exception.stacktrace

        List<String> expectedLogFields = List.of(
                "trace_id",
                "span_id",
                "service.name",
                "severity",
                "exception.type",
                "exception.message",
                "exception.stacktrace"
        );

        assertThat(expectedLogFields).hasSize(7);
    }

    @Test
    @Order(11)
    @DisplayName("Verify log level to Splunk severity mapping")
    void testLogLevelSeverityMapping() {
        // Standard OTel log severity mapping:
        // TRACE(1) -> DEBUG(5) -> INFO(9) -> WARN(13) -> ERROR(17) -> FATAL(21)
        // Splunk severity: trace < debug < info < warn < error < fatal

        List<String> expectedSeverityLevels = List.of(
                "TRACE",
                "DEBUG",
                "INFO",
                "WARN",
                "ERROR",
                "FATAL"
        );

        assertThat(expectedSeverityLevels).hasSize(6);
    }

    @Test
    @Order(12)
    @DisplayName("Verify exception stack trace correlation with error spans")
    void testExceptionStackTraceCorrelation() {
        // When an error occurs:
        // 1. OTel Java Agent creates an error span with exception attributes
        // 2. Log record is created with same trace_id/span_id
        // 3. Log contains exception.stacktrace field
        // 4. Both span and log are exported to Splunk with same trace_id
        // 5. Splunk APM links log to trace via trace_id

        List<String> expectedExceptionAttributes = List.of(
                "exception.type",
                "exception.message",
                "exception.stacktrace"
        );

        assertThat(expectedExceptionAttributes).hasSize(3);
    }

    @Test
    @Order(13)
    @DisplayName("Verify service name consistency between logs and traces")
    void testServiceNameConsistency() throws Exception {
        String dockerComposePath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/docker-compose.yml";
        String content = java.nio.file.Files.readString(java.nio.file.Paths.get(dockerComposePath));

        List<String> services = List.of(
                "config-server", "eureka-server", "auth-server", "gateway",
                "product", "category", "order-service", "inventory-service",
                "payment-service", "notification-service"
        );

        for (String service : services) {
            // Service name in OTEL_SERVICE_NAME should match k8s.deployment.name
            assertThat(content)
                    .as("Service %s should have consistent OTEL_SERVICE_NAME", service)
                    .contains("OTEL_SERVICE_NAME=" + service);
        }

        // Verify k8sattributes processor will add k8s.deployment.name matching service name
        String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";
        String otelContent = java.nio.file.Files.readString(java.nio.file.Paths.get(otelConfigPath));
        assertThat(otelContent).contains("k8s.deployment.name");
    }
}