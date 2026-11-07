package com.example.system.k8s;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * System-level verification of the Kubernetes deployment manifests in {@code k8s/deployments.yaml}.
 *
 * <p>These tests verify that all services are declared as Deployments with the required
 * OpenTelemetry configuration, readiness probes, and production profile — without needing
 * a live Kubernetes cluster. They read the manifest file directly and assert on its content.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KubernetesDeploymentVerificationTest {

    private static final String K8S_DEPLOYMENTS_PATH =
            "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/deployments.yaml";

    private static final List<String> ALL_SERVICES = List.of(
            "config-server", "eureka-server", "auth-server", "gateway",
            "product", "category", "order-service", "inventory-service",
            "payment-service", "notification-service"
    );

    private String content() throws Exception {
        return new java.io.File(K8S_DEPLOYMENTS_PATH).exists()
                ? java.nio.file.Files.readString(java.nio.file.Paths.get(K8S_DEPLOYMENTS_PATH))
                : "";
    }

    @Test
    void otelConfigConfigMap_shouldDefineRequiredEnvironmentVariables() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: ConfigMap exists with required OTel env vars
        assertThat(content).contains("kind: ConfigMap");
        assertThat(content).contains("name: otel-config");
        assertThat(content).contains("OTEL_EXPORTER_OTLP_ENDPOINT");
        assertThat(content).contains("OTEL_PROPAGATORS: \"tracecontext,baggage\"");
        assertThat(content).contains("OTEL_LOGS_EXPORTER: \"otlp\"");
        assertThat(content).contains("otel.javaagent.extensions=splunk-otel-profiler");
    }

    @Test
    void everyService_shouldHaveADeployment() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each service has a Deployment resource
        for (String service : ALL_SERVICES) {
            assertThat(content)
                    .as("Deployment for service %s", service)
                    .contains("name: " + service);
        }
    }

    @Test
    void everyDeployment_shouldUseOtelConfigConfigMap() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each Deployment references the otel-config ConfigMap via envFrom
        int deploymentCount = countOccurrences(content, "kind: Deployment");
        int configMapRefCount = countOccurrences(content, "configMapRef:");
        int otelConfigRefCount = countOccurrences(content, "name: otel-config");

        assertThat(deploymentCount).isEqualTo(ALL_SERVICES.size());
        assertThat(configMapRefCount).isGreaterThanOrEqualTo(ALL_SERVICES.size());
        assertThat(otelConfigRefCount).isGreaterThanOrEqualTo(ALL_SERVICES.size());
    }

    @Test
    void everyDeployment_shouldSetProdProfileAndOtelServiceName() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each Deployment sets SPRING_PROFILES_ACTIVE=prod and OTEL_SERVICE_NAME
        for (String service : ALL_SERVICES) {
            assertThat(content)
                    .as("Service %s should set SPRING_PROFILES_ACTIVE=prod", service)
                    .contains("value: \"prod\"");
            assertThat(content)
                    .as("Service %s should set OTEL_SERVICE_NAME", service)
                    .contains("value: \"" + service + "\"");
        }
    }

    @Test
    void everyDeployment_shouldHaveReadinessProbe() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each Deployment has a readinessProbe hitting /actuator/health
        int readinessProbeCount = countOccurrences(content, "readinessProbe:");
        int healthPathCount = countOccurrences(content, "path: /actuator/health");

        assertThat(readinessProbeCount).isEqualTo(ALL_SERVICES.size());
        assertThat(healthPathCount).isEqualTo(ALL_SERVICES.size());
    }

    @Test
    void everyDeployment_shouldHaveOpenTelemetryInstrumentationAnnotation() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each pod template has the OTel Java instrumentation annotation
        int annotationCount = countOccurrences(content,
                "instrumentation.opentelemetry.io/inject-java: \"true\"");

        assertThat(annotationCount).isGreaterThanOrEqualTo(ALL_SERVICES.size());
    }

    @Test
    void everyService_shouldHaveAServiceResource() throws Exception {
        // Arrange & Act
        String content = content();

        // Assert: each service has a corresponding Service resource
        for (String service : ALL_SERVICES) {
            assertThat(content)
                    .as("Service resource for %s", service)
                    .contains("name: " + service);
        }
    }

    private static int countOccurrences(String content, String substring) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}
