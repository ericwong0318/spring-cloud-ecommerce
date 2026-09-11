# Context — Payment Service Ticket 06b Implementation

## Project State (as of 2026-09-08)

### Ticket 06b Status: **Code Complete, Tests Blocked**

**Spring Boot 3.3.5** | **Spring Cloud 2023.0.4** | **Java 21**

---

## ✅ Completed (Code Implementation)

### Payment Domain
- **`Payment` entity** (R2DBC): `id`, `orderId`, `amount`, `currency`, `status` (AUTHORIZED, CAPTURED, REFUNDED, FAILED, PARTIALLY_REFUNDED), `gatewayTransactionId`, `idempotencyKey` (unique), `authorizedAt`, `capturedAt`, `refundedAt`, `refundedAmount`
- **DTOs in `common`**: `PaymentDto`, `AuthorizeRequest`, `CaptureRequest`, `RefundRequest`

### PaymentService
- `authorize(orderId, amount, currency, idempotencyKey)` → creates Payment(AUTHORIZED), returns paymentId
- `capture(paymentId, gatewayTransactionId)` → AUTHORIZED → CAPTURED
- `refund(paymentId, amount)` → full → REFUNDED, partial → PARTIALLY_REFUNDED
- All methods idempotent via `idempotencyKey`

### PaymentController
- `POST /api/payments/authorize` → 201 + PaymentDto
- `POST /api/payments/{id}/capture` → 200 + PaymentDto
- `POST /api/payments/{id}/refund` → 200 + PaymentDto
- `GET /api/payments/{id}` / `/order/{orderId}` / `/order/{orderId}/all`
- `POST /api/payments/webhook/{gateway}` - signature verification + status update

### Event Publishing
- **RabbitMQ publisher confirms** (no outbox/Debezium)
- Events: AUTHORIZED, CAPTURED, REFUNDED, FAILED
- Published on: authorize → AUTHORIZED, capture → CAPTURED, refund → REFUNDED/PARTIALLY_REFUNDED, failure → FAILED

### Database Migrations
- `V1__create_payments_table.sql` - payments table + indexes
- `V2__create_outbox_table.sql` - processed_events table
- Flyway 11.7.0 + `flyway-database-postgresql` (PostgreSQL 16 support)

---

## ❌ Blocked: Integration Test Infrastructure

### Problem: "Two Databases" Conflict
```
Flyway (JDBC)  → Testcontainers DB (dynamic port)  ✅ Tables created
R2DBC (App)    → localhost:5432 (hardcoded)       ❌ Empty DB → "relation 'payments' does not exist"
```

**Root Cause:** Flyway runs during Spring context initialization (before `@DynamicPropertySource` applies). R2DBC reads hardcoded `localhost:5432` from `application.yml`.

---

## 📁 Key Files (Test Infrastructure WIP)

| File | Status |
|------|--------|
| `payment-service/pom.xml` | ✅ Deps added: `spring-boot-testcontainers`, `flyway-database-postgresql:11.7.0`, `HikariCP` |
| `BaseIntegrationTest.java` | 🔴 **WIP** - `@ServiceConnection` failed (missing `R2DBCDatabaseContainer` class) |
| `application-test.yml` | Clean config, relies on dynamic props |
| `TestDatabaseConfig.java` | Deleted (replaced by `@ServiceConnection` approach) |

### Current `BaseIntegrationTest.java` (simplified @ServiceConnection attempt)
```java
@Container @ServiceConnection
static final PostgreSQLContainer<?> postgres = ...;

@DynamicPropertySource
static void configureRabbitMQ(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> 
        "r2dbc:postgresql://%s:%d/%s".formatted(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
    // ... rabbitmq + flyway + eureka props
}
```
**Error:** `NoClassDefFoundError: org/testcontainers/r2dbc/R2DBCDatabaseContainer` - Spring Boot's `@ServiceConnection` for R2DBC requires `org.testcontainers:r2dbc` module which doesn't exist in Testcontainers 1.21.x.

---

## 🔧 Next Steps to Unblock

### Option 1: Fallback to Manual DynamicPropertySource (Recommended)
Replace `@ServiceConnection` with explicit `@DynamicPropertySource` wiring **both** JDBC and R2DBC to the same container:

```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    // 1. JDBC (Flyway)
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    
    // 2. R2DBC (App) - same container, dynamic port
    registry.add("spring.r2dbc.url", () -> 
        "r2dbc:postgresql://%s:%d/%s".formatted(postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
    // ... rabbitmq, flyway, eureka
}
```

### Critical: Disable Flyway Auto-Config
In `application-test.yml`: `spring.flyway.enabled: false` → run Flyway manually in `ContextRefreshedEvent` listener after DataSource is created.

### Dependencies to Keep
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Commands to Resume

```bash
# 1. Fix BaseIntegrationTest.java (manual DynamicPropertySource + ContextRefreshedEvent for Flyway)
# 2. Run single test
mvn test -pl payment-service -Dtest=PaymentServiceIntegrationTest#testAuthorizePayment

# 2. Full test suite
mvn test -pl payment-service

# 3. Full build
mvn clean verify -pl payment-service
mvn clean install -DskipTests  # full build
```

---

## Key Decisions Recorded

| Decision | Detail |
|----------|--------|
| Authorize amount | `orderTotal` (full order including backorders) |
| Capture amount | `reservedTotal` only (sum of RESERVED OrderItems) |
| Idempotency | UUID per request, checked before create |
| Refund | Full → REFUNDED, partial → PARTIALLY_REFUNDED; max refund = captured amount |
| Event publishing | Direct RabbitMQ with publisher confirms |
| Flyway version | 11.7.0 + flyway-database-postgresql |

---

## Next Agent: Start Here

1. Open `payment-service/src/test/java/com/example/payment/BaseIntegrationTest.java`
2. Replace `@ServiceConnection` with manual `@DynamicPropertySource` wiring both JDBC and R2DBC to same container
3. Add `ContextRefreshedEvent` listener to run Flyway manually after DataSource created
4. Run: `mvn test -pl payment-service -Dtest=PaymentServiceIntegrationTest#testAuthorizePayment`
3. If passes → run full suite → full build → code-review → commit

**DO NOT** modify production code (entities, services, controllers) - they are complete and correct.
