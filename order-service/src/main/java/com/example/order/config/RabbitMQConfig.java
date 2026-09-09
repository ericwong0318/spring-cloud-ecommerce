package com.example.order.config;

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

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.exchange.inventory}")
    private String inventoryExchange;

    @Value("${rabbitmq.exchange.ecommerce}")
    private String ecommerceExchange;

    @Value("${rabbitmq.queue.order-events}")
    private String orderEventsQueue;

    @Value("${rabbitmq.queue.payment-events}")
    private String paymentEventsQueue;

    @Value("${rabbitmq.queue.inventory-events}")
    private String inventoryEventsQueue;

    @Value("${rabbitmq.queue.reservation-expired}")
    private String reservationExpiredQueue;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-updated}")
    private String orderUpdatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-cancelled}")
    private String orderCancelledRoutingKey;

    @Value("${rabbitmq.routing-key.payment-authorized}")
    private String paymentAuthorizedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-captured}")
    private String paymentCapturedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-refunded}")
    private String paymentRefundedRoutingKey;

    @Value("${rabbitmq.routing-key.payment-failed}")
    private String paymentFailedRoutingKey;

    @Value("${rabbitmq.routing-key.inventory-reserved}")
    private String inventoryReservedRoutingKey;

    @Value("${rabbitmq.routing-key.reservation-expired}")
    private String reservationExpiredRoutingKey;

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange, true, false);
    }

    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(inventoryExchange, true, false);
    }

    @Bean
    public TopicExchange ecommerceExchange() {
        return new TopicExchange(ecommerceExchange, true, false);
    }

    @Bean
    public Queue orderEventsQueue() {
        return new Queue(orderEventsQueue, true);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(paymentEventsQueue, true);
    }

    @Bean
    public Queue inventoryEventsQueue() {
        return new Queue(inventoryEventsQueue, true);
    }

    @Bean
    public Queue reservationExpiredQueue() {
        return new Queue(reservationExpiredQueue, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(orderEventsQueue())
                .to(orderExchange())
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderUpdatedBinding() {
        return BindingBuilder.bind(orderEventsQueue())
                .to(orderExchange())
                .with(orderUpdatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(orderEventsQueue())
                .to(orderExchange())
                .with(orderCancelledRoutingKey);
    }

    @Bean
    public Binding paymentAuthorizedBinding() {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(paymentExchange())
                .with(paymentAuthorizedRoutingKey);
    }

    @Bean
    public Binding paymentCapturedBinding() {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(paymentExchange())
                .with(paymentCapturedRoutingKey);
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(paymentExchange())
                .with(paymentRefundedRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentEventsQueue())
                .to(paymentExchange())
                .with(paymentFailedRoutingKey);
    }

    @Bean
    public Binding inventoryReservedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(inventoryExchange())
                .with(inventoryReservedRoutingKey);
    }

    @Bean
    public Binding inventoryReleasedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(inventoryExchange())
                .with("released");
    }

    @Bean
    public Binding inventoryConfirmedBinding() {
        return BindingBuilder.bind(inventoryEventsQueue())
                .to(inventoryExchange())
                .with("confirmed");
    }

    @Bean
    public Binding reservationExpiredBinding() {
        return BindingBuilder.bind(reservationExpiredQueue())
                .to(inventoryExchange())
                .with(reservationExpiredRoutingKey);
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