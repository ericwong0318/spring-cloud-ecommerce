# ADR 010: Consumer-Driven Contracts with Pact (Modern @PactFolder Approach)

## Status
Accepted

## Context
Microservices need contract verification to prevent breaking changes. Previous implementation used deprecated `ProviderInfo` API (Pact 3.x style) which doesn't work with Pact 4.x. Consumer tests were missing for several service pairs.

## Decision
Adopt **Consumer-Driven Contracts (CDC)** using **Pact 4.7.5** with modern annotations:
- `@PactFolder` for local file-based verification (no broker required for local dev)
- `@PactBroker` for CI/CD integration (optional)
- Consumer tests generate pacts in `target/pacts/`
- Provider tests consume from consumer's `target/pacts/` via relative path

## Consequences

### Positive
- Fast local feedback loop (no broker needed)
- Contracts versioned with code (Git)
- Explicit consumer-provider relationships
- Breaking changes caught at provider verification time
- Works with Testcontainers for real DB verification

### Negative
- Consumer tests must run before provider tests (ordering dependency)
- Pact file paths must be maintained (`../consumer/target/pacts`)
- Initial setup effort for each consumer-provider pair

## Contract Map

| Consumer | Provider | Consumer Test | Provider Test | Status |
|----------|----------|---------------|---------------|--------|
| order-service | product-service | OrderProductConsumerPactTest | ProductPactProviderTest | ✅ Working |
| order-service | payment-service | OrderPaymentConsumerPactTest | PaymentPactProviderTest | ✅ Working |
| order-service | inventory-service | OrderInventoryConsumerPactTest | InventoryPactProviderTest | ✅ Working |
| product-service | category-service | ProductCategoryConsumerPactTest | CategoryPactProviderTest | ✅ Working |
| notification-service | order-service | OrderServiceConsumerPactTest | OrderPactProviderTest | ✅ Working |

## Implementation Pattern

### Consumer Test (Modern)
```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "payment-service", port = "8086")
class OrderPaymentConsumerPactTest {

    @Pact(consumer = "order-service")
    V4Pact authorizePayment(PactBuilder builder) {
        return builder
            .given("valid authorize request")
            .uponReceiving("A request to authorize a payment")
            .path("/payments/authorize")
            .method("POST")
            .body(new PactDslJsonBody()
                .integerType("orderId", 1)
                .decimalType("amount", 99.99)
                .stringType("currency", "USD")
                ...
            )
            .willRespondWith()
            .status(201)
            .body(...)
            .toPact();
    }
}
```

### Provider Test (Modern)
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Provider("payment-service")
@PactFolder("../order-service/target/pacts")  // Relative to provider module
@ExtendWith(PactVerificationInvocationContextProvider.class)
class PaymentPactProviderTest {
    
    @State("valid authorize request")
    void validAuthorizeRequest() { /* mock setup */ }
    
    @TestTemplate
    void pactVerificationTestTemplate(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
```

## Local Workflow
```bash
# 1. Install common (DTOs)
mvn clean install -pl common -DskipTests

# 2. Generate consumer pacts
mvn test -pl order-service -Dtest=*ConsumerPactTest

# 3. Verify providers
mvn test -pl product -Dtest=ProductPactProviderTest
mvn test -pl payment-service -Dtest=PaymentPactProviderTest
mvn test -pl inventory-service -Dtest=InventoryPactProviderTest
mvn test -pl category -Dtest=CategoryPactProviderTest
mvn test -pl order-service -Dtest=OrderPactProviderTest
```

## CI/CD Integration
```yaml
# .github/workflows/pact.yml
jobs:
  consumer-tests:
    steps:
      - run: mvn clean install -pl common -DskipTests
      - run: mvn test -pl order-service -Dtest="*ConsumerPactTest"
      - run: mvn test -pl product -Dtest="*ConsumerPactTest"
      - run: mvn test -pl notification-service -Dtest="*ConsumerPactTest"
      - uses: actions/upload-artifact@v4
        with:
          name: pacts
          path: |
            order-service/target/pacts/
            product/target/pacts/
            notification-service/target/pacts/

  provider-tests:
    needs: consumer-tests
    steps:
      - uses: actions/download-artifact@v4
        with:
          name: pacts
      - run: mvn test -pl product -Dtest=ProductPactProviderTest
      - run: mvn test -pl payment-service -Dtest=PaymentPactProviderTest
      # ... etc
```

## Troubleshooting
| Issue | Fix |
|-------|-----|
| "No pacts found" | Run consumer tests first; check `@PactFolder` path |
| "Provider state not found" | Add `@State("exact name")` method in provider test |
| "400 vs 500 validation" | Ensure `@ControllerAdvice` with `@ExceptionHandler(WebExchangeBindException.class)` in test context |
| Compilation errors | Install `common` module first (Lombok/MapStruct processors) |

## Related
- `docs/pact-cdc-workflow.md` (detailed workflow guide)
- `.scratch/pact-contract-testing/issues/`
