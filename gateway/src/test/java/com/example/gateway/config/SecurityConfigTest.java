package com.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT
)
@Import({com.example.gateway.GatewayApplication.class, SecurityConfig.class})
class SecurityConfigTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    public void setWebTestClient(WebTestClient.Builder builder) {
        this.webTestClient = builder.baseUrl("http://localhost:" + port).build();
    }

    @Test
    void actuatorHealthEndpointShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuatorInfoEndpointShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void actuatorPrometheusEndpointShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void wellKnownEndpointsShouldBeAccessibleWithoutAuthentication() {
        webTestClient.get()
                .uri("/.well-known/jwks.json")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void protectedEndpointShouldRequireAuthentication() {
        webTestClient.get()
                .uri("/api/orders/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void corsPreflightRequestShouldSucceed() {
        webTestClient.options()
                .uri("/api/products")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void corsActualRequestShouldIncludeHeaders() {
        webTestClient.get()
                .uri("/api/products")
                .header("Origin", "http://localhost:3000")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void securityHeadersShouldBePresent() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("X-Frame-Options", "DENY");
    }
}