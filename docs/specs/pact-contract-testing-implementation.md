# Pact Contract Testing Implementation

## Problem Statement

The Spring Cloud Microservices Platform has provider-side Pact verification tests (`*PactProviderTest.java`) implemented across 5 services (order-service, payment-service, product-service, category-service, inventory-service), but these tests are failing because:

1. **No Consumer Contracts Exist**: Provider tests reference `target/pacts` directory which doesn't exist. Pact consumer tests (`*ConsumerPactTest.java`) have never been implemented to generate these contracts.

2. **Pact Maven Plugin Bug**: The parent POM includes `au.com.dius.pact.provider:maven:4.7.5` which has known Kotlin initialization issues (`PluginManagerKt`/`serviceProviders` uninitialized), making `mvn pact:verify` unusable.

3. **Lombok Compilation Errors**: The `payment-service` has 33 compilation errors in `PaymentServiceIntegrationTest.java` (missing `builder()`, `getOrderId()`) due to annotation processor misconfiguration in the multi-module setup.

4. **Deadlock**: Provider verification cannot run without consumer-generated contracts, but consumer tests don't exist yet.

## Solution

Implement a complete Consumer-Driven Contract (CDC) testing pipeline using Pact:

1. **Remove the broken Pact Maven plugin** from parent POM and rely on JUnit 5 provider runner (`pact-jvm-provider-junit5`)
2. **Implement Consumer Pact Tests** for all service-to-service interactions to generate contract JSON files
3. **Configure local Pact file sharing** between consumer and provider modules via `target/pacts`
4. **Fix Lombok/annotation processor configuration** in parent POM for multi-module compilation
5. **Establish CI/CD pipeline** for contract verification (consumer tests → generate pacts → provider verification)

## User Stories

1. As a **developer**, I want to run `mvn test` on a provider service and have it verify against real consumer contracts, so that I can detect breaking API changes before deployment.

2. As a **developer**, I want consumer tests to automatically generate Pact JSON files in `target/pacts`, so that provider tests can find and verify them without manual steps.

3. As a **developer**, I want the Pact Maven plugin removed and provider verification to run via standard JUnit 5 test execution, so that the Kotlin initialization bug doesn't block the build.

4. As a **developer**, I want the `common` module DTOs to compile correctly with Lombok/MapStruct annotation processors, so that dependent services (payment-service, order-service) can build and test.

5. As a **platform engineer**, I want a documented CDC workflow in CI/CD that runs consumer tests first, publishes pacts, then runs provider verification, so that contract compatibility is enforced on every PR.

6. As a **developer**, I want consumer tests for `order-service → product-service`, `order-service → payment-service`, `order-service → inventory-service`, `product-service → category-service` interactions, so that all service contracts are covered.

7. As a **developer**, I want provider tests to load pacts from both local filesystem (`target/pacts`) and Pact Broker, so that local development and CI can use different sources.

8. As a **developer**, I want clear documentation on how to run the full CDC cycle locally, so that new team members can verify contracts without deep Pact knowledge.

## Implementation Decisions

### 1. Remove Pact Maven Plugin
- **Decision**: Remove `au.com.dius.pact.provider:maven:4.7.5` from parent POM `pluginManagement` and `plugins` sections
- **Rationale**: Plugin has known Kotlin initialization bug; JUnit 5 provider runner (`pact-jvm-provider-junit5`) handles verification without the plugin
- **Impact**: Provider tests will run via `mvn test -Dtest=*PactProviderTest` instead of `mvn pact:verify`

### 2. Consumer Pact Test Implementation
- **Modules needing consumer tests**:
  - `order-service` → tests for calls to `product-service`, `payment-service`, `inventory-service`
  - `product-service` → tests for calls to `category-service`
  - `category-service` → (no downstream consumers currently)
  - `payment-service` → (no downstream consumers currently)
  - `inventory-service` → (no downstream consumers currently)

- **Consumer test pattern**: Use `au.com.dius.pact.consumer.junit5.PactConsumerTestExt` with `@Pact` annotated methods defining expected interactions
- **Output directory**: Configure `pact.test.outputDir=target/pacts` in consumer test configuration

### 3. Pact File Sharing Strategy
- **Local Development**: Consumer tests write to `target/pacts` in their module; provider tests read from `../<consumer-module>/target/pacts` via relative path or Maven dependency
- **CI/CD**: Use `pact:publish` goal (from `au.com.dius.pact:pact-jvm-provider-maven_2.13`) to publish to Pact Broker, then provider tests use `@PactBroker` annotation
- **Fallback**: Provider tests check local `target/pacts` first, then Pact Broker

### 4. Lombok/Annotation Processor Fix
- **Decision**: Add Lombok to parent POM `maven-compiler-plugin` `<annotationProcessorPaths>` for all modules
- **Configuration**:
  ```xml
  <annotationProcessorPaths>
      <path>
          <groupId>org.mapstruct</groupId>
          <artifactId>mapstruct-processor</artifactId>
          <version>${mapstruct.version}</version>
      </path>
      <path>
          <groupId>org.projectlombok</groupId>
          <artifactId>lombok</artifactId>
          <version>${lombok.version}</version>
      </path>
  </annotationProcessorPaths>
  ```
- **Build Order**: Ensure `common` module is installed first: `mvn clean install -pl common -DskipTests`

