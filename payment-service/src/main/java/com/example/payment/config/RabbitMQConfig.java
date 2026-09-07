package com.example.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange.payment}")
    private String paymentExchange;

    @Value("${rabbitmq.queue.payment-events}")
    private String paymentEventsQueue;

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(paymentExchange, true, false);
    }

    @Bean
    public Queue paymentEventsQueue() {
        return new Queue(paymentEventsQueue, true);
    }

    @Bean
    public Binding paymentAuthorizedBinding(DirectExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.authorized");
    }

    @Bean
    public Binding paymentCapturedBinding(DirectExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.captured");
    }

    @Bean
    public Binding paymentRefundedBinding(DirectExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.refunded");
    }

    @Bean
    public Binding paymentFailedBinding(DirectExchange paymentExchange, Queue paymentEventsQueue) {
        return BindingBuilder.bind(paymentEventsQueue).to(paymentExchange).with("payment.failed");
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
}