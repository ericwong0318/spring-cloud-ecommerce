package com.example.system;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.example.common.event.BaseEvent;

/**
 * Utility for collecting events from RabbitMQ for assertions in tests.
 * Subscribes to event topics and stores received events in a thread-safe list.
 */
public class EventCollector {

    private final RabbitTemplate rabbitTemplate;
    private volatile java.util.List<BaseEvent> events = new java.util.concurrent.CopyOnWriteArrayList<>();

    public EventCollector(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Gets the list of collected events.
     * 
     * @return the list of collected events
     */
    public java.util.List<BaseEvent> getEvents() {
        return new java.util.ArrayList<>(events);
    }

    /**
     * Clears all collected events.
     */
    public void clear() {
        events.clear();
    }

    /**
     * Waits for an event matching the given event type within the timeout.
     * 
     * @param eventType the event type to wait for
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout
     * @return the matching event, or null if timeout
     */
    public BaseEvent awaitEvent(String eventType, long timeout, Supplier<LocalDateTime> nowSupplier, java.util.concurrent.TimeUnit unit) {
        return Awaitility.await()
                .atMost(timeout, unit)
                .until(() -> events.stream().anyMatch(e -> eventType.equals(e.getEventType())),
                       b -> b);
    }

    /**
     * Waits for an event matching the given event type and event ID within the timeout.
     * 
     * @param eventType the event type to wait for
     * @param eventId the event ID to wait for
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout
     * @return the matching event, or null if timeout
     */
    public BaseEvent awaitEvent(String eventType, UUID eventId, long timeout, Supplier<LocalDateTime> nowSupplier, java.util.concurrent.TimeUnit unit) {
        return Awaitility.await()
                .atMost(timeout, unit)
                .until(() -> events.stream()
                        .filter(e -> eventType.equals(e.getEventType()) && e.getEventId().equals(eventId))
                        .findFirst()
                        .orElse(null),
                       b -> b != null);
    }

    /**
     * Checks if an event of the given type was received.
     * 
     * @param eventType the event type to check
     * @return true if the event was received, false otherwise
     */
    public boolean hasEvent(String eventType) {
        return events.stream().anyMatch(e -> eventType.equals(e.getEventType()));
    }

    /**
     * Gets all events of the given type.
     * 
     * @param eventType the event type to filter by
     * @return the list of matching events
     */
    public java.util.List<BaseEvent> getEventsByType(String eventType) {
        return events.stream()
                .filter(e -> eventType.equals(e.getEventType()))
                .toList();
    }

    /**
     * Gets the count of events of the given type.
     * 
     * @param eventType the event type to count
     * @return the count of matching events
     */
    public long countEvents(String eventType) {
        return events.stream().filter(e -> eventType.equals(e.getEventType())).count();
    }

    /**
     * Publishes an event to the specified exchange and routing key.
     * 
     * @param event the event to publish
     * @param exchange the exchange name
     * @param routingKey the routing key
     */
    public void publishEvent(BaseEvent event, String exchange, String routingKey) {
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }

    /**
     * Listener method for receiving events from RabbitMQ.
     * In a real test setup, this would be annotated with @RabbitListener.
     * 
     * @param event the received event
     */
    public void onEventReceived(BaseEvent event) {
        events.add(event);
    }
}