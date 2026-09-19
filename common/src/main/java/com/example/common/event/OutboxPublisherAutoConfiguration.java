package com.example.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.TransactionTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(OutboxPublisherProperties.class)
@EnableSchedulerLock(defaultLockAtMostFor = "5m", defaultLockAtLeastFor = "30s")
@ConditionalOnClass({RabbitTemplate.class, ObjectMapper.class})
public class OutboxPublisherAutoConfiguration {

    @Configuration
    @ConditionalOnBean(R2dbcTransactionManager.class)
    @ConditionalOnMissingBean(OutboxEventPublisher.class)
    static class ReactiveOutboxConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OutboxEventPublisher reactiveOutboxEventPublisher(
                ReactiveOutboxEventRepository<OutboxEvent> outboxEventRepository,
                RabbitTemplate rabbitTemplate,
                ObjectMapper objectMapper,
                R2dbcTransactionManager transactionManager,
                OutboxPublisherProperties properties,
                RoutingKeyStrategy routingKeyStrategy) {

            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
            return new ReactiveOutboxEventPublisher<>(
                    outboxEventRepository,
                    rabbitTemplate,
                    objectMapper,
                    transactionalOperator,
                    properties,
                    routingKeyStrategy);
        }
    }

    @Configuration
    @ConditionalOnBean(PlatformTransactionManager.class)
    @ConditionalOnMissingBean(OutboxEventPublisher.class)
    static class JpaOutboxConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OutboxEventPublisher jpaOutboxEventPublisher(
                OutboxEventRepository outboxEventRepository,
                RabbitTemplate rabbitTemplate,
                ObjectMapper objectMapper,
                PlatformTransactionManager transactionManager,
                OutboxPublisherProperties properties,
                RoutingKeyStrategy routingKeyStrategy) {

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            return new JpaOutboxEventPublisher(
                    outboxEventRepository,
                    rabbitTemplate,
                    objectMapper,
                    transactionTemplate,
                    properties,
                    routingKeyStrategy);
        }
    }

    @Configuration
    @ConditionalOnBean(R2dbcTransactionManager.class)
    @ConditionalOnMissingBean(ReactiveIdempotentEventProcessor.class)
    static class ReactiveIdempotentProcessorConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ReactiveIdempotentEventProcessor reactiveIdempotentEventProcessor(
                CommonProcessedEventRepository processedEventRepository,
                R2dbcTransactionManager transactionManager) {

            TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
            return new ReactiveIdempotentEventProcessor(processedEventRepository, transactionalOperator);
        }
    }
}