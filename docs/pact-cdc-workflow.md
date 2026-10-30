# Pact Consumer-Driven Contract (CDC) Workflow

This document describes how to run the full Consumer-Driven Contract testing cycle locally using Pact. The workflow enables developers to verify service contracts before deployment.

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Required for Spring Boot 3.3 |
| Maven | 3.9+ | Use wrapper `./mvnw` if available |
| Docker / OrbStack | Latest | Required for Testcontainers (integration tests) and Pact Broker (optional) |
| Pact Broker | Optional | Local instance at `http://localhost:9292` or CI-provided |

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CDC PIPELINE                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. CONSUMER TESTS          2. PACT GENERATION       3. PROVIDER │
│  ┌──────────────────┐      ┌──────────────────┐      ┌────────┐  │
│  │ order-service    │─────▶│ target/pacts/    │─────▶│ product│  │
│  │ product-service  │      │ *.json contracts │      │payment │  │
│  │ (calls others)   │      │                  │      │inventory│  │
│  └──────────────────┘      └──────────────────┘      └────────┘  │
│         │                                                │        │
│         ▼                                                ▼        │
│  4. (OPTIONAL) PACT BROKER ──────────────────────────────────▶   │
│     Publish pacts → Provider verifies against broker            │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Contract Directions

| Consumer | Provider | Test Location | Verifies |
|----------|----------|---------------|----------|
| order-service | product-service | `order-service/OrderProductConsumerPactTest.java` | Product CRUD APIs |
| order-service | payment-service | `order-service/OrderPaymentConsumerPactTest.java` | Payment authorize/capture/refund |
| order-service | inventory-service | `order-service/OrderInventoryConsumerPactTest.java` | Inventory reservation |
| product-service | category-service | `product/ProductCategoryConsumerPactTest.java` | Category lookup |
| notification-service | order-service | `notification-service/OrderServiceConsumerPactTest.java` | Order create/get/cancel |

All consumer contracts are implemented. Pacts are generated in each consumer module's `target/pacts/`.

---

## Full Local Cycle Commands

### Step 1: Install Common Module (Fixes DTO Compilation)

The `common` module contains shared DTOs used by multiple services. Lombok/MapStruct annotation processors must be applied correctly.

```bash
# From project root
mvn clean install -pl common -DskipTests
```

**Why this is needed:** The parent POM configures annotation processors for Lombok and MapStruct. Without installing `common` first, dependent services (order-service, payment-service, etc.) will fail to compile DTOs with errors like "cannot find symbol builder()" or missing getters.

---

### Step 2: Generate Consumer Pacts

Run consumer tests to generate Pact JSON files in each module's `target/pacts/` directory.

```bash
# Order service → Product, Payment, Inventory
mvn test -pl order-service -Dtest=*ConsumerPactTest

# Product service → Category
mvn test -pl product -Dtest=ProductCategoryConsumerPactTest

# Notification service → Order
mvn test -pl notification-service -Dtest=OrderServiceConsumerPactTest
```

**Expected output:** Each test creates a `.json` file in `target/pacts/`:
```
order-service/target/pacts/
├── order-service-product-service.json
├── order-service-payment-service.json
└── order-service-inventory-service.json
```

> **Tip:** Run with `-X` for debug output if pacts aren't generated.

---

### Step 3: Verify Providers Against Local Pacts

Run provider verification tests. Each provider test reads pacts from the consumer's `target/pacts/` directory using `@PactFolder`.

> **Current Status**: All 5 provider tests use the modern `@PactFolder` annotation and pass end-to-end:
> - Product (verifies order-service → product pacts)
> - Order (verifies notification-service → order pacts)
> - Payment (verifies order-service → payment pacts)
> - Inventory (verifies order-service → inventory pacts, requires Testcontainers/Docker)
> - Category (verifies product-service → category pacts)

```bash
# Product service provider (verifies order-service → product pacts)
mvn test -pl product -Dtest=ProductPactProviderTest

# Order service provider (verifies notification-service → order pacts)
mvn test -pl order-service -Dtest=OrderPactProviderTest

# Category service provider (verifies product-service → category pacts)
mvn test -pl category -Dtest=CategoryPactProviderTest

# Payment service provider (verifies order-service → payment pacts)
mvn test -pl payment-service -Dtest=PaymentPactProviderTest

# Inventory service provider (verifies order-service → inventory pacts, needs Docker/Testcontainers)
mvn test -pl inventory-service -Dtest=InventoryPactProviderTest
```

**How it works:** Provider tests use `@PactFolder` annotation pointing to the consumer's pact directory:
```java
@Provider("product-service")
@PactFolder("../order-service/target/pacts")  // Reads from consumer's output
class ProductPactProviderTest { ... }
```

