# 05 — Resilience4j Adoption (Circuit Breaker, Retry, TimeLimiter)

**What to build:** Apply Resilience4j annotations or programmatic usage on external service-to-service calls (via WebClient, RestClient, or Gateway) using the already-configured registries in `common/src/main/java/com/example/common/config/Resilience4jConfig.java`.

**Blocked by:** None — can start immediately (registries already exist in common module).

**Status:** ready-for-agent

- [ ] Identify all external calls: WebClient/RestClient calls between services, Gateway route filters
- [ ] Add `@CircuitBreaker` on WebClient calls in services (order-service → inventory-service, payment-service, etc.)
- [ ] Add `@Retry` with exponential backoff on idempotent operations
- [ ] Add `@TimeLimiter` on async calls to enforce timeouts
- [ ] Add `@RateLimiter` on high-throughput endpoints if needed
- [ ] Configure fallback methods for circuit breaker open state
- [ ] Add Resilience4j actuator endpoints exposure: `management.endpoints.web.exposure.include=health,circuitbreakers,ratelimiters`
- [ ] Add `@CircuitBreaker` on Gateway routes via `CircuitBreakerGatewayFilterFactory` if using Spring Cloud Gateway resilience
- [ ] Run integration tests to verify resilience behavior: `mvn verify -pl order-service,payment-service,inventory-service`