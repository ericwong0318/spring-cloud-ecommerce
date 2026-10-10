package com.example.category;

import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.event.OutboxPublisherProperties;
import com.example.common.event.RoutingKeyStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.category", "com.example.common.exception"})
@EnableJpaRepositories(basePackages = {"com.example.category", "com.example.common.event"})
@EntityScan(basePackages = {"com.example.category", "com.example.common.event"})
@EnableTransactionManagement
public class CategoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CategoryApplication.class, args);
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    @ConfigurationProperties(prefix = "outbox.publisher")
    public OutboxPublisherProperties outboxPublisherProperties() {
        return new OutboxPublisherProperties();
    }

    @Bean
    public OutboxEventPublisher outboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            OutboxPublisherProperties properties) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        return new JpaOutboxEventPublisher(
                outboxEventRepository,
                rabbitTemplate,
                objectMapper,
                transactionTemplate,
                properties,
                RoutingKeyStrategy.DEFAULT);
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
