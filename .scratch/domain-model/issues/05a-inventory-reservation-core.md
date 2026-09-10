# 05a — Inventory: Reservation Core (Reserve/Confirm/Release)

**What to build:** `Inventory.quantity` = on-hand (semantics clarified). `reserveStock(variantId, qty)` → returns `{reserved, backordered}`; partial reserve + backorder (OrderItem status = BACKORDERED). `confirmStock` moves reserved→confirmed (reduces on-hand). `releaseReservation` releases hold. **All events published directly to RabbitMQ** (no outbox/Debezium).

**Blocked by:** 03a — Product Catalog: ProductVariant Entity + API

**Status:** done

- [x] Update `Inventory` entity: ensure `productId` → `variantId` (FK to ProductVariant); clarify `quantity` = on-hand physical stock in JavaDoc/comments
- [x] Implement `InventoryService.reserveStock(variantId, qty)`:
  - Returns `ReservationResult {reserved, backordered}`
  - If available ≥ qty: reserve full, backordered = 0
  - If available < qty: reserve available, backordered = qty - available
  - Updates `reservedQuantity`; sets OrderItem.status = RESERVED, `reservedAt = now`
  - **Publishes `InventoryEvent.RESERVED` directly to RabbitMQ** (publisher confirms)
- [x] Implement `InventoryService.confirmStock(variantId, qty)`:
  - Reduces `quantity` by qty, reduces `reservedQuantity` by qty
  - **Publishes `InventoryEvent.CONFIRMED` directly to RabbitMQ**
- [x] Implement `InventoryService.releaseReservation(variantId, qty)`:
  - Reduces `reservedQuantity` by qty (min 0)
  - Sets matching OrderItem.status = CANCELLED (handled by order-service via ReservationExpiredEvent)
  - **Publishes `ReservationExpiredEvent` + `InventoryEvent.RELEASED` directly to RabbitMQ**
- [x] Update `InventoryEvent` in `common`: add `variantId`, `reserved`, `backordered` fields; add `ReservationExpiredEvent` with `{eventId, orderItemId, variantId, quantityReleased}`
- [x] Update `InventoryEventListener` to consume `OrderEvent.CREATED` → call `reserveStock` per line; on partial reserve, publish backorder info
- [x] Integration tests: full reserve, partial reserve + backorder, confirm, release; verify events via RabbitMQ consumer
