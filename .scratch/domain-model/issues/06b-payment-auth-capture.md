# 06b — Payment Service: Auth/Capture + Refunds + PaymentEvent

**What to build:** `Payment` entity (AUTHORIZED/CAPTURED/REFUNDED/FAILED), idempotency key, gatewayTransactionId. API: `POST /payments/authorize` → paymentId + clientToken; `POST /payments/{id}/capture`; `POST /payments/{id}/refund` (full/partial). Webhook → publishes `PaymentEvent` directly to RabbitMQ. **Auth captures `orderTotal`, capture captures only `reservedTotal` (sum of RESERVED OrderItems).**

**Blocked by:** 06a — Payment Service: Module Scaffold + Build/CI

**Status:** **SKIPPED** (test infra blocked)

- [x] Create `Payment` JPA/R2DBC entity: `id`, `orderId`, `amount`, `currency`, `status` (enum: AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED), `gatewayTransactionId`, `idempotencyKey` (unique), `authorizedAt`, `capturedAt`, `refundedAmount` (default 0)
- [x] Create `PaymentDto`, `AuthorizeRequest`, `CaptureRequest`, `RefundRequest` in `common`
- [x] Implement `PaymentService`:
  - `authorize(orderId, amount, currency, idempotencyKey)` → creates Payment(AUTHORIZED), calls payment gateway, stores gatewayTransactionId, returns paymentId + clientToken (for frontend)
  - `capture(paymentId)` → calls gateway capture, transitions AUTHORIZED → CAPTURED, sets capturedAt
  - `refund(paymentId, amount?)` → calls gateway refund; if full: REFUNDED; if partial: PARTIALLY_REFUNDED; updates refundedAmount
  - All methods idempotent via `idempotencyKey` (check existing before create)
- [x] Implement `PaymentController`: REST endpoints + webhook endpoint (`POST /webhook/{gateway}`)
- [x] Webhook handler: verify signature, parse event → update Payment status
- [x] **Publish `PaymentEvent` directly to RabbitMQ** (publisher confirms; no outbox/Debezium) on AUTHORIZED, CAPTURED, REFUNDED, FAILED
- [x] Update `PaymentEvent` in `common`: ensure `eventId`, `paymentId`, `orderId`, `amount`, `status`, `gatewayTransactionId`
- [ ] Integration tests: auth→capture flow, idempotency key reuse, full/partial refund, webhook handling, event publishing

## Key Decisions (from grilling session)

- **Authorize `orderTotal`**: At order creation, authorize the full order amount (including backordered items)
- **Capture `reservedTotal` only**: Only the sum of RESERVED OrderItems is captured when Order → CONFIRMED. Backordered items are not charged.
- **Idempotency**: Every payment request carries a unique `idempotencyKey` (UUID) to guarantee exactly-once charging
- **Refund**: Full refund (cancel all items) or partial refund (specific items/shipments); refund amount cannot exceed captured amount per item

## Current Blocker: Integration Test Infrastructure

**Problem:** Flyway (JDBC) and R2DBC connect to **different database instances** because:
- Flyway runs during Spring context initialization (before `@DynamicPropertySource` takes effect)
- R2DBC reads hardcoded `localhost:5432` from `application.yml`
- Result: Flyway migrates Testcontainers DB, R2DBC hits empty local DB → `relation "payments" does not exist`

**Attempted fixes (all failed):**
1. `@DynamicPropertySource` in `BaseIntegrationTest` - applied too late for Flyway
2. `@ServiceConnection` - fails: `NoClassDefFoundError: org/testcontainers/r2dbc/R2DBCDatabaseContainer` (missing R2DBC Testcontainers support)
3. Custom `ApplicationContextInitializer` - Flyway still runs before properties apply

**Next attempt needed:** Use `@ServiceConnection` with `spring-boot-testcontainers` + `@ServiceConnection(type = ConnectionFactory.class)` for R2DBC (Spring Boot 3.3.5 supports this). If fails, fallback to custom `ApplicationContextInitializer` registered via `@ContextConfiguration(initializers = ...)`.

**Files modified for test infra:**
- `payment-service/pom.xml` - added `spring-boot-testcontainers`, `flyway-database-postgresql`, `HikariCP`
- `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java` - current WIP
- `payment-service/src/test/resources/application-test.yml` - test profile config
- `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java` - WIP (currently using `@DynamicPropertySource` approach)

## Context for Next Agent

**Spring Boot 3.3.5** - supports `@ServiceConnection` for both JDBC and R2DBC.

**Remaining work to pass tests:**
1. Fix Testcontainers + Flyway + R2DBC wiring so both connect to same Testcontainers instance
2. Run `mvn test -pl payment-service` - all 8 integration tests should pass
3. Run `mvn clean verify` (integration tests) 
4. Run `mvn clean install -DskipTests` (full build)
5. `/code-review` and commit

**Key files:**
- `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java` - test infrastructure
- `payment-service/src/test/resources/application-test.yml` - test profile
- `payment-service/pom.xml` - test dependencies

**Spring Boot 3.3.5** - supports `@ServiceConnection` for both JDBC and R2DBC (add `spring-boot-testcontainers` dependency).
