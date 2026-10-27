package com.example.notification;

import com.example.common.event.CommonProcessedEventRepository;
import com.example.common.event.CommonProcessedEventRepositoryImpl;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.event.OutboxPublisherProperties;
import com.example.common.event.ProcessedEventEntityRepository;
import com.example.common.event.RoutingKeyStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.notification", "com.example.common.config", "com.example.common.exception", "com.example.common.util", "com.example.common.dto"})
@EntityScan(basePackages = {"com.example.notification.model", "com.example.common.event"})
@EnableJpaRepositories(basePackages = {"com.example.notification.repository", "com.example.common.event"})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            jakarta.persistence.EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
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
    public CommonProcessedEventRepository commonProcessedEventRepository(
            ProcessedEventEntityRepository repository) {
        return new CommonProcessedEventRepositoryImpl(repository);
    }

    @Bean
    public IdempotentEventProcessor idempotentEventProcessor(
            CommonProcessedEventRepository processedEventRepository) {
        return new IdempotentEventProcessor(processedEventRepository);
    }
}
