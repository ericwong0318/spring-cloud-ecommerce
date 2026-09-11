package com.example.payment.event;

import com.example.common.event.PaymentEvent;
import com.example.payment.domain.ProcessedEvent;
import com.example.payment.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ProcessedEventRepository processedEventRepository;
    private final String exchange;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate, ProcessedEventRepository processedEventRepository,
                                 @Value("${rabbitmq.exchange.payment}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.processedEventRepository = processedEventRepository;
        this.exchange = exchange;
    }

    public Mono<Void> publish(PaymentEvent event) {
        String routingKey = switch (event.getEventType()) {
            case "AUTHORIZED" -> "payment.authorized";
            case "CAPTURED" -> "payment.captured";
            case "REFUNDED" -> "payment.refunded";
            case "FAILED" -> "payment.failed";
            default -> "payment.unknown";
        };

        UUID eventId = event.getEventId();

        return processedEventRepository.findByEventId(eventId)
                .flatMap(existing -> {
                    log.info("Event {} already processed, skipping", eventId);
                    return Mono.<Void>empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    CorrelationData correlationData = new CorrelationData(eventId.toString());

                    return Mono.fromRunnable(() -> {
                        rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
                            message.getMessageProperties().setMessageId(eventId.toString());
                            message.getMessageProperties().setContentType("application/json");
                            return message;
                        }, correlationData);
                        log.info("Published PaymentEvent {} to {} with routing key {}", eventId, exchange, routingKey);
                    })
                    .then(processedEventRepository.save(eventId, java.time.LocalDateTime.now())
                            .then())
                    .then();
                }));
    }
}
