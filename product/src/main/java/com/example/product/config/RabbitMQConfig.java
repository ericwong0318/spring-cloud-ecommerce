package com.example.product.config;

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

    @Value("${rabbitmq.queue.product-events}")
    private String productEventsQueue;

    @Value("${rabbitmq.routing-key.product-created}")
    private String productCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.product-updated}")
    private String productUpdatedRoutingKey;

    @Value("${rabbitmq.routing-key.product-deleted}")
    private String productDeletedRoutingKey;

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(productExchange, true, false);
    }

    @Bean
    public Queue productEventsQueue() {
        return new Queue(productEventsQueue, true);
    }

    @Bean
    public Binding productCreatedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with(productCreatedRoutingKey);
    }

    @Bean
    public Binding productUpdatedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with(productUpdatedRoutingKey);
    }

    @Bean
    public Binding productDeletedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with(productDeletedRoutingKey);
    }

    @Bean
    public Binding variantCreatedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with("product.variant.created");
    }

    @Bean
    public Binding variantUpdatedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with("product.variant.updated");
    }

    @Bean
    public Binding variantDeletedBinding() {
        return BindingBuilder.bind(productEventsQueue())
                .to(productExchange())
                .with("product.variant.deleted");
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