### 5. Provider Test Configuration Updates
- **Current**: Provider tests use `ProviderInfo` with `consumer.setPactSource("target/pacts")`
- **Updated**: Use `@PactFolder("../order-service/target/pacts")` annotation (JUnit 5 style) or `@PactBroker(url="http://localhost:9292")` for broker
- **State Methods**: Keep existing `@State` methods; they define provider-side test data setup

### 6. CI/CD Pipeline Integration
- **Stage 1**: `mvn test -Dtest=*ConsumerPactTest` (generates pacts)
- **Stage 2**: `mvn pact:publish -Dpact.broker.url=$PACT_BROKER_URL` (publishes to broker)
- **Stage 3**: `mvn test -Dtest=*PactProviderTest` (verifies against broker)

### 7. Pact Version Alignment
- **Decision**: Downgrade Pact dependencies from 4.7.5 to 4.6.x (stable) or 4.5.x
- **Rationale**: 4.7.5 has the Maven plugin bug; 4.6.x is stable for JUnit 5 provider/consumer

## Testing Decisions

### What Makes a Good Pact Test
- **Test External Behavior Only**: Verify request/response contracts, not implementation details
- **Consumer-Driven**: Consumer tests define what they need; provider tests verify they satisfy it
- **State Isolation**: Each `@State` method sets up specific provider state for a given interaction
- **Realistic Data**: Use realistic test data that matches production shapes

### Modules to Test
| Module | Consumer Tests | Provider Tests | Contract Direction |
|--------|---------------|----------------|-------------------|
| order-service | ✅ (product, payment, inventory) | ✅ (existing) | order → product/payment/inventory |
| product-service | ✅ (category) | ✅ (existing) | product → category |
| category-service | ❌ | ✅ (existing) | — |
| payment-service | ❌ | ✅ (existing) | — |
| inventory-service | ❌ | ✅ (existing) | — |

### Prior Art / Similar Tests
- Existing provider tests in `*/pact/*PactProviderTest.java` show the verification pattern
- Spring Boot test slices (`@SpringBootTest`, `@AutoConfigureWebTestClient`) used for provider tests
- Testcontainers used for integration tests (pattern for consumer tests with real dependencies)

### Test Execution Commands
```bash
# 1. Install common module first (fixes Lombok/DTO compilation)
mvn clean install -pl common -DskipTests

# 2. Run consumer tests to generate pacts
mvn test -pl order-service -Dtest=*ConsumerPactTest
mvn test -pl product-service -Dtest=*ConsumerPactTest

# 3. Run provider verification (local pacts)
mvn test -pl order-service -Dtest=OrderPactProviderTest
mvn test -pl product-service -Dtest=ProductPactProviderTest
mvn test -pl category-service -Dtest=CategoryPactProviderTest
mvn test -pl payment-service -Dtest=PaymentPactProviderTest
mvn test -pl inventory-service -Dtest=InventoryPactProviderTest
```

## Out of Scope

1. **Pact Broker Deployment**: Setting up a hosted Pact Broker (assumes local broker at `localhost:9292` or CI-provided)
2. **Contract Versioning/Tagging**: Advanced Pact features like tags, branches, webhooks
3. **Bi-directional Contracts**: Not using Pact's bi-directional contract testing feature
4. **Message/Async Contracts**: Only HTTP/REST contracts; async (RabbitMQ/Kafka) contracts not included
5. **Contract Migration**: No migration from existing integration tests to Pact
6. **Performance Testing**: Pact is for correctness, not performance

## Further Notes

### Current State Summary
- **Provider Tests**: 5 services have `*PactProviderTest.java` with `@State` methods defined
- **Consumer Tests**: 0 services have `*ConsumerPactTest.java` (need to create)
- **Pact Files**: 0 JSON contracts in any `target/pacts` directory
- **Plugin**: Broken Pact Maven plugin 4.7.5 in parent POM

### Recommended First Steps
1. Create ticket: "Remove Pact Maven Plugin 4.7.5 from parent POM"
2. Create ticket: "Fix Lombok annotation processor in parent POM for multi-module builds"
3. Create ticket: "Implement OrderConsumerPactTest for product-service calls"
4. Create ticket: "Implement OrderConsumerPactTest for payment-service calls"
5. Create ticket: "Implement OrderConsumerPactTest for inventory-service calls"
6. Create ticket: "Implement ProductConsumerPactTest for category-service calls"
7. Create ticket: "Update provider tests to use @PactFolder/@PactBroker annotations"
8. Create ticket: "Document local CDC workflow in CONTRIBUTING.md"

### Dependencies Between Tickets
```
1. Fix Lombok (unblocks compilation)
2. Remove Pact Maven Plugin (unblocks provider test execution)
3. Implement Consumer Tests (generates pacts)
4. Update Provider Tests (consumes pacts)
5. Document Workflow (enables team adoption)
```

### Related Files
- Parent POM: `/pom.xml` (lines 416-420: Pact Maven plugin)
- Provider Tests: 5 files in `*/pact/*PactProviderTest.java`
- Common DTOs: `/common/src/main/java/com/example/common/dto/*.java`
- Payment Service Integration Test: `/payment-service/src/test/java/.../PaymentServiceIntegrationTest.java` (33 errors)