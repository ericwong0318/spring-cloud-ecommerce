# 09b — System Tests: Error & Edge Cases

**What to build:** (1) Partial reservation + backorder flow; (2) Reservation expiry (15 min) auto-release; (3) Payment failure → order cancel → inventory release; (4) Payment partial capture → partial confirm; (5) Notification retry/fallback (email→SMS); (6) Category move subtree; (7) Variant soft-delete with existing OrderItems.

**Blocked by:** 09a — System Tests: Infrastructure + Happy Path

**Status:** ready-for-agent

- [x] Test: **Partial Reservation + Backorder**
  - Create Inventory with quantity=3
  - Place Order for 5 units of same variant
  - Verify `reserveStock` returns `{reserved: 3, backordered: 2}`
  - Verify OrderItem status = BACKORDERED for 2 units
  - Add stock (2 more) → verify backorder can be fulfilled
- [x] Test: **Reservation Expiry (15 min)**
  - Place Order → reservation created
  - Do NOT complete payment
  - Wait 16 min (or manipulate `Inventory.updatedAt` in test)
  - Verify scheduler releases reservation → `InventoryEvent.RELEASED`
  - Verify Order still PENDING but inventory available again
- [x] Test: **Payment Failure → Order Cancel → Inventory Release**
  - Place Order → reservation created
  - Payment fails (webhook returns FAILED)
  - Verify `PaymentEvent.FAILED` → Order CANCELLED
  - Verify `OrderEvent.CANCELLED` → inventory `releaseReservation` called
  - Verify inventory availableQuantity restored
- [x] Test: **Partial Capture → Partial Confirm**
  - Order with 2 lines (variant A qty=2, variant B qty=2)
  - Payment captures only for variant A amount
  - Verify Order CONFIRMED but only variant A items → RESERVED
  - Variant B items remain PENDING (or BACKORDERED)
- [x] Test: **Notification Retry + Fallback**
  - Configure email provider to fail
  - Trigger order confirmation notification
  - Verify 3 retries at 5-min intervals (status=RETRYING)
  - After 3 failures → status=FAILED, SMS notification auto-created
  - Verify SMS sent successfully
- [x] Test: **Category Move Subtree**
  - Create category tree: Electronics → Computers → Laptops
  - Move Computers under Accessories
  - Verify all descendants (Laptops) moved correctly
  - Verify no cycles created
- [x] Test: **Variant Soft-Delete with Existing OrderItems**
  - Create Product + Variant, place Order referencing variant
  - Soft-delete variant (status=DELETED, not hard delete)
  - Verify Order still readable, variant info preserved in OrderItem
  - Verify new orders cannot use deleted variant
