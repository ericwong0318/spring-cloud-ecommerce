# 07 — Remove Hardcoded Values in OrderServiceTest

**What to build:** Replace hardcoded exchange names ("order.exchange", "ecommerce.events") in `OrderServiceTest.java` with `@Value` injection or test properties.

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Add `@Value("${rabbitmq.exchange.order}")` and `@Value("${rabbitmq.exchange.ecommerce}")` fields to test class
- [x] Create `src/test/resources/application-test.yml` with test exchange names
- [x] Use `@TestPropertySource` to inject test values
- [x] Update `OrderServiceTest.setUp()` to use injected values instead of hardcoded strings
- [x] Run test: `mvn test -pl order-service -Dtest=OrderServiceTest`
