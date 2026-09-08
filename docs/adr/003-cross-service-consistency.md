# ADR-003: Cross-Service Consistency — Sagas + RabbitMQ

## Status
Accepted

## Context
Order placement spans multiple services: order → inventory → payment. Need consistency without distributed transactions.

## Decision
**Choreography-based sagas with RabbitMQ** for reliable event publishing.

### Saga: Place Order
```
1. Order Service: CREATE order (PENDING)
   └─► Publish "OrderCreated" to RabbitMQ exchange
2. Inventory Service: RESERVE stock (consumes OrderCreated)
   ├─► Success: Publish "StockReserved"
   └─► Failure: Publish "StockReservationFailed"
3. Payment Service: AUTHORIZE payment (consumes StockReserved)
   ├─► Success: Publish "PaymentAuthorized"
   └─► Failure: Publish "PaymentFailed"
4. Order Service: CONFIRM/CANCEL order (consumes PaymentAuthorized/Failed)
```

### RabbitMQ Topology
- **Exchange**: `ecommerce.events` (topic, durable)
- **Queues**: Per service (e.g., `inventory.events`, `payment.events`, `order.events`)
- **Routing keys**: `{aggregate}.{action}` (e.g., `order.created`, `inventory.stock_reserved`)
- **Dead letter exchange**: `ecommerce.events.dlx` for failed messages
- **Message format**: JSON with `eventId`, `eventType`, `aggregateId`, `payload`, `timestamp`, `correlationId`

### Reliability
- Publisher confirms (mandatory)
- Consumer acknowledgments (manual ack)
- Retry with exponential backoff (max 3) → DLX
- Idempotent consumers via `eventId` deduplication

## Rationale
- **No 2PC**: Avoids distributed transaction complexity
- **Eventual consistency**: Acceptable for e-commerce (seconds)
- **RabbitMQ**: Mature, lightweight, Spring Cloud Stream native support, good local dev story
- **Auditability**: Message broker = event log; DLX captures failures
- **Operational simplicity**: Single broker vs. per-service outbox pollers

## Consequences
- **Positive**: Reliable delivery, built-in retry/DLX, Spring Cloud Stream integration, simpler than outbox pollers
- **Negative**: Broker dependency, message ordering per queue only, network partition handling
- **Mitigation**: Idempotent consumers, correlation IDs for tracing, health checks on broker

## Alternatives Considered
- **Outbox pattern**: No broker, but poller latency, duplicate events, more code
- **Kafka + Schema Registry**: Overkill for 9 services, operational burden
- **Synchronous REST calls**: Tight coupling, cascade failures, no resilience
- **Orchestration saga**: More coupling, single point of failure