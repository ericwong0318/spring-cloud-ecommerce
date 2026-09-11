package com.example.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end gateway test for ticket 04: verifies that a real HTTP request with
 * the {@code /api/v1/} prefix is routed through the gateway and that
 * {@code StripPrefix(2)} strips version/api segments before the downstream
 * service receives it (e.g. {@code /api/v1/test/products/42} → {@code /test/products/42}).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {GatewayVersioningIntegrationTest.GatewayTestConfig.class}
)
class GatewayVersioningIntegrationTest {

    /** Path the downstream stub received after the gateway applied StripPrefix. */
    private static final AtomicReference<String> DOWNSTREAM_PATH = new AtomicReference<>();

    /** Minimal downstream HTTP stub started before the Spring context is created. */
    private static final HttpServer STUB = startStub();

    @LocalServerPort
    private int gatewayPort;

    @Test
    void shouldRouteApiV1RequestThroughGatewayAndStripVersionPrefix() throws Exception {
        HttpResponse<String> response = get("/api/v1/test/products/42");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).contains("\"downstream\":true");
        assertThat(DOWNSTREAM_PATH.get())
                .isEqualTo("/test/products/42");
    }

    @Test
    void shouldNotRouteUnversionedRequests() throws Exception {
        HttpResponse<String> response = get("/test/products/42");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(DOWNSTREAM_PATH.get()).isNull();
    }

    @Test
    void shouldNotRouteWrongVersionRequests() throws Exception {
        HttpResponse<String> response = get("/api/v2/test/products/42");

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(DOWNSTREAM_PATH.get()).isNull();
    }

    private HttpResponse<String> get(String path) throws Exception {
        String url = "http://localhost:" + gatewayPort + path;
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer startStub() {
        try {
            HttpServer server = HttpServer.create(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/", GatewayVersioningIntegrationTest::handle);
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start downstream HTTP stub", e);
        }
    }

    private static void handle(HttpExchange exchange) throws IOException {
        DOWNSTREAM_PATH.set(exchange.getRequestURI().getRawPath());
        byte[] body = "{\"downstream\":true}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @SpringBootApplication(
            exclude = {
                    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class,
                    org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration.class
            }
    )
    static class GatewayTestConfig {

        @Bean
        RouteLocator versionedTestRoute(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-downstream", r -> r
                            .path("/api/v1/test/**")
                            .filters(f -> f.stripPrefix(2))
                            .uri("http://localhost:" + STUB.getAddress().getPort()))
                    .build();
        }
    }
}