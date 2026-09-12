package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behavioral contract tests for the gateway's centralized, path-based API
 * versioning (ticket 04).
 *
 * Verifies that every service route is only reachable under the
 * {@code /api/v1/} prefix and that the {@code StripPrefix(2)} filter forwards an
 * unversioned path ({@code /products/...}) to the downstream service.
 */
@SpringBootTest(classes = GatewayApplication.class)
class GatewayRouteContractTest {

    @Autowired
    RouteLocatorBuilder routeLocatorBuilder;

    private static final Map<String, String> EXPECTED_DOWNSTREAM_URIS = Map.of(
            "product-service", "lb://product",
            "category-service", "lb://category",
            "order-service", "lb://order-service",
            "inventory-service", "lb://inventory-service",
            "notification-service", "lb://notification-service",
            "payment-service", "lb://payment-service");

    private static final Map<String, String> RESOURCE_PATHS = Map.of(
            "product-service", "/api/products",
            "category-service", "/api/categories",
            "order-service", "/api/orders",
            "inventory-service", "/api/inventory",
            "notification-service", "/api/notifications",
            "payment-service", "/api/payments");

    @Test
    void shouldDefineAllServiceRoutesUnderApiV1Prefix() {
        Map<String, Route> routes = routesById();

        assertThat(routes).containsKeys(EXPECTED_DOWNSTREAM_URIS.keySet().toArray(new String[0]));
        assertThat(routes).hasSize(EXPECTED_DOWNSTREAM_URIS.size());
    }

    @Test
    void shouldOnlyMatchVersionedApiV1Paths() {
        Map<String, Route> routes = routesById();

        EXPECTED_DOWNSTREAM_URIS.forEach((routeId, expectedUri) -> {
            Route route = routes.get(routeId);
            String versionedPath = RESOURCE_PATHS.get(routeId);

            assertThat(matches(route, versionedPath + "/1"))
                    .as("%s should match %s/**", routeId, versionedPath)
                    .isTrue();
            assertThat(matches(route, versionedPath))
                    .as("%s should match %s", routeId, versionedPath)
                    .isTrue();

            // Unversioned paths (without /api) must NOT reach the route.
            assertThat(matches(route, versionedPath.replace("/api", "") + "/1"))
                    .as("%s should reject the unversioned path", routeId)
                    .isFalse();
            // A v2 path should NOT match
            assertThat(matches(route, versionedPath.replace("/api", "/api/v2") + "/1"))
                    .as("%s should reject the /api/v2 path", routeId)
                    .isFalse();
        });
    }

    @Test
    void shouldStripApiV1PrefixAndForwardUnversionedPathDownstream() {
        Map<String, Route> routes = routesById();

        EXPECTED_DOWNSTREAM_URIS.forEach((routeId, expectedUri) -> {
            Route route = routes.get(routeId);
            String versionedPath = RESOURCE_PATHS.get(routeId);

            assertThat(route.getUri().toString())
                    .as("%s should forward to %s", routeId, expectedUri)
                    .isEqualTo(expectedUri);

            // Applying the route's filter chain to a /api/v1/{resource}/1 request
            // must forward an unversioned path ({resource}/1) downstream.
            MockServerWebExchange exchange = exchangeFor(versionedPath + "/1");
            ServerWebExchange forwarded = forwardThroughFilters(route, exchange);
            assertThat(forwarded.getRequest().getURI().getPath())
                    .as("%s should receive an unversioned path", routeId)
                    .isEqualTo(versionedPath.replace("/api/v1", "") + "/1");
        });
    }

    @Test
    void shouldApplyStripPrefixFilterOnEveryRoute() {
        Map<String, Route> routes = routesById();

        routes.forEach((routeId, route) -> {
            assertThat(hasStripPrefixFilter(route))
                    .as("%s should apply the StripPrefix filter", routeId)
                    .isTrue();
        });
    }

    private Map<String, Route> routesById() {
        RouteLocator locator = GatewayApplication.customRouteLocator(routeLocatorBuilder);
        List<Route> routeList = locator.getRoutes().collectList().block();
        return routeList.stream().collect(Collectors.toMap(Route::getId, Function.identity()));
    }

    private boolean matches(Route route, String path) {
        MockServerWebExchange exchange = exchangeFor(path);
        return Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block());
    }

    /**
     * Applies the route's filter chain and returns the exchange that would be
     * forwarded downstream (StripPrefix passes a mutated exchange to the chain).
     */
    private ServerWebExchange forwardThroughFilters(Route route, MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain capturingChain = captured -> {
            forwarded.set(captured);
            return Mono.empty();
        };
        route.getFilters().forEach(filter -> filter.filter(exchange, capturingChain).block());
        assertThat(forwarded.get()).as("filters must forward an exchange").isNotNull();
        return forwarded.get();
    }

    private boolean hasStripPrefixFilter(Route route) {
        return route.getFilters().stream()
                .map(GatewayRouteContractTest::unwrap)
                .map(filter -> filter.getClass().getName())
                .anyMatch(name -> name.contains("StripPrefixGatewayFilterFactory"));
    }

    /** OrderedGatewayFilter wraps the real filter; look through the wrapper. */
    private static GatewayFilter unwrap(GatewayFilter filter) {
        return filter instanceof OrderedGatewayFilter ordered ? ordered.getDelegate() : filter;
    }

    private MockServerWebExchange exchangeFor(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("http://localhost" + path));
    }
}