# 04a — Order Management: OrderItem Aggregate (Core)

**What to build:** `OrderItem` entity owned by Order (cascade persist/remove) with per-line status (PENDING/RESERVED/SHIPPED/BACKORDERED/CANCELLED), `quantityOrdered`/`quantityShipped`, `variantId`, `reservedAt` timestamp. `POST /orders` creates Order+Items → publishes `OrderEvent.CREATED` via RabbitMQ. `GET /orders/{id}` returns items+status.

**Blocked by:** 03a — Product Catalog: ProductVariant Entity + API

**Status:** **done**

- [x] Create `OrderItem` JPA entity: `id`, `orderId` (FK to Order), `variantId` (FK to ProductVariant), `quantityOrdered`, `quantityShipped` (default 0), `unitPrice` (snapshot), `status` (enum: PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED), `reservedAt` (timestamp, nullable)
- [x] Add `@OneToMany` items to `Order` entity (cascade ALL, orphanRemoval=true)
- [x] Update `OrderDto` and `OrderItemDto` in `common` with `variantId`, `status`, `quantityShipped`, `reservedAt`
- [x] Implement `OrderItemRepository`, update `OrderService`/`OrderMapper` to handle items
- [x] API: `POST /orders` — creates Order + OrderItems in single transaction; publishes `OrderEvent.CREATED` (with full lines, per-line status = PENDING) via **RabbitMQ direct publishing** (publisher confirms)
- [x] API: `GET /orders/{id}` — returns Order with items (status, quantityOrdered, quantityShipped, reservedAt)
- [x] Update `OrderEvent` in `common`: add `items[i].variantId`, `items[i].status`, `items[i].reservedAt` fields
- [x] Integration tests: create order with multiple items, verify event published with variantId + status; verify `reservedAt` set correctly
