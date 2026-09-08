# 02b — Event-Driven Reservation & Cancellation Flow (RabbitMQ)

**Status:** resolved

## Problem Statement

The existing tickets (02, 03a, 05a, 05b) propose a Debezium CDC + transactional outbox pattern for event publishing. However, the domain modeling grilling session resolved on a **simpler, lower-latency** approach: direct RabbitMQ publishing with publisher confirms + consumer-side idempotency. Additionally, the grilling session established that:

- Reservations are tracked via `OrderItem.status` (not a separate entity)
- `ReservationExpiredEvent` must be published by inventory-service when a reservation TTL exceeds 15 minutes
- order-service consumes `ReservationExpiredEvent` to transition Order → CANCELLED
- Payment authorizes `orderTotal` but captures only `reservedTotal`

This creates a seam between Inventory (stock hold), Order (lifecycle), and Payment (billing) that the current tickets don't address cleanly.

## Solution

Replace the Debezium + outbox pattern with **direct RabbitMQ publishing** using publisher confirms. Reservations are managed via `OrderItem.status` (with a `reservedAt` timestamp), and `ReservationExpiredEvent` drives Order→CANCELLED transitions. Payment authorizes full `orderTotal`, captures only `reservedTotal`.

## User Stories

1. As a merchant, I want inventory to hold stock for 15 minutes after order creation, so that customers have time to complete payment.
2. As a system, I want to detect stale reservations via a 1-minute scheduler scan, so that expired holds are released promptly.
3. As an operator, I want to ensure only one scheduler instance releases reservations, so that distributed deployments don't double-release.
4. As an order-service developer, I want to consume `ReservationExpiredEvent` to cancel orders, so that the bounded context stays clean.
5. As a payment provider, I want to authorize the full order amount but capture only reserved items, so that I comply with consumer protection laws.
6. As a customer, I want my order auto-cancelled if I don't pay within 15 minutes, so that I don't lose inventory for other customers.
7. As a system, I want to handle partial reservations gracefully, so that customers can still buy available items while backordering the rest.
8. As an idempotency consumer, I want to store the `eventId` in a `processed_events` table, so that at-least-once RabbitMQ delivery doesn't cause double-processing.

## Implementation Decisions

### Replace Ticket #02 (Debezium CDC → RabbitMQ Direct)
- **RabbitMQ direct publishing**: Services publish events directly to RabbitMQ exchanges using `spring-boot-starter-amqp` with `publisher-confirm-type: correlated`
- No outbox table, no Debezium connector, no schema registry
- Publisher confirms (`RabbitTemplate.returnsCallback` + `CorrelationData`) ensure broker ack
- Consumers use `processed_events(event_id PK, processed_at)` table for idempotency (same table from ticket #01)

### Reservation Model (Update Tickets #05a, #05b)
- **No `StockReservation` entity** — reservations tracked via `OrderItem.status` (RESERVED → CANCELLED/SHIPPED/etc.)
- `OrderItem.reservedAt` (timestamp) is the TTL source; scheduler scans `OrderItem` where `status = RESERVED` and `reservedAt < now - 15 min`
- `Inventory.reservedQuantity` is the source of truth — scheduler releases by decrementing this and updating matching OrderItem status

### ReservationExpiredEvent (New)
- Published by `inventory-service` when scheduler releases a stale reservation
- Event payload: `{eventId, orderItemId, variantId, quantityReleased, reservationExpiresAt}`
- Consumed by `order-service` → transitions `OrderItem.status` → CANCELLED → if all items CANCELLED/backordered, transitions `Order.status` → CANCELLED

### Payment Flow (Update Ticket #06b)
- **Authorize `orderTotal`** at order creation (not just `reservedTotal`)
- **Capture only `reservedTotal`** (sum of RESERVED OrderItems) when Order → CONFIRMED
- Backordered items billed separately via `PaymentEvent.BACKORDER_PENDING` (future)

### Testing Seams
- **Single seam**: RabbitMQ + PostgreSQL testcontainers — publish event → verify DB state + downstream effect
- Existing `IdempotentEventProcessorTest` in `common` (ticket #01) already covers idempotency; reuse for new flows
- Scheduler tests use `updatedAt` manipulation rather than real-time waiting

### Schema Changes
- `OrderItem` — add `reservedAt` (timestamp), `status` (per-line)
- No new entity tables (remains lightweight)
- `processed_events` table per service (already in ticket #01)

### API Contracts
- **Inventory → RabbitMQ**: `ReservationExpiredEvent` published to `inventory.exchange`
- **Order → RabbitMQ**: `OrderEvent` published to `order.exchange`
- **Payment → OrderService**: consume `PaymentEvent.SUCCESS` → Order → CONFIRMED

## Testing Decisions

- **Good test**: Verify Order→CANCELLED when `ReservationExpiredEvent` consumed with stale reservation
- **Good test**: Verify Payment captures only `reservedTotal`, not `orderTotal`
- **Good test**: Idempotent consumption of `ReservationExpiredEvent` — duplicate event does not double-release
- **Modules**: inventory-service, order-service, payment-service, common
- **Prior art**: `IdempotentEventProcessorTest` in `common` module (ticket #01) for idempotency; `inventory-service` integration tests for ReservationExpiredEvent handling

## Out of Scope
- Backorder billing (deferred to future: `PaymentEvent.BACKORDER_PENDING`)
- Multi-instance scheduler clustering (single instance assumed; `pg_advisory_lock` noted as future enhancement)
- Real-time payment gateway integration (mocked in tests)

## Further Notes
- This spec obsoletes ticket #02 (Debezium CDC) — Debezium dependency removed from POMs
- Tickets #03a, #05a, #05b update: replace "write to outbox" with "publish to RabbitMQ directly"
