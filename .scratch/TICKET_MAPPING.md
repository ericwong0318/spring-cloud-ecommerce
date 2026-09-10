# Ticket Tracking: domain-model

## Overview
- **domain-model** (17 tickets): Granular, domain-focused tickets for order, inventory, payment bounded contexts

## Ticket Status

| Ticket | Description | Status |
|--------|-------------|--------|
| 01-event-idempotency | Idempotency foundation (`ProcessedEvent`, `IdempotentEventListener`) | ready-for-agent |
| 02-debezium-cdc | Superseded by RabbitMQ decision (ADR-006) | deprecated |
| 02b-event-driven-reservation-cancellation-rabbitmq | Inventory expiry → order cancellation flow | ready-for-agent |
| 03a-product-variant | Product/variant schema, API | ready-for-agent |
| 03b-category-hierarchy | Category tree, hierarchy API | ready-for-agent |
| 04a-order-item-aggregate | OrderItem entity, status transitions | ready-for-agent |
| 04b-shipment-aggregate | ShipmentItem denormalization | deferred (out of scope) |
| 04c-order-cancellation-on-reservation-expiry | `ReservationExpiredEvent` flow | ready-for-agent |
| 05a-inventory-reservation-core | Reserve/release stock, reservation entity | **done** |
| 05b-inventory-ttl-scheduler | Scheduler for expired reservations | **done** |
| 06a-payment-scaffold | Module scaffold, build, CI | done |
| 06b-payment-auth-capture | Authorize/capture, idempotency, events | **done** |
| 07-order-payment-integration | Saga: order.created → stock_reserved → payment.authorized → order.confirmed | **done** |
| 08-notifications | Out of scope | out of scope |
| 09a-system-tests-happy-path | Browse → order → pay → confirm | ready-for-agent |
| 09b-system-tests-edge-cases | Payment failure, reservation expiry, backorder | ready-for-agent |
| 09c-system-tests-load | Load testing (may be separate k6/Gatling) | ready-for-agent |
| cleanup-debezium-removal | Remove all Debezium artifacts (docker, POM, SQL, scripts, tickets) | ready-for-agent |

## Workflow

1. **Current**: 06b done → 07 done → 02b, 04c, 05a, 05b (inventory), 03a, 03b (product/category)
2. **Integration**: 02b, 04c, 05a, 05b → 09a/b/c (system tests)
3. **Platform**: After 09, unblocks CI/CD, observability, contract tests

## Sync Protocol

- When a domain-model ticket completes, update its status here
- Keep this document as single source of truth for cross-ticket dependencies
