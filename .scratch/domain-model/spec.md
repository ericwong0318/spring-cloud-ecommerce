# Domain Model Implementation Specification — Order Management & Payments

## Problem Statement

The system needs a robust, compliant order-to-payment flow that respects legal/regulatory constraints (charging only for stock that is actually reserved) while maintaining loose coupling between Order, Inventory, and Payment bounded contexts.

## Solution

A decoupled, event-driven architecture where:
- **Order** is the aggregate root; it owns OrderItems with per-line status (PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED).
- **Inventory** manages stock and reserves via `reservedQuantity`; it publishes `ReservationExpiredEvent` when a reservation exceeds 15 minutes.
- **Payment** authorizes the full `orderTotal` at order creation but captures only `reservedTotal` (sum of RESERVED OrderItems) upon order confirmation. Backordered items trigger a separate billing event.
- **Events** are published directly to RabbitMQ with publisher confirms; consumers use database idempotency tables.
- **ShipmentItems** are denormalized copies linked by `orderItemId` (no cross-aggregate cascades).
- **Cancellation** is driven by Inventory → `ReservationExpiredEvent` → order-service transitions Order to CANCELLED.

## User Stories

1. As a customer, I want to place an order with multiple line items, so that I can purchase products in one transaction.
2. As a customer, I want to see my order status (PENDING, RESERVED, SHIPPED, DELIVERED, CANCELLED), so that I know the current state of my purchase.
3. As a merchant, I want to reserve inventory for each order item before confirming payment, so that stock isn't lost if the order is cancelled.
4. As a merchant, I want to capture payment only for reserved items (not backordered ones), so that I comply with consumer protection laws requiring payment only for goods ready to ship.
5. As a system, I want to automatically cancel orders whose reservations expire after 15 minutes, so that stale reservations don't block inventory indefinitely.
6. As a system, I want to ship orders in multiple shipments (multi-shipping), so that customers can receive items from different warehouses.
7. As a system, I want to track shipments with tracking numbers and carrier information, so that customers can monitor delivery status.
8. As a customer, I want to receive notifications (email/SMS) at key milestones (order placed, reserved, shipped, delivered), so that I stay informed.
9. As a payment provider, I want to authorize the full order amount at order creation, but capture only the reserved portion upon confirmation, so that I can reconcile revenue accurately.
10. As a payment service, I want to support idempotency keys to ensure exactly-once charging even if retries occur due to network failures.
11. As a system, I want to handle backorders transparently — when stock is insufficient, reserve what's available and backorder the remainder, allowing partial fulfillment.
12. As a system, I want to support partial refunds for cancelled orders, so that customers can recover costs for partially fulfilled orders.
13. As an operator, I want to view a consolidated view of all active orders, reservations, and shipments, so that I can manage the system holistically.
14. As a developer, I want clear bounded contexts with well-defined responsibilities, so that teams can work independently without conflicts.

## Implementation Decisions

### Modules to Build/Modify
- **order-service** — Add `OrderItem` entity with status, `reservedAt` timestamp; add `ReservationExpiredEvent` publication; add `ReservationExpiredHandler` that transitions Order to CANCELLED.
- **inventory-service** — Add `ReservationExpiredEvent` publication when `reservedQuantity` exceeds 15 minutes; ensure `reservedQuantity` decrement on cancellation.
- **payment-service** — Add `PaymentEvent` with SUCCESS/FAILED/REFUNDED; implement idempotency key handling; implement authorize/capture flow; implement partial refund workflow.
- **common** — Add `ProcessedEvent` table for idempotency; add `eventId` UUID column to all event publishers.
- **shared** — Add `ReservationExpiredEvent` to integration events; ensure `order-service` consumes it and transitions orders.

