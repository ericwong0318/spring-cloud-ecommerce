package com.example.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.notification", "com.example.common.event", "com.example.common.config", "com.example.common.exception", "com.example.common.util", "com.example.common.dto"})
@EntityScan(basePackages = {"com.example.notification.model", "com.example.common.event"})
@EnableJpaRepositories(basePackages = {"com.example.notification.repository", "com.example.common.event"})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}