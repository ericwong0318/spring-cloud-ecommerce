# 12 — Contract Tests (Pact)

**What to build:** Consumer-driven contract tests between Gateway and each domain service using Pact JVM.

**Blocked by:** 04-gateway, 05-product-service, 06-category-service, 07-inventory-service, 08-payment-service, 09-order-service

**Status:** ready-for-agent

- [ ] Gateway as consumer: defines Pact contracts for each service API
- [ ] Each service as provider: verifies Pact contracts in CI
- [ ] Pact broker (local file or Docker) for publishing/verification
- [ ] Contracts cover: GET/POST products, categories, reservations, orders, payments
- [ ] `mvn pact:verify -pl gateway` passes in CI
- [ ] Contracts versioned with semantic versioning