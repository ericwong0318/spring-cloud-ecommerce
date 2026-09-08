# ADR-003: Cross-Service Consistency — Sagas + Transactional Outbox

## Status
Accepted

## Context
Order placement spans multiple services: order → inventory → payment. Need consistency without distributed transactions.

## Decision
**Choreography-based sagas with transactional outbox pattern** for event publishing.

### Saga: Place Order
```
1. Order Service: CREATE order (PENDING)
   └─► Persist Order + OutboxEvent("OrderCreated")
2. Inventory Service: RESERVE stock (via OrderCreated event)
   ├─► Success: OutboxEvent("StockReserved")
   └─► Failure: OutboxEvent("StockReservationFailed")
3. Payment Service: AUTHORIZE payment (via StockReserved event)
   ├─► Success: OutboxEvent("PaymentAuthorized")
   └─► Failure: OutboxEvent("PaymentFailed")
4. Order Service: CONFIRM/CANCEL order (via PaymentAuthorized/Failed)
```

### Outbox Pattern
- Each service has `outbox_event` table (id, aggregate_id, event_type, payload, created_at, published_at)
- In same transaction as domain change, write event to outbox
- Background poller (or transaction log miner) publishes to message broker
- **No Kafka** — using database as the event store; poller writes to a simple in-memory bus for local dev, can swap to Kafka later

## Rationale
- **No 2PC**: Avoids distributed transaction complexity and locking
- **Eventual consistency**: Acceptable for e-commerce (seconds, not milliseconds)
- **Auditability**: Outbox table = event log for debugging/replay
- **No Kafka dependency**: Simpler local dev, fewer moving parts
- **Idempotency**: Events carry correlation IDs; consumers deduplicate

## Consequences
- **Positive**: Resilient, scalable, auditable, no message broker ops
- **Negative**: Eventual consistency (temporary inconsistencies), outbox poller latency, saga complexity
- **Mitigation**: Compensating transactions for failures, idempotent consumers, circuit breakers

## Alternatives Considered
- **Orchestration saga (state machine in order service)**: More coupling, single point of failure
- **Kafka + Schema Registry**: Overkill for 9 services, operational burden
- **Synchronous REST calls**: Tight coupling, cascade failures, no resilience