### Interfaces / APIs
- **Order → Inventory**: `reserve()` / `release()` methods on OrderItem (internal)
- **Order → Payment**: `authorize(orderId)` → returns `orderTotal`; `capture(reservedTotal)` → returns `paymentId`
- **Order → OrderService**: `cancel()` → transitions Order to CANCELLED
- **Inventory → OrderService**: `reservationExpired()` → triggers cancellation
- **Payment → OrderService**: `refund(orderId, amount)` → marks order as refunded
- **Event bus**: All services publish events to RabbitMQ (outbox pattern replaced by direct publishing with publisher confirms)

### Technical Clarifications
- **Reservation TTL**: Fixed 15 minutes; no extension mechanism. Payment delays beyond 15 min result in reservation expiry and potential oversell (business risk accepted).
- **Payment Authorization**: Full `orderTotal` is authorized at order creation; only `reservedTotal` (sum of RESERVED OrderItems) is captured upon confirmation. Backordered items are charged separately via a backorder billing event.
- **Idempotency**: Every payment request includes a unique `idempotencyKey` (UUID) to guarantee exactly-once charging.
- **ShipmentItems**: Denormalized copy with `orderItemId` reference; no cross-aggregate cascades (Inventory → ShipmentItems via OrderItem status transitions).
- **Event Delivery**: RabbitMQ direct publishing with publisher confirms; consumers use `processed_events(event_id PK, processed_at)` table for idempotency.
- **Outbox**: Replaced by direct RabbitMQ publishing (simpler, lower latency). Outbox pattern was considered but rejected due to operational complexity.

### Schema Changes
- **OrderItem** — add `status` (PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED), `reservedAt` (timestamp)
- **Order** — no structural changes (already has status enum)
- **Inventory** — no structural changes (already has `reservedQuantity`)
- **Payment** — new `PaymentEvent` enum values (SUCCESS, FAILED, REFUNDED)
- **New event**: `ReservationExpiredEvent` (triggered by Inventory when reservation > 15 min)

### API Contracts
- **OrderService** — `POST /orders` (creates Order + OrderItems), `PUT /orders/{id}/cancel`, `GET /orders/{id}`, `POST /orders/{id}/reserve` (internal), `POST /orders/{id}/capture` (internal)
- **PaymentService** — `POST /payments/authorize`, `POST /payments/{id}/capture`, `POST /payments/{id}/refund`
- **Event consumption** — `ReservationExpiredEvent` consumed by `order-service` to transition Order to CANCELLED

## Testing Decisions
- **External behavior testing**: Tests should verify order state transitions, reservation expiry, payment authorization/capture, and cancellation flow.
- **Unit tests**: Each service module will have unit tests for core logic (status transitions, reservation calculations, payment flows).
- **Integration tests**: End-to-end tests for order → reserve → capture → cancel → cancellation flow; payment authorization/capture; shipment creation.
- **Prior art**: Existing `system-test` module already uses Testcontainers for PostgreSQL; similar pattern will be used for payment and order integration tests.
- **Idempotency tests**: Verify that duplicate payment requests with same idempotency key produce the same result.
- **Reservation expiry tests**: Simulate time passing beyond 15 minutes and verify order cancellation.
- **Backorder flow**: Test partial reservation + backorder scenario; verify backordered items are charged separately.

## Out of Scope
- **Notification service enhancements** (beyond basic email/SMS templates)
- **Advanced fraud detection** (beyond basic idempotency)
- **Multi-region deployment** (infrastructure concerns, not domain model)
- **Audit logging** (separate compliance module)
- **Real-time analytics dashboards** (operational tooling)

## Further Notes
- The `ReservationExpiredEvent` is the key seam connecting Inventory → OrderService → Payment (indirectly). This is the highest-seeming seam for testing.
- Payment authorization captures the full order amount but only captures reserved stock — this prevents charging for backordered items.
- ShipmentItems are denormalized for performance; the primary source of truth remains OrderItem status.
- All event publishing uses RabbitMQ with publisher confirms; consumers rely on idempotency tables for safety.
- The `order-service` is responsible for cancelling orders when reservations expire, keeping the bounded context clean.
