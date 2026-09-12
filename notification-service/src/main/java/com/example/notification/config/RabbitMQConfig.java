package com.example.notification.config;

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

    @Value("${rabbitmq.queue.notification-order-events}")
    private String notificationOrderEventsQueue;

    @Value("${rabbitmq.queue.notification-payment-events}")
    private String notificationPaymentEventsQueue;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

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

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(orderExchange, true, false);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(paymentExchange, true, false);
    }

    @Bean
    public Queue notificationOrderEventsQueue() {
        return new Queue(notificationOrderEventsQueue, true);
    }

    @Bean
    public Queue notificationPaymentEventsQueue() {
        return new Queue(notificationPaymentEventsQueue, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(notificationOrderEventsQueue())
                .to(orderExchange())
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(notificationOrderEventsQueue())
                .to(orderExchange())
                .with(orderCancelledRoutingKey);
    }

    @Bean
    public Binding paymentAuthorizedBinding() {
        return BindingBuilder.bind(notificationPaymentEventsQueue())
                .to(paymentExchange())
                .with(paymentAuthorizedRoutingKey);
    }

    @Bean
    public Binding paymentCapturedBinding() {
        return BindingBuilder.bind(notificationPaymentEventsQueue())
                .to(paymentExchange())
                .with(paymentCapturedRoutingKey);
    }

    @Bean
    public Binding paymentRefundedBinding() {
        return BindingBuilder.bind(notificationPaymentEventsQueue())
                .to(paymentExchange())
                .with(paymentRefundedRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(notificationPaymentEventsQueue())
                .to(paymentExchange())
                .with(paymentFailedRoutingKey);
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
