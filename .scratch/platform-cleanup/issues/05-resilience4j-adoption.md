# 05 — Resilience4j Adoption (Circuit Breaker, Retry, TimeLimiter)

**What to build:** Apply Resilience4j annotations or programmatic usage on external service-to-service calls (via WebClient, RestClient, or Gateway) using the already-configured registries in `common/src/main/java/com/example/common/config/Resilience4jConfig.java`.

**Blocked by:** None — can start immediately (registries already exist in common module).

**Status:** ready-for-agent

- [x] Identify all external calls: WebClient/RestClient calls between services, Gateway route filters
- [x] Add `@CircuitBreaker` on WebClient calls in services (order-service → inventory-service, payment-service, etc.)
- [x] Add `@Retry` with exponential backoff on idempotent operations
- [x] Add `@TimeLimiter` on async calls to enforce timeouts
- [x] Add `@RateLimiter` on high-throughput endpoints if needed
- [x] Configure fallback methods for circuit breaker open state
- [x] Add Resilience4j actuator endpoints exposure: `management.endpoints.web.exposure.include=health,circuitbreakers,ratelimiters`
- [x] Add `@CircuitBreaker` on Gateway routes via `CircuitBreakerGatewayFilterFactory` if using Spring Cloud Gateway resilience
- [x] Run integration tests to verify resilience behavior: `mvn verify -pl order-service,payment-service,inventory-service`