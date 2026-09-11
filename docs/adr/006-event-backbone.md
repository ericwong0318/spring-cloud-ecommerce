# ADR-006: Event Backbone — RabbitMQ

## Status
Accepted

## Context
Sagas need reliable event publishing between services. Chosen: RabbitMQ over Kafka or outbox pattern.

## Decision
**RabbitMQ** as the message broker for all inter-service events.

### Topology
```
Exchange: ecommerce.events (topic, durable)
  │
  ├─► Queue: inventory.events (durable)
  │     Routing: inventory.*, stock.*
  │
  ├─► Queue: payment.events (durable)
  │     Routing: payment.*
  │
  ├─► Queue: order.events (durable)
  │     Routing: order.*
  │
  └─► DLX: ecommerce.events.dlx (topic)
        └─► Queue: ecommerce.dlq (durable)
```

### Message Contract
```json
{
  "eventId": "uuid",
  "eventType": "order.created",
  "aggregateId": "uuid",
  "aggregateType": "Order",
  "payload": { ... },
  "metadata": {
    "correlationId": "uuid",
    "causationId": "uuid",
    "timestamp": "ISO8601"
  }
}
```

### Spring Cloud Stream Configuration
```yaml
spring:
  cloud:
    stream:
      binders:
        rabbit:
          type: rabbit
          environment:
            spring:
              rabbitmq:
                host: ${RABBITMQ_HOST:localhost}
                port: ${RABBITMQ_PORT:5672}
      bindings:
        orderCreated-out-0:
          destination: ecommerce.events
          producer:
            routing-key-expression: "'order.created'"
        stockReserved-in-0:
          destination: ecommerce.events
          group: inventory
        stockReserved-out-0:
          destination: ecommerce.events
          producer:
            routing-key-expression: "'inventory.stock_reserved'"
```

### Local Dev
- `docker-compose.yml` includes RabbitMQ (management UI on :15672)
- Testcontainers `rabbitmq` module for integration tests

## Rationale
- **Spring Cloud Stream native**: Zero-config binding, automatic serialization
- **Lightweight**: Single Erlang VM, low resource usage
- **Rich routing**: Topic exchange fits saga routing keys naturally
- **DLX + retry**: Built-in dead letter handling
- **Management UI**: Visibility into queues, rates, stuck messages
- **Testcontainers support**: First-class `RabbitMQContainer`

## Consequences
- **Positive**: Declarative config, reliable delivery, good observability, local dev parity
- **Negative**: Broker operational concern, single point of failure (mitigate with HA cluster in prod)
- **Mitigation**: Health checks, publisher confirms, consumer acks, DLX monitoring

## Alternatives Considered
- **Outbox pattern**: No broker ops, but poller latency, duplicate events, more custom code
- **Kafka**: Higher throughput, but heavier, schema registry overhead, overkill for 9 services
- **HTTP callbacks**: No durability, coupling, no retry semantics
