package com.example.inventory;

import com.example.common.event.CommonProcessedEventRepository;
import com.example.common.event.CommonProcessedEventRepositoryImpl;
import com.example.common.event.IdempotentEventProcessor;
import com.example.common.event.JpaOutboxEventPublisher;
import com.example.common.event.OutboxEventPublisher;
import com.example.common.event.OutboxEventRepository;
import com.example.common.event.OutboxPublisherProperties;
import com.example.common.event.ProcessedEventEntityRepository;
import com.example.common.event.RoutingKeyStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManagerFactory;

@SpringBootApplication
@EntityScan(basePackages = {"com.example.inventory.model", "com.example.common.event"})
@EnableJpaRepositories(basePackages = {"com.example.inventory.repository", "com.example.common.event"})
@EnableScheduling
@EnableTransactionManagement
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
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
    public IdempotentEventProcessor idempotentEventProcessor(
            CommonProcessedEventRepository processedEventRepository) {
        return new IdempotentEventProcessor(processedEventRepository);
    }

    @Bean
    public CommonProcessedEventRepository commonProcessedEventRepository(ProcessedEventEntityRepository repository) {
        return new CommonProcessedEventRepositoryImpl(repository);
    }
}
