package com.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@WebFluxTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TokenRelayGatewayFilterFactory tokenRelayGatewayFilterFactory;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockBean
    private RouteLocator routeLocator;

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