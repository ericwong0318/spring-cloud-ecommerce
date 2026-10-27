package com.example.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.payment:payment.events}")
    private String paymentExchange;

    @Value("${rabbitmq.exchange.inventory:inventory.events}")
    private String inventoryExchange;

    @Value("${rabbitmq.queue.payment-events:payment.events.queue}")
    private String paymentEventsQueue;

    @Value("${rabbitmq.queue.inventory-reserved:inventory.reserved.queue}")
    private String inventoryReservedQueue;

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange, true, false);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(paymentEventsQueue, true);
    }

    @Bean
    public Queue inventoryReservedQueue() {
        return new Queue(inventoryReservedQueue, true);
    }

    @Bean
    public Binding paymentAuthorizedBinding(TopicExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.authorized");
    }

    @Bean
    public Binding paymentCapturedBinding(TopicExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.captured");
    }

    @Bean
    public Binding paymentRefundedBinding(TopicExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.refunded");
    }

    @Bean
    public Binding paymentFailedBinding(TopicExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.failed");
    }

    @Bean
    public Binding inventoryReservedBinding(TopicExchange inventoryExchange, Queue inventoryReservedQueue) {
        return BindingBuilder.bind(inventoryReservedQueue).to(inventoryExchange).with("inventory.reserved");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }
}
