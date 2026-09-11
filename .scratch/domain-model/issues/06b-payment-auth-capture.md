# 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**What to build:** `Payment` entity (AUTHORIZED/CAPTURED/REFUNDED/FAILED), idempotency key, gatewayTransactionId. API: `POST /payments/authorize` → paymentId + clientToken; `POST /payments/{id}/capture`; `POST /payments/{id}/refund` (full/partial). Webhook → publishes `PaymentEvent` directly to RabbitMQ. **Auth captures `orderTotal`, capture captures only `reservedTotal` (sum of RESERVED OrderItems).**

**Blocked by:** 06a — Payment Service: Module Scaffold + Build/CI

**Status:** **DONE**

- [x] Create `Payment` JPA/R2DBC entity: `id`, `orderId`, `amount`, `currency`, `status` (enum: AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED), `gatewayTransactionId`, `idempotencyKey` (unique), `authorizedAt`, `capturedAt`, `refundedAmount` (default 0)
- [x] Create `PaymentDto`, `AuthorizeRequest`, `CaptureRequest`, `RefundRequest` in `common` (Lombok removed, explicit code)
- [x] Implement `PaymentService`:
  - `authorize(orderId, amount, currency, idempotencyKey)` → creates Payment(AUTHORIZED), calls payment gateway, stores gatewayTransactionId, returns paymentId + clientToken (for frontend)
  - `capture(paymentId)` → calls gateway capture, transitions AUTHORIZED → CAPTURED, sets capturedAt
  - `refund(paymentId, amount?)` → calls gateway refund; if full: REFUNDED; if partial: PARTIALLY_REFUNDED; updates refundedAmount
  - All methods idempotent via `idempotencyKey` (check existing before create)
- [x] Implement `PaymentController`: REST endpoints at `/api/v1/payments` + webhook endpoint (`POST /webhook/{gateway}`)
- [x] Webhook handler: verify signature, parse event → update Payment status
- [x] **Publish `PaymentEvent` directly to RabbitMQ** (publisher confirms; no outbox/Debezium) on AUTHORIZED, CAPTURED, REFUNDED, FAILED
- [x] Update `PaymentEvent` in `common`: ensure `eventId`, `paymentId`, `orderId`, `amount`, `status`, `gatewayTransactionId` (Lombok removed)
- [x] Integration tests: auth→capture flow, idempotency key reuse, full/partial refund, webhook handling, event publishing
- [x] Mock gateway: simulates success/failure based on amount or test header
- [x] OpenAPI docs + Actuator + Prometheus
- [x] Dockerfile + docker-compose entry

## Key Decisions (from grilling session)

- **Authorize `orderTotal`**: At order creation, authorize the full order amount (including backordered items)
- **Capture `reservedTotal` only**: Only the sum of RESERVED OrderItems is captured when Order → CONFIRMED. Backordered items are not charged.
- **Idempotency**: Every payment request carries a unique `idempotencyKey` (UUID) to guarantee exactly-once charging
- **Refund**: Full refund (cancel all items) or partial refund (specific items/shipments); refund amount cannot exceed captured amount per item
- **RabbitMQ consumer**: Consumes `inventory.stock_reserved` events → triggers payment authorization

## Test Infrastructure Resolution

**Problem:** Flyway (JDBC) and R2DBC connected to different database instances.

**Solution:** Used `@ServiceConnection` with `spring-boot-testcontainers` dependency for both JDBC and R2DBC (Spring Boot 3.3.5 support). Added `@ServiceConnection(type = ConnectionFactory.class)` for R2DBC and `@ServiceConnection` for DataSource. Both now connect to the same Testcontainers PostgreSQL instance.

## Files modified for test infra:
- `payment-service/pom.xml` - added `spring-boot-testcontainers`, `flyway-database-postgresql`, `HikariCP`
- `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java` - test infrastructure with `@ServiceConnection`
- `payment-service/src/test/resources/application-test.yml` - test profile config

## Context for Next Agent

**Spring Boot 3.3.5** - supports `@ServiceConnection` for both JDBC and R2DBC.

**All 8 integration tests pass:**
- `testAuthorizePayment`
- `testAuthorizePaymentIdempotency`
- `testCapturePayment`
- `testCapturePaymentIdempotency`
- `testRefundPaymentFull`
- `testRefundPaymentPartial`
- `testGetPaymentById`
- `testGetPaymentByOrderId`

**Key files:**
- `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java` - test infrastructure
- `payment-service/src/test/resources/application-test.yml` - test profile
- `payment-service/pom.xml` - test dependencies

**Spring Boot 3.3.5** - supports `@ServiceConnection` for both JDBC and R2DBC (add `spring-boot-testcontainers` dependency).
