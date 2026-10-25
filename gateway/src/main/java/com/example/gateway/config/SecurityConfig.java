package com.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(-200)
    public WebFilter corsPreflightFilter() {
        return new CorsPreflightWebFilter();
    }

    @Bean
    @Order(-199)
    public WebFilter requestLoggingFilter() {
        return new RequestLoggingWebFilter();
    }

    private static class CorsPreflightWebFilter implements WebFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                System.out.println(">>> CORS Preflight: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath() + " origin=" + exchange.getRequest().getHeaders().getOrigin());
                var origin = exchange.getRequest().getHeaders().getOrigin();
                var response = exchange.getResponse();
                response.getHeaders().add("Access-Control-Allow-Origin", origin != null ? origin : "*");
                response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
                response.getHeaders().add("Access-Control-Allow-Headers", "*");
                response.getHeaders().add("Access-Control-Allow-Credentials", "true");
                response.getHeaders().add("Access-Control-Max-Age", "3600");
                return response.setComplete();
            }
            return chain.filter(exchange);
        }

        @Override
        public int getOrder() {
            return -200;
        }
    }

    private static class RequestLoggingWebFilter implements WebFilter, Ordered {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            System.out.println(">>> Gateway Request: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath() + " uri=" + exchange.getRequest().getURI());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                System.out.println(">>> Gateway Response: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getPath() + " status=" + exchange.getResponse().getStatusCode());
            }));
        }

        @Override
        public int getOrder() {
            return -199;
        }
    }
}