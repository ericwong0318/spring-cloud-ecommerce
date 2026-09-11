package org.example.configserver;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    @Bean
    public OpenAPI configServerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Config Server API")
                        .version("1.0.0")
                        .description("Spring Cloud Config Server with Git backend")
                        .contact(new Contact()
                                .name("Spring Cloud Platform")
                                .email("support@example.com")));
    }

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
