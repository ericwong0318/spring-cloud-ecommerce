package org.example.authserver;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AuthServerApplication {

    @Bean
    public OpenAPI authServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Authorization Server API")
                        .version("1.0.0")
                        .description("Spring Authorization Server for OAuth2/JWT")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")));
    }

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}