# 04b — Order Management: Shipment Aggregate

**What to build:** `Shipment` entity (trackingNumber, carrier, shippedAt, status) + `ShipmentItem` linking to OrderItem. `POST /orders/{id}/shipments` creates shipment, increments `quantityShipped`, publishes `OrderEvent.SHIPPED`.

**Blocked by:** 04a — Order Management: OrderItem Aggregate (Core)

**Status:** ready-for-agent

- [ ] Create `Shipment` JPA entity: `id`, `orderId` (FK to Order), `trackingNumber`, `carrier`, `shippedAt`, `status` (CREATED, IN_TRANSIT, DELIVERED, EXCEPTION)
- [ ] Create `ShipmentItem` JPA entity: `id`, `shipmentId` (FK to Shipment), `orderItemId` (FK to OrderItem), `quantity`
- [ ] Add `@OneToMany` shipments to `Order` entity; `@OneToMany` shipmentItems to `Shipment`
- [ ] Create `ShipmentDto`, `ShipmentItemDto` in `common`
- [ ] Implement `ShipmentService`, `ShipmentController` in `order-service`
- [ ] API: `POST /orders/{id}/shipments` — body: `{trackingNumber, carrier, items: [{orderItemId, quantity}]}`
  - Validates: order exists, items belong to order, quantity ≤ (quantityOrdered - quantityShipped)
  - Creates Shipment + ShipmentItems, increments `OrderItem.quantityShipped`, updates `OrderItem.status` → SHIPPED if fully shipped
  - Publishes `OrderEvent.SHIPPED` via outbox with shipment details
- [ ] API: `GET /orders/{id}/shipments` — list shipments for order
- [ ] Update `OrderEvent` in `common`: add `shipmentId`, `trackingNumber`, `carrier`, `shippedAt`
- [ ] Integration tests: create shipment, verify quantityShipped increment, event published, partial shipment flow