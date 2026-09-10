# Ticket Mapping: domain-model → full-build

## Overview
- **domain-model** (17 tickets): Granular, domain-focused tickets for order, inventory, payment bounded contexts
- **full-build** (16 tickets): Broader, platform/infrastructure tickets for all 9 services + platform

This mapping ensures traceability between the two ticket sets.

## Mapping Table

| domain-model Ticket | full-build Ticket(s) | Notes |
|---------------------|---------------------|-------|
| 01-event-idempotency | 00-common-module | Idempotency foundation (`ProcessedEvent`, `IdempotentEventListener`) |
| 02-debezium-cdc | — | Superseded by RabbitMQ decision (ADR-006) |
| 02b-event-driven-reservation-cancellation-rabbitmq | 07-inventory-service (scheduler), 09-order-service (consumer) | Inventory expiry → order cancellation flow |
| 03a-product-variant | 05-product-service | Product/variant schema, API |
| 03b-category-hierarchy | 06-category-service | Category tree, hierarchy API |
| 04a-order-item-aggregate | 09-order-service | OrderItem entity, status transitions |
| 04b-shipment-aggregate | 09-order-service | ShipmentItem denormalization (out of scope for MVP) |
| 04c-order-cancellation-on-reservation-expiry | 07-inventory-service (publishes), 09-order-service (consumes) | `ReservationExpiredEvent` flow |
| 05a-inventory-reservation-core | 07-inventory-service | Reserve/release stock, reservation entity |
| 05b-inventory-ttl-scheduler | 07-inventory-service | Scheduler for expired reservations |
| 06a-payment-scaffold | 08-payment-service | Module scaffold, build, CI |
| **06b-payment-auth-capture** | **08-payment-service** | **Current work: authorize/capture, idempotency, events** |
| 07-order-payment-integration | 09-order-service (consumer), 08-payment-service (publisher) | Saga: order.created → stock_reserved → payment.authorized → order.confirmed |
| 08-notifications | — | Out of scope (notification-service not in 9 services) |
| 09a-system-tests-happy-path | 13-e2e-tests (Playwright) | Browse → order → pay → confirm |
| 09b-system-tests-edge-cases | 13-e2e-tests (Playwright) | Payment failure, reservation expiry, backorder |
| 09c-system-tests-load | 13-e2e-tests (Playwright) | Load testing (may be separate k6/Gatling) |

## Reverse Mapping (full-build → domain-model)

| full-build Ticket | domain-model Ticket(s) | Notes |
|-------------------|------------------------|-------|
| 00-common-module | 01-event-idempotency | Shared events, idempotency, DTOs |
| 01-config-server | — | New infra ticket |
| 02-eureka-server | — | New infra ticket |
| 03-auth-server | — | New infra ticket |
| 04-gateway | — | New infra ticket |
| 05-product-service | 03a-product-variant | |
| 06-category-service | 03b-category-hierarchy | |
| 07-inventory-service | 02b, 04c, 05a, 05b | Inventory core + expiry + events |
| 08-payment-service | 06a, 06b | Payment scaffold + auth/capture |
| 09-order-service | 04a, 04c, 07 | Order aggregate + cancellation + integration |
| 10-rabbitmq | — | New infra ticket |
| 11-docker-compose | — | New infra ticket |
| 12-contract-tests | — | New platform ticket |
| 13-e2e-tests | 09a, 09b, 09c | System tests → Playwright E2E |
| 14-ci-cd | — | New platform ticket |
| 15-observability | — | New platform ticket |

## Status Tracking

| domain-model | Status | full-build | Status |
|--------------|--------|------------|--------|
| 01-event-idempotency | ready-for-agent | 00-common-module | ready-for-agent |
| 02-debezium-cdc | deprecated | — | — |
| 02b-reservation-cancellation | ready-for-agent | 07-inventory-service | ready-for-agent |
| 03a-product-variant | ready-for-agent | 05-product-service | ready-for-agent |
| 03b-category-hierarchy | ready-for-agent | 06-category-service | ready-for-agent |
| 04a-order-item-aggregate | ready-for-agent | 09-order-service | blocked (needs 07,08) |
| 04b-shipment-aggregate | deferred | — | out of scope |
| 04c-cancellation-expiry | ready-for-agent | 07/09 | ready-for-agent |
| 05a-inventory-core | ready-for-agent | 07-inventory-service | ready-for-agent |
| 05b-inventory-ttl | ready-for-agent | 07-inventory-service | ready-for-agent |
| 06a-payment-scaffold | done | 08-payment-service | ready-for-agent |
| **06b-payment-auth-capture** | **done** | **08-payment-service** | **done** |
| 07-order-payment-integration | **done** | 09-order-service | **done** |
| 08-notifications | out of scope | — | — |
| 09a-happy-path | ready-for-agent | 13-e2e-tests | blocked |
| 09b-edge-cases | ready-for-agent | 13-e2e-tests | blocked |
| 09c-load | ready-for-agent | 13-e2e-tests | blocked |

## Workflow

1. **Current**: Complete domain-model/06b → then 07, 09a/b/c
2. **Parallel**: Start full-build frontier (00, 01, 02, 10) — no blockers
3. **Integration**: When domain-model 07 done, unblocks full-build 09
4. **Platform**: After 09, unblocks 11, 12, 13, 14, 15

## Sync Protocol

- When a domain-model ticket completes, update its status here and check if it unblocks any full-build ticket
- When a full-build ticket completes, update status here
- Keep this document as single source of truth for cross-ticket dependencies