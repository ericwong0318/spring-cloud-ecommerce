package com.example.category;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CategoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CategoryApplication.class, args);
    }

    @Bean
    public OpenAPI categoryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Category Service API")
                        .version("1.0.0")
                        .description("Category domain service for managing categories")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")));
    }
}