No provider tests use the deprecated `ProviderInfo` API — all have been migrated to `@PactFolder`.

---

### Step 4: (Optional) Pact Broker Alternative

For team/CI workflows, use a Pact Broker instead of local file sharing.

#### Start Local Pact Broker
```bash
# Using Docker
docker run -d --name pact-broker \
  -p 9292:9292 \
  -e PACT_BROKER_DATABASE_URL=sqlite:///data/pactbroker.db \
  pactfoundation/pact-broker
```

#### Publish Pacts to Broker
```bash
# From project root (requires pact-jvm-provider-maven plugin)
mvn pact:publish \
  -Dpact.broker.url=http://localhost:9292 \
  -Dpact.consumer.version=$(git rev-parse --short HEAD)
```

#### Verify Providers Against Broker
```bash
# Provider tests use @PactBroker annotation
mvn test -pl product -Dtest=ProductPactProviderTest
# Reads from: @PactBroker(url = "http://localhost:9292")
```

---

## Troubleshooting

### "No pacts found" Error

**Symptom:** Provider test fails with `No pacts found for provider 'X'`

**Causes & Fixes:**
| Cause | Fix |
|-------|-----|
| Consumer tests didn't run | Run Step 2 first: `mvn test -pl order-service -Dtest=*ConsumerPactTest` |
| Wrong `@PactFolder` path | Check path is relative to provider module: `../order-service/target/pacts` |
| Pact files in wrong location | Verify `target/pacts/*.json` exists in consumer module |
| Consumer test disabled | Check `@Disabled` annotations on `@Pact` methods |

---

### "Provider State Mismatch" Error

**Symptom:** Verification fails on state setup: `Provider state 'X' not found`

**Causes & Fixes:**
| Cause | Fix |
|-------|-----|
| `@State` method missing | Add `@State("state name")` method in provider test |
| State name typo | Match exact string from consumer's `.given("state name")` |
| State method not static | Make `@State` methods `static void` |
| Database not initialized | Ensure `@State` sets up test data (use Testcontainers or in-memory) |

---

### Compilation Errors (Lombok/MapStruct)

**Symptom:** `cannot find symbol: method builder()` or missing getters/setters

**Fixes:**
```bash
# 1. Ensure common is installed first
mvn clean install -pl common -DskipTests

# 2. Clean and rebuild consumer module
mvn clean test -pl order-service -Dtest=*ConsumerPactTest

# 3. Check annotation processor config in parent POM
# maven-compiler-plugin → annotationProcessorPaths includes:
#   - org.mapstruct:mapstruct-processor
#   - org.projectlombok:lombok
```

---

### Pact Version Mismatch

**Symptom:** `Unsupported pact specification version`

**Fix:** All modules must use the same Pact version (defined in parent POM `dependencyManagement`):
```xml
<pact.version>4.7.5</pact.version>
```

---

### Testcontainers / Docker Issues

**Symptom:** `Could not find a valid Docker environment`

**Fixes:**
```bash
# Verify Docker/OrbStack is running
docker ps

# OrbStack: Settings → General → "Expose Docker socket"

# Disable Ryuk (cleanup container) if needed
export TESTCONTAINERS_RYUK_DISABLED=true
```

---

## Verification Checklist

Follow this checklist to verify the full Pact CDC cycle (all verified passing):

- [x] `mvn clean install -pl common -DskipTests` — **PASS** (installs shared DTOs)
- [x] `mvn test -pl order-service -Dtest=*ConsumerPactTest` — **PASS** (generates product, payment, inventory pacts)
- [x] `mvn test -pl product -Dtest=ProductCategoryConsumerPactTest` — **PASS** (generates category pact)
- [x] `mvn test -pl notification-service -Dtest=OrderServiceConsumerPactTest` — **PASS** (generates order pacts for notification consumer)
- [x] `mvn test -pl product -Dtest=ProductPactProviderTest` — **PASS** (verifies order-service → product)
- [x] `mvn test -pl order-service -Dtest=OrderPactProviderTest` — **PASS** (verifies notification-service → order)
- [x] `mvn test -pl category -Dtest=CategoryPactProviderTest` — **PASS** (verifies product-service → category)
- [x] `mvn test -pl payment-service -Dtest=PaymentPactProviderTest` — **PASS** (verifies order-service → payment)
- [x] `mvn test -pl inventory-service -Dtest=InventoryPactProviderTest` — **PASS** (verifies order-service → inventory; requires Docker/Testcontainers)

> **Note**: The full Pact CDC cycle passes for all 5 consumer-provider pairs. All provider tests use `@PactFolder` (no deprecated `ProviderInfo` API remains).

---

## CI/CD Integration

### GitHub Actions Pipeline (Reference)

