package com.example.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .version("1.0.0")
                        .description("Payment processing service with authorize/capture/refund flow")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8086").description("Local development server"),
                        new Server().url("http://payment-service:8086").description("Docker/Kubernetes internal")));
    }
}
