package com.example.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.product}")
    private String productExchange;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.queue.inventory-events}")
    private String inventoryEventsQueue;

    @Value("${rabbitmq.routing-key.product-created}")
    private String productCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.product-updated}")
    private String productUpdatedRoutingKey;

    @Value("${rabbitmq.routing-key.product-deleted}")
    private String productDeletedRoutingKey;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-cancelled}")
    private String orderCancelledRoutingKey;

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(productExchange, true, false);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    @Bean
    public Queue inventoryEventsQueue() {
        return new Queue(inventoryEventsQueue, true);
    }

    @Bean
    public Binding productCreatedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(productExchange())
                .with(productCreatedRoutingKey);
    }

    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(productExchange())
                .with(productUpdatedRoutingKey);
    }

    @Bean
    public Binding productDeletedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(productExchange())
                .with(productDeletedRoutingKey);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(orderExchange())
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(orderExchange())
                .with(orderCancelledRoutingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        return template;
    }
}