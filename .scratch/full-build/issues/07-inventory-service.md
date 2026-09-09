# 07 — Inventory Service

**What to build:** Inventory service at `http://localhost:8084`. WebFlux + R2DBC + PostgreSQL. Manages stock, reservations (15-min TTL), publishes events to RabbitMQ.

**Blocked by:** 01-config-server, 02-eureka-server, 04-gateway

**Status:** ready-for-agent

- [ ] Service starts on port 8084, registers with Eureka
- [ ] PostgreSQL schema: `inventory` (product_id, location, available, reserved), `reservation` (order_id, product_id, quantity, expires_at, status)
- [ ] Endpoints: `POST /api/v1/reservations`, `DELETE /api/v1/reservations/{id}`, `GET /api/v1/inventory/{productId}`
- [ ] Reserve stock: decrements available, increments reserved, creates reservation with 15-min TTL
- [ ] Release reservation: increments available, decrements reserved, marks reservation RELEASED
- [ ] Scheduler: every minute, find expired reservations → release → publish `ReservationExpiredEvent`
- [ ] RabbitMQ: publishes `inventory.stock_reserved`, `inventory.stock_released`, `inventory.stock_reservation_failed` to `ecommerce.events` exchange
- [ ] Consumes `order.created` → attempts reservation
- [ ] OpenAPI docs + Actuator + Prometheus
- [ ] Unit tests + Integration tests (Testcontainers PG + RabbitMQ)
- [ ] Dockerfile + docker-compose entry