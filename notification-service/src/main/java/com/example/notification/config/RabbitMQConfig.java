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

    @Value("${rabbitmq.queue.notification-events}")
    private String notificationEventsQueue;

    @Value("${rabbitmq.routing-key.order-created}")
    private String orderCreatedRoutingKey;

    @Value("${rabbitmq.routing-key.order-cancelled}")
    private String orderCancelledRoutingKey;

    @Value("${rabbitmq.routing-key.payment-success}")
    private String paymentSuccessRoutingKey;

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
    public Queue notificationEventsQueue() {
        return new Queue(notificationEventsQueue, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder.bind(notificationEventsQueue())
                .to(orderExchange())
                .with(orderCreatedRoutingKey);
    }

    @Bean
    public Binding orderCancelledBinding() {
        return BindingBuilder.bind(notificationEventsQueue())
                .to(orderExchange())
                .with(orderCancelledRoutingKey);
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(notificationEventsQueue())
                .to(paymentExchange())
                .with(paymentSuccessRoutingKey);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(notificationEventsQueue())
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
