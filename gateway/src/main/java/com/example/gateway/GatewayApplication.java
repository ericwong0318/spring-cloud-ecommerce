package com.example.gateway;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.factory.CircuitBreakerGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

    @Bean
    public ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory(CircuitBreakerRegistry circuitBreakerRegistry) {
        return new ReactiveResilience4JCircuitBreakerFactory(circuitBreakerRegistry);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
        return gatewayRoutes(builder, circuitBreakerFactory);
    }

    /**
     * Centralized, path-based API versioning.
     *
     * All public API routes are versioned at the gateway using an {@code /api/v1/}
     * prefix. The {@code StripPrefix=2} filter removes the {@code /api} and
     * {@code /v1} segments before forwarding, so downstream services receive an
     * unversioned path (e.g. {@code /products/...}). Services therefore stay
     * version-agnostic: to introduce {@code /api/v2/}, add a new gateway route and
     * leave the services untouched.
     */
    static RouteLocator gatewayRoutes(RouteLocatorBuilder builder, ReactiveResilience4JCircuitBreakerFactory circuitBreakerFactory) {
        return builder.routes()
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("product-service").setFallbackUri("forward:/fallback/product")))
                        .uri("lb://product"))
                .route("category-service", r -> r
                        .path("/api/v1/categories/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("category-service").setFallbackUri("forward:/fallback/category")))
                        .uri("lb://category"))
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("order-service").setFallbackUri("forward:/fallback/order")))
                        .uri("lb://order-service"))
                .route("inventory-service", r -> r
                        .path("/api/v1/inventory/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("inventory-service").setFallbackUri("forward:/fallback/inventory")))
                        .uri("lb://inventory-service"))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("notification-service").setFallbackUri("forward:/fallback/notification")))
                        .uri("lb://notification-service"))
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .circuitBreaker(config -> config.setName("payment-service").setFallbackUri("forward:/fallback/payment")))
                        .uri("lb://payment-service"))
                .build();
    }

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .version("1.0.0")
                        .description("Spring Cloud Gateway for routing to microservices")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")));
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
