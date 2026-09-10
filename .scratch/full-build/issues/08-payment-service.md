# 08 — Payment Service

**What to build:** Payment service at `http://localhost:8085`. WebFlux + JPA + PostgreSQL. Mock payment gateway. Authorize/capture flow with idempotency keys.

**Blocked by:** 01-config-server, 02-eureka-server, 04-gateway

**Status:** **DONE**

- [x] Service starts on port 8085, registers with Eureka
- [x] PostgreSQL schema: `payment` (order_id, payment_id, status, authorized_amount, captured_amount, idempotency_key, created_at)
- [x] Endpoints: `POST /api/v1/payments/authorize`, `POST /api/v1/payments/{id}/capture`, `POST /api/v1/payments/{id}/refund`
- [x] Authorize: validates idempotency_key, stores AUTHORIZED payment with full order total
- [x] Capture: only captures up to reserved_total (passed from order service), marks CAPTURED
- [x] Refund: marks REFUNDED, supports partial refund
- [x] Idempotency: all operations require `Idempotency-Key` header; duplicate keys return same result
- [x] RabbitMQ: consumes `inventory.stock_reserved` → authorizes; publishes `payment.authorized` / `payment.failed`
- [x] Mock gateway: simulates success/failure based on amount or test header
- [x] OpenAPI docs + Actuator + Prometheus
- [x] Unit tests + Integration tests (Testcontainers PG + RabbitMQ)
- [x] Dockerfile + docker-compose entry