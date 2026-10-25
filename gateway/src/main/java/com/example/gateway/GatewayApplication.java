package com.example.gateway;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@SpringBootApplication
public class GatewayApplication {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r
                        .path("/api/v1/products/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://product:8081"))
                .route("category-service", r -> r
                        .path("/api/v1/categories/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("http://category:8082"))
                .route("order-service", r -> r
                        .path("/api/v1/orders/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://order-service"))
                .route("inventory-service", r -> r
                        .path("/api/v1/inventory/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://inventory-service"))
                .route("notification-service", r -> r
                        .path("/api/v1/notifications/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://notification-service"))
                .route("payment-service", r -> r
                        .path("/api/v1/payments/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://payment-service"))
                .build();
    }

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .version("1.0.0")
                        .description("API Gateway for routing to microservices")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")));
    }

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}