package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class
})
@ComponentScan(basePackages = {
    "com.example.order",
    "com.example.common.event",
    "com.example.common.dto",
    "com.example.common.exception",
    "com.example.common.util"
})
@EnableJpaRepositories(basePackages = {"com.example.order.repository", "com.example.common.event"})
@EntityScan(basePackages = {"com.example.order.model", "com.example.common.event"})
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
