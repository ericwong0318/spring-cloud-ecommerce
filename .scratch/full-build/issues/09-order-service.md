# 09 — Order Service

**What to build:** Order service at `http://localhost:8083`. WebFlux + R2DBC + PostgreSQL. Saga orchestrator for order lifecycle.

**Blocked by:** 01-config-server, 02-eureka-server, 04-gateway, 07-inventory-service, 08-payment-service

**Status:** **DONE**

- [x] Service starts on port 8083, registers with Eureka
- [x] PostgreSQL schema: `order`, `order_item` (status: PENDING/RESERVED/SHIPPED/BACKORDERED/CANCELLED, reserved_at)
- [x] Endpoints: `POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`
- [x] Create order: PENDING state, publishes `order.created` to RabbitMQ
- [x] Consumes `inventory.stock_reserved` → transitions items to RESERVED, order to RESERVED
- [x] Consumes `inventory.stock_reservation_failed` → CANCEL order, release any partial reservations
- [x] Consumes `payment.authorized` → transitions order to PAID (captured_amount = reserved_total)
- [x] Consumes `payment.failed` → CANCEL order, release reservations
- [x] Consumes `ReservationExpiredEvent` → CANCEL order, release reservations
- [x] RabbitMQ: publishes `order.created`, `order.cancelled`, `order.confirmed` to `ecommerce.events`
- [x] Partial fulfillment: supports backorder items (status BACKORDERED)
- [x] OpenAPI docs + Actuator + Prometheus
- [x] Unit tests + Integration tests (Testcontainers PG + RabbitMQ, full saga)
- [x] Dockerfile + docker-compose entry

## Scope Creep (Explicitly NOT in Scope)

The following are **explicitly excluded** from this ticket and should be implemented in separate tickets:

- Shipment/ShipmentItem domain models, repositories, mappers
- Shipment endpoints: `POST /api/v1/orders/{orderId}/shipment`, `GET /api/v1/orders/{orderId}/shipments`
- `OrderEvent.SHIPPED` and `OrderEvent.DELIVERED` publishing
- Endpoints: `GET /api/v1/orders`, `GET /api/v1/orders?customerId=...`, `PUT /api/v1/orders/{id}`, `DELETE /api/v1/orders/{id}`
- `OrderEvent.SHIPPED`/`DELIVERED` status transitions

These are documented here to prevent scope creep and ensure clear separation of concerns.