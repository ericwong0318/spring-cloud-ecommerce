# Spec: Spring Cloud E-Commerce Platform — Domain Model Implementation

## Problem Statement

The current Spring Cloud microservices platform has a working but incomplete domain model. Key entities are missing (OrderItem, ProductVariant, Shipment, Payment), relationships are not properly modeled (Order→OrderItem aggregate, Category hierarchy), and critical business rules are not encoded (reservation TTL, partial fulfillment, idempotency). The codebase has integration events but no deduplication mechanism. The team needs a clear, agreed-upon domain model to guide implementation across all 11 services.

## Solution

Implement the complete domain model as defined in the grilling session, codified in `CONTEXT.md`. This includes:

1. **Product Catalog**: Product + Variant (SKU) model, hierarchical Categories
2. **Order Management**: Order aggregate with OrderItem value objects, per-line status, multi-shipment support via Shipment entity
3. **Inventory Management**: Clear field semantics (on-hand vs available), reservation TTL (15 min), partial reservation with backorder
4. **Notifications**: Fixed retry policy (3 retries × 5 min), template system, multi-channel fallback
5. **Payment**: New `payment-service` module with authorization/capture flow
6. **Integration Events**: At-least-once delivery with UUID-per-event idempotency stored in DB tables

## User Stories

### Product Catalog
1. As a **merchant**, I want to **define a base Product with multiple Variants (SKUs) for size/color**, so that **customers can purchase specific configurations with accurate pricing and stock**.
2. As a **merchant**, I want to **organize Categories in a parent-child hierarchy**, so that **customers can browse logically (Electronics → Computers → Laptops)**.
3. As a **system**, I want to **publish ProductEvent.CREATED/UPDATED/DELETED with variant details**, so that **inventory-service can create Inventory records per variant**.

### Order Management
4. As a **customer**, I want to **place an Order with multiple line items**, so that **I can buy several products in one transaction**.
5. As a **customer**, I want to **see per-line status (PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED)**, so that **I know exactly which items are shipping when**.
6. As a **merchant**, I want to **ship an Order in multiple Shipments with tracking numbers**, so that **I can fulfill from different warehouses or split backordered items**.
7. As a **system**, I want to **transition Order status: PENDING → CONFIRMED → (partial) SHIPPED → DELIVERED**, so that **the lifecycle reflects real fulfillment progress**.
8. As a **system**, I want to **publish OrderEvent.CREATED with full line items on order creation**, so that **inventory-service can reserve stock immediately**.

### Inventory Management
9. As a **merchant**, I want to **track on-hand physical stock separately from available-to-sell**, so that **reserved units don't inflate sellable quantity**.
10. As a **system**, I want to **reserve stock for 15 minutes on OrderEvent.CREATED**, so that **customers have time to complete payment without losing inventory**.
11. As a **system**, I want to **auto-release reservations older than 15 minutes via scheduler**, so that **abandoned carts don't permanently block stock**.
12. As a **customer**, I want to **partially reserve an order line when stock is low (reserve available, backorder rest)**, so that **I can still purchase what's available**.
13. As a **merchant**, I want to **receive LOW_STOCK events when availableQuantity ≤ reorderLevel**, so that **I can reorder before stockouts**.

### Payment
14. As a **customer**, I want to **pay for an Order via authorization then capture**, so that **funds are held at order time but only captured on fulfillment**.
15. As a **system**, I want to **publish PaymentEvent.SUCCESS/FAILED/REFUNDED with idempotency keys**, so that **order-service can transition to CONFIRMED or CANCELLED reliably**.
16. As a **merchant**, I want to **process full and partial refunds**, so that **I can handle returns and goodwill adjustments**.

### Notifications
17. As a **customer**, I want to **receive order confirmation, payment success/failure, and shipment emails**, so that **I'm informed at every stage**.
18. As a **system**, I want to **retry failed notifications 3 times at 5-minute intervals before marking FAILED**, so that **transient email/SMS issues self-heal**.
19. As a **merchant**, I want to **use templates for notification content**, so that **branding and messaging are consistent without code changes**.
20. As a **customer**, I want to **receive SMS as fallback if email fails**, so that **critical notifications (payment failed) always reach me**.

### Integration Events
21. As a **consumer service**, I want to **deduplicate events using UUID eventId stored in a processed_events table**, so that **at-least-once RabbitMQ delivery doesn't cause double-processing**.
22. As an **operator**, I want to **query the processed_events table to debug event processing**, so that **I can trace why an event was or wasn't handled**.
23. As a **developer**, I want to **evolve event schemas without breaking consumers**, so that **services can deploy independently**.

## Implementation Decisions

### Modules to Build/Modify

| Module | Changes |
|--------|---------|
| `common` | Add `eventId` (UUID) to all event classes; add `ProcessedEvent` entity; add `ProductVariantDto`, `CategoryDto` with parentId; add `ShipmentDto`, `PaymentDto` |
| `product` | Add `ProductVariant` entity (skuCode, attributes Map, price, inventory linkage); add `parentId` + `@ManyToOne`/`@OneToMany` to Category; update ProductEvent to include variant data |
| `category` | Add `parentId` self-referencing FK; add children collection; update API for tree operations |
| `order-service` | Add `OrderItem` entity (status, quantityOrdered, quantityShipped, `@ManyToOne` Order); add `@OneToMany` items to Order; add `Shipment` entity (trackingNumber, carrier, shippedAt, `@ManyToOne` Order, `@OneToMany` ShipmentItem); update OrderEvent to include per-line status |
| `inventory-service` | Clarify `quantity` = on-hand; add reservation TTL scheduler (15 min); implement partial reservation with backorder; ensure LOW_STOCK triggers on `availableQuantity` |
| `notification-service` | Add `NotificationTemplate` entity; implement fixed retry (3×5min); add multi-channel fallback logic; add template variables for order/payment/shipment |
| `payment-service` (NEW) | Create new module: Payment entity (AUTHORIZED, CAPTURED, REFUNDED, FAILED); authorization/capture flow; idempotency key; refund workflow; publish PaymentEvent |
| `gateway` | Add routes for new payment-service endpoints |
| `system-test` | Add integration tests for: partial reservation + backorder; reservation expiry; multi-shipment; payment auth/capture; notification retry/fallback; event idempotency |

### Key Interfaces/Contracts

**OrderService API** (existing, enhanced):
- `POST /orders` — creates Order + OrderItems, publishes OrderEvent.CREATED
- `GET /orders/{id}` — returns Order with items + per-line status + shipments
- `POST /orders/{id}/shipments` — creates Shipment with ShipmentItems, updates OrderItem quantities, publishes OrderEvent.SHIPPED

**InventoryService API** (existing, enhanced):
- `reserveStock(productId, quantity)` — returns reserved + backordered quantities
- `confirmStock(productId, quantity)` — moves reserved → confirmed (reduces on-hand)
- `releaseReservation(productId, quantity)` — releases reservation
- Scheduler: `@Scheduled(fixedDelay=1min)` scans for reservations > 15 min old → release

**PaymentService API** (new):
- `POST /payments/authorize` — authorize amount for orderId, returns paymentId + clientToken
- `POST /payments/{id}/capture` — capture authorized payment
- `POST /payments/{id}/refund` — full/partial refund
- Webhook endpoint for gateway callbacks → publishes PaymentEvent

**Event Contracts** (common module):
- All events gain `eventId: UUID` field
- `OrderEvent` adds `items[i].status` (PENDING/RESERVED/SHIPPED/BACKORDERED/CANCELLED)
- `ProductEvent` adds `variantId` and `skuCode`
- `InventoryEvent` adds `eventId` for idempotency

### Architectural Decisions

1. **Aggregate Boundaries**: Order is aggregate root owning OrderItems (cascade persist/remove). Shipment is separate aggregate referencing Order.
2. **Inventory per Variant**: Inventory table keyed by `variantId` (not `productId`). ProductEvent carries variantId.
3. **Reservation as Implicit State**: No separate Reservation entity; `Inventory.reservedQuantity` is the source of truth. Scheduler reads `Inventory.updatedAt` to detect stale reservations.
4. **Idempotency per Consumer**: Each service has own `processed_events(event_id PK, processed_at)` table. Consumer checks table before processing; inserts after successful handling.
5. **Event Publishing**: Use transactional outbox pattern (DB table + relay) for reliability, or RabbitMQ publisher confirms with at-least-once + idempotency.

### Schema Changes