```yaml
# .github/workflows/pact-cdc.yml
jobs:
  consumer-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install common
        run: mvn clean install -pl common -DskipTests
      - name: Generate pacts
        run: mvn test -pl order-service -Dtest="*ConsumerPactTest"
      - name: Publish to broker
        if: env.PACT_BROKER_URL != ''
        run: mvn pact:publish -Dpact.broker.url=$PACT_BROKER_URL

  provider-tests:
    needs: consumer-tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Verify all providers (@PactFolder)
        run: |
          mvn test -B -pl order-service,payment-service,category,inventory-service,product \
            -Dtest='*PactProviderTest' -DfailIfNoTests=false
```

### Required Secrets

| Secret | Description |
|--------|-------------|
| `PACT_BROKER_URL` | Pact Broker base URL (e.g., `https://pact-broker.company.com`) |
| `PACT_BROKER_TOKEN` | Bearer token for authentication |

---

## Ticket Dependency Diagram

From the implementation spec:

```
┌─────────────────────────┐ ◀── Complete
│ 1. Common module        │    (shared DTOs installed)
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐ ◀── Complete
│ 2. Consumer Pact Tests  │    (generates target/pacts/*.json)
│    - order-service      │    → product, payment, inventory
│    - product-service    │    → category
│    - notification-service│  → order
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐ ◀── Complete
│ 3. Provider Pact Tests  │    (verify via @PactFolder)
│    - product-service    │    ← order-service pacts
│    - order-service      │    ← notification-service pacts
│    - category-service   │    ← product-service pacts
│    - payment-service    │    ← order-service pacts
│    - inventory-service  │    ← order-service pacts
└──────────┬──────────────┘
           │
           ▼
┌─────────────────────────┐ ◀── Complete (THIS DOCUMENT)
│ 4. Document Workflow    │
└─────────────────────────┘
```

---

## Related Files

| File | Purpose |
|------|---------|
| `order-service/src/test/java/.../pact/OrderProductConsumerPactTest.java` | Consumer tests for product-service |
| `order-service/src/test/java/.../pact/OrderPaymentConsumerPactTest.java` | Consumer tests for payment-service |
| `order-service/src/test/java/.../pact/OrderInventoryConsumerPactTest.java` | Consumer tests for inventory-service |
| `product/src/test/java/.../pact/ProductCategoryConsumerPactTest.java` | Consumer tests for category-service |
| `notification-service/src/test/java/.../pact/OrderServiceConsumerPactTest.java` | Consumer tests for order-service |
| `product/src/test/java/.../pact/ProductPactProviderTest.java` | Provider verification for product-service |
| `category/src/test/java/.../pact/CategoryPactProviderTest.java` | Provider verification for category-service |
| `payment-service/src/test/java/.../pact/PaymentPactProviderTest.java` | Provider verification for payment-service |
| `inventory-service/src/test/java/.../pact/InventoryPactProviderTest.java` | Provider verification for inventory-service |
| `order-service/src/test/java/.../pact/OrderPactProviderTest.java` | Provider verification for order-service |
| `pom.xml` (parent) | Pact version (4.7.5), maven-compiler-plugin config |

---

## Quick Reference Card

```bash
# === WORKING CYCLE (copy-paste) ===

# 1. Setup
mvn clean install -pl common -DskipTests

# 2. Generate consumer contracts (all pairs)
mvn test -pl order-service -Dtest="*ConsumerPactTest"
mvn test -pl product -Dtest=ProductCategoryConsumerPactTest
mvn test -pl notification-service -Dtest=OrderServiceConsumerPactTest

# 3. Verify all providers (all use @PactFolder)
mvn test -pl product -Dtest=ProductPactProviderTest
mvn test -pl order-service -Dtest=OrderPactProviderTest
mvn test -pl category -Dtest=CategoryPactProviderTest
mvn test -pl payment-service -Dtest=PaymentPactProviderTest
mvn test -pl inventory-service -Dtest=InventoryPactProviderTest   # requires Docker/Testcontainers

# === OPTIONAL: WITH PACT BROKER ===
# docker run -d -p 9292:9292 pactfoundation/pact-broker
# mvn pact:publish -Dpact.broker.url=http://localhost:9292
# mvn test -pl product -Dtest=ProductPactProviderTest  # uses @PactBroker
```

---

## Further Reading

- [Pact JVM Documentation](https://docs.pact.io/implementation_guides/jvm/)
- [Pact Broker Documentation](https://docs.pact.io/pact_broker/)
- [Spring Cloud Contract vs Pact](https://docs.pact.io/faq/#how-is-pact-different-from-spring-cloud-contract)
- Project Spec: `docs/specs/pact-contract-testing-implementation.md`