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
| order-service | product-service | `order-service/*ConsumerPactTest.java` | Product CRUD APIs |
| order-service | payment-service | `order-service/*ConsumerPactTest.java` | Payment authorize/capture/refund |
| order-service | inventory-service | `order-service/*ConsumerPactTest.java` | Inventory reservation |
| product-service | category-service | *TODO* | Category lookup |

> **Note**: Consumer tests for `product-service → category-service` and `order-service → inventory-service` are not yet implemented (see open tickets).

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

# Product service → Category (when implemented)
# mvn test -pl product -Dtest=*ConsumerPactTest
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

Run provider verification tests. Each provider test reads pacts from the consumer's `target/pacts/` directory.

> **Current Status**: Only `ProductPactProviderTest` uses the modern `@PactFolder` annotation and works end-to-end. Other provider tests use the deprecated `ProviderInfo` API and need migration (see tickets).

```bash
# Product service provider (WORKS - uses @PactFolder)
mvn test -pl product -Dtest=ProductPactProviderTest

# Order service provider (NEEDS UPDATE - uses deprecated ProviderInfo API)
# mvn test -pl order-service -Dtest=OrderPactProviderTest

# Category service provider (NEEDS CONSUMER PACTS - product→category not implemented)
# mvn test -pl category -Dtest=CategoryPactProviderTest

# Payment service provider (NEEDS UPDATE - uses deprecated ProviderInfo API)
# mvn test -pl payment-service -Dtest=PaymentPactProviderTest

# Inventory service provider (NEEDS UPDATE - uses deprecated ProviderInfo API)
# mvn test -pl inventory-service -Dtest=InventoryPactProviderTest
```

**How it works (modern approach):** Provider tests use `@PactFolder` annotation pointing to the consumer's pact directory:
```java
@Provider("product-service")
@PactFolder("../order-service/target/pacts")  // Reads from consumer's output
class ProductPactProviderTest { ... }
```

**Legacy approach (deprecated):** Some provider tests still use `ProviderInfo` API in `@BeforeAll`:
```java
// DEPRECATED - doesn't work with Pact 4.7.5
private static ProviderInfo providerInfo;

@BeforeAll
static void setupProvider() {
    providerInfo = new ProviderInfo("payment-service");
    providerInfo.hasPactWith("order-service", consumer -> {
        consumer.setPactSource(new File("../order-service/target/pacts"));
        return INSTANCE;
    });
}
```

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

Follow this checklist to verify the working cycle (as of current implementation):

- [ ] `mvn clean install -pl common -DskipTests` — **PASS**
- [ ] `mvn test -pl order-service -Dtest=*ConsumerPactTest` — **PASS** (generates pacts)
- [ ] `ls order-service/target/pacts/*.json` — **2 files exist** (product, payment)
- [ ] `mvn test -pl product -Dtest=ProductPactProviderTest` — **PASS** (uses @PactFolder)
- [ ] `mvn test -pl order-service -Dtest=OrderPactProviderTest` — **NEEDS UPDATE** (uses deprecated ProviderInfo API)
- [ ] `mvn test -pl category -Dtest=CategoryPactProviderTest` — **NEEDS CONSUMER PACTS** (product→category not implemented)
- [ ] `mvn test -pl payment-service -Dtest=PaymentPactProviderTest` — **NEEDS UPDATE** (uses deprecated ProviderInfo API)
- [ ] `mvn test -pl inventory-service -Dtest=InventoryPactProviderTest` — **NEEDS UPDATE** (uses deprecated ProviderInfo API)

> **Note**: The full cycle passes for `order-service → product-service`. Other contracts require consumer test implementation and/or provider test migration to `@PactFolder`.

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
      - name: Verify providers (working)
        run: mvn test -pl product -Dtest=ProductPactProviderTest
      # TODO: Enable after provider test migration to @PactFolder
      # - name: Verify other providers
      #   run: |
      #     mvn test -pl order-service -Dtest=OrderPactProviderTest
      #     mvn test -pl category -Dtest=CategoryPactProviderTest
      #     mvn test -pl payment-service -Dtest=PaymentPactProviderTest
      #     mvn test -pl inventory-service -Dtest=InventoryPactProviderTest
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
┌─────────────────┐
│ 1. Fix Lombok   │ ◀── Unblocks compilation
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 2. Remove Pact  │ ◀── Unblocks provider test execution
│    Maven Plugin │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 3. Consumer     │ ◀── Generates pacts
│    Tests        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 4. Update       │ ◀── Consumes pacts
│    Provider     │
│    Tests        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ 5. Document     │ ◀── Enables team adoption
│    Workflow     │  (THIS DOCUMENT)
└─────────────────┘
```

---

## Related Files

| File | Purpose |
|------|---------|
| `order-service/src/test/java/.../pact/OrderProductConsumerPactTest.java` | Consumer tests for product-service |
| `order-service/src/test/java/.../pact/OrderPaymentConsumerPactTest.java` | Consumer tests for payment-service |
| `product/src/test/java/.../pact/ProductPactProviderTest.java` | Provider verification for product-service |
| `category/src/test/java/.../pact/CategoryPactProviderTest.java` | Provider verification for category-service |
| `payment-service/src/test/java/.../pact/PaymentPactProviderTest.java` | Provider verification for payment-service |
| `inventory-service/src/test/java/.../pact/InventoryPactProviderTest.java` | Provider verification for inventory-service |
| `pom.xml` (parent) | Pact version (4.7.5), maven-compiler-plugin config |

---

## Quick Reference Card

```bash
# === WORKING CYCLE (copy-paste) ===

# 1. Setup
mvn clean install -pl common -DskipTests

# 2. Generate contracts (order-service → product, payment)
mvn test -pl order-service -Dtest="*ConsumerPactTest"

# 3. Verify providers (currently only product-service works end-to-end)
mvn test -pl product -Dtest=ProductPactProviderTest

# === OTHER PROVIDERS (need updates) ===
# mvn test -pl order-service -Dtest=OrderPactProviderTest        # needs @PactFolder migration
# mvn test -pl category -Dtest=CategoryPactProviderTest         # needs consumer pacts
# mvn test -pl payment-service -Dtest=PaymentPactProviderTest   # needs @PactFolder migration
# mvn test -pl inventory-service -Dtest=InventoryPactProviderTest # needs @PactFolder migration

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