- `product` table: add `base_product_id` FK (self-referencing for variants) — or separate `product_variant` table
- `category` table: add `parent_id` FK self-referencing
- `orders` table: unchanged (Order entity)
- New `order_items` table: `id`, `order_id` FK, `variant_id`, `quantity_ordered`, `quantity_shipped`, `unit_price`, `status`
- New `shipments` table: `id`, `order_id` FK, `tracking_number`, `carrier`, `shipped_at`, `status`
- New `shipment_items` table: `id`, `shipment_id` FK, `order_item_id` FK, `quantity`
- New `payments` table: `id`, `order_id` FK, `amount`, `currency`, `status`, `gateway_transaction_id`, `idempotency_key`, `authorized_at`, `captured_at`
- New `processed_events` table (per service): `event_id` PK, `processed_at`
- New `notification_templates` table: `id`, `type`, `channel`, `subject_template`, `body_template`, `variables`

## Testing Decisions

### What Makes a Good Test
- Test **external behavior** (API responses, event publishing, state transitions) not implementation details
- Use **Testcontainers** for PostgreSQL, RabbitMQ — same as existing `system-test` module
- **Contract tests** for event schemas (Pact or JSON schema validation)
- **Scenario-based** integration tests covering happy path + edge cases

### Modules to Test

| Module | Test Focus |
|--------|------------|
| `product` | Variant CRUD, category tree, ProductEvent publishing with variant data |
| `category` | Tree operations (move, delete with children), cycle detection |
| `order-service` | Order+items creation, per-line status transitions, multi-shipment, OrderEvent content |
| `inventory-service` | Reserve/confirm/release, partial reservation + backorder, 15-min expiry scheduler, LOW_STOCK trigger |
| `payment-service` | Auth/capture flow, idempotency key enforcement, refund (full/partial), PaymentEvent publishing |
| `notification-service` | Retry logic (3×5min), template rendering, multi-channel fallback, dead letter after max retries |
| `common` | Event serialization, idempotency key generation, ProcessedEvent repository |
| `system-test` | End-to-end: order → reserve → pay → confirm → ship (partial) → deliver; notification flow; event deduplication |

### Prior Art
- Existing `*IntegrationTest.java` in each module using Testcontainers + PostgreSQL
- `system-test` module with Testcontainers for cross-service flows
- RabbitMQ test config in `common/src/test/resources`

## Out of Scope

- **Search/browse UI** — frontend not in this repo
- **Admin dashboard** — separate project
- **Advanced promotions/discounts** — coupon codes, loyalty, bundles
- **Multi-warehouse inventory** — single inventory per variant for now
- **Internationalization** — templates in English only
- **Event schema registry** — manual versioning for now
- **Distributed tracing** — OpenTelemetry setup exists but not required for this spec
- **Performance/load testing** — separate effort

## Further Notes

### Open Questions Requiring Future Decisions
1. **Reservation extension** — Can a pending payment extend the 15-min hold? (Requires PaymentEvent interaction)
2. **Event retention** — How long to keep `processed_events`? (Suggest 30 days + cleanup job)
3. **Schema evolution** — Avro/Protobuf migration or JSON with optional fields?
4. **Category move** — Moving a subtree: update all descendants' paths or recursive CTE?
5. **Variant deletion** — Soft delete only? What about existing OrderItems referencing deleted variants?

### Migration Path
1. Add `eventId` to all events in `common` — deploy first (backward compatible)
2. Add `processed_events` table + idempotency filter in each consumer
3. Implement ProductVariant + Category hierarchy in `product`/`category`
4. Add OrderItem + Shipment in `order-service`
5. Implement reservation TTL scheduler + partial reservation in `inventory-service`
6. Build `payment-service` module
7. Add notification templates + retry/fallback in `notification-service`
8. End-to-end system tests

### Dependencies Between Decisions
- ProductVariant must exist before Inventory can key by variantId
- OrderItem needs variantId (not productId) for accurate reservation
- PaymentService must exist before OrderService can transition PENDING→CONFIRMED on PaymentEvent.SUCCESS
- Shipment entity needs OrderItem to track per-line quantities

### Risks
- **Event ordering**: OrderEvent.CREATED may arrive before ProductEvent.CREATED for new variants — inventory-service must handle missing variant gracefully (create Inventory on first reservation attempt)
- **Partial reservation + payment**: If customer pays for 3 reserved + 2 backordered, payment amount must match reserved only — payment-service needs to know reserved vs backordered
- **Scheduler contention**: Multiple inventory-service instances running expiry job — use DB advisory lock or Quartz cluster

---

*Generated from grilling session using domain-modeling skill. See `CONTEXT.md` for full glossary and decision log.*