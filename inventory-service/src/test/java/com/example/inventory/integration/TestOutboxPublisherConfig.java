package com.example.inventory.integration;

import com.example.common.event.OutboxEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class TestOutboxPublisherConfig {

    private static final Logger log = LoggerFactory.getLogger(TestOutboxPublisherConfig.class);

    @Bean
    @Primary
    public OutboxEventPublisher testOutboxEventPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            @Value("${rabbitmq.exchange.inventory}") String inventoryExchange) {
        log.info("Creating TEST OutboxEventPublisher bean");
        return new OutboxEventPublisher() {
            @Override
            public Mono<Void> saveEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
                log.info("saveEvent called: aggregateType={}, aggregateId={}, eventType={}", aggregateType, aggregateId, eventType);
                try {
                    String routingKey = aggregateType.toLowerCase() + "." + eventType.toLowerCase();
                    log.info("Publishing event to exchange={}, routingKey={}: payload={}", inventoryExchange, routingKey, payload);
                    // Send the payload object directly - Jackson2JsonMessageConverter will serialize it
                    rabbitTemplate.convertAndSend(inventoryExchange, routingKey, payload);
                    log.info("Event published successfully");
                    return Mono.empty();
                } catch (Exception e) {
                    log.error("Failed to publish outbox event", e);
                    throw new RuntimeException("Failed to publish outbox event", e);
                }
            }

            @Override
            public void publishOutboxEvents() {
                log.info("publishOutboxEvents called (no-op)");
            }

            @Override
            public Mono<Void> publishOutboxEventsReactive() {
                log.info("publishOutboxEventsReactive called (no-op)");
                return Mono.empty();
            }
        };
    }
}