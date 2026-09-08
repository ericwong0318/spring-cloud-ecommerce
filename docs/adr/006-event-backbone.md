# ADR-006: Event Backbone — Database Outbox Pattern

## Status
Accepted

## Context
Sagas need reliable event publishing without a message broker. Events must survive process crashes.

## Decision
**Transactional outbox pattern** using PostgreSQL as the event store.

### Schema (per service)
```sql
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);
CREATE INDEX idx_outbox_unpublished ON outbox_event (created_at) WHERE published_at IS NULL;
```

### Publisher
- **Local dev**: In-memory `ApplicationEventPublisher` → synchronous consumers (no poller needed)
- **Production**: Background poller (Spring `@Scheduled` every 100ms) → publishes to message broker (Kafka, RabbitMQ, or HTTP webhook)
- **Exactly-once**: Consumers deduplicate via `aggregate_id + event_type` (idempotency key)

### Event Types (examples)
| Aggregate | Events |
|-----------|--------|
| Order | OrderCreated, OrderConfirmed, OrderCancelled |
| Inventory | StockReserved, StockReleased, StockAdjusted |
| Payment | PaymentAuthorized, PaymentFailed, PaymentRefunded |

## Rationale
- **No message broker dependency**: PostgreSQL already running; outbox = transaction log
- **Atomicity**: Domain change + event = single transaction (no dual-write problem)
- **Replayability**: Full event history for debugging, new consumers, projections
- **Swapability**: Poller target configurable (Kafka, HTTP, in-memory) without domain changes
- **Local dev simplicity**: In-memory publisher = zero infrastructure

## Consequences
- **Positive**: Reliable, auditable, no broker ops, works offline
- **Negative**: Poller latency (100ms), potential duplicate events, schema coupling
- **Mitigation**: Idempotent consumers, event versioning in payload, outbox cleanup job

## Alternatives Considered
- **Kafka + Transactional Outbox (Kafka Connect)**: Requires Kafka cluster
- **CDC (Debezium)**: Operational complexity, schema coupling to DB internals
- **Direct HTTP calls**: No durability, coupling, no replay