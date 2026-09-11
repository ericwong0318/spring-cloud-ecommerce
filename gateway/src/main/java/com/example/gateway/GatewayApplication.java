package com.example.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://product"))
                .route("category-service", r -> r
                        .path("/api/categories/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://category"))
                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://order-service"))
                .route("inventory-service", r -> r
                        .path("/api/inventory/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://inventory-service"))
                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("lb://notification-service"))
                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.stripPrefix(1))
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
