# 04a — Order Management: OrderItem Aggregate (Core)

**What to build:** `OrderItem` entity owned by Order (cascade persist/remove) with per-line status (PENDING/RESERVED/SHIPPED/BACKORDERED/CANCELLED), `quantityOrdered`/`quantityShipped`, `variantId`. `POST /orders` creates Order+Items → publishes `OrderEvent.CREATED` via Debezium. `GET /orders/{id}` returns items+status.

**Blocked by:** 03a — Product Catalog: ProductVariant Entity + API

**Status:** ready-for-agent

- [ ] Create `OrderItem` JPA entity: `id`, `orderId` (FK to Order), `variantId` (FK to ProductVariant), `quantityOrdered`, `quantityShipped` (default 0), `unitPrice` (snapshot), `status` (enum: PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED)
- [ ] Add `@OneToMany` items to `Order` entity (cascade ALL, orphanRemoval=true)
- [ ] Update `OrderDto` and `OrderItemDto` in `common` with `variantId`, `status`, `quantityShipped`
- [ ] Implement `OrderItemRepository`, update `OrderService`/`OrderMapper` to handle items
- [ ] API: `POST /orders` — creates Order + OrderItems in single transaction; publishes `OrderEvent.CREATED` (with full lines, per-line status = PENDING) via outbox/Debezium
- [ ] API: `GET /orders/{id}` — returns Order with items (status, quantityOrdered, quantityShipped)
- [ ] Update `OrderEvent` in `common`: add `items[i].variantId`, `items[i].status` fields
- [ ] Integration tests: create order with multiple items, verify event published with variantId + status