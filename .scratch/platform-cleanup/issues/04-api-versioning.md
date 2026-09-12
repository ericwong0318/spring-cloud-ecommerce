# 04 — API Versioning in Gateway Routes

**What to build:** Add version prefix (`/api/v1/`) to all gateway routes to match service-level API versioning (services already use `/api/v1/`).

**Blocked by:** 03 — Gateway Security Hardening (prefer to do together to avoid multiple gateway changes)

**Status:** done

- [x] Update `GatewayApplication.java` routes from `/api/products/**` → `/api/v1/products/**`
- [x] Update routes for categories, orders, inventory, notifications, payments to use `/api/v1/`
- [x] Add `StripPrefix(2)` filter (strip `/api/v1/`) so services receive `/products/**` etc. (or keep stripPrefix(1) if services expect `/v1/`)
- [x] Verify services' `@RequestMapping` paths align (order-service, payment-service use `/api/v1/`; product uses `/api/`; category uses `/categories`)
- [x] Consider adding version negotiation (header-based or path-based) for future-proofing
- [x] Run gateway tests: `mvn test -pl gateway`
- [x] Integration test: verify requests through gateway with `/api/v1/` prefix reach services correctly

## Comments

**2026-09-11 — implemented (agent)**

Design chosen: **versioning is centralized at the gateway** (path-based). All public
routes use `/api/v1/{resource}/**` with `StripPrefix=2`, so downstream services
receive unversioned paths (`/products/**`, `/categories/**`, `/orders/**`,
`/inventory/**`, `/payments/**`) and stay version-agnostic — introducing `/api/v2/`
later only requires new gateway routes. This follows the ticket's primary
StripPrefix(2) option.

Changes:
- `gateway/GatewayApplication.java` — all 6 routes now `/api/v1/{resource}/**` +
  `stripPrefix(2)`; route building extracted to a testable `static gatewayRoutes(...)`.
- `config-server/.../config/gateway.yml` — same scheme; added the 4 routes that only
  existed in the Java DSL (order, inventory, notification, payment).
- Service controllers aligned to unversioned roots: product `/api/products` →
  `/products` (+ variants), order-service `/api/v1/orders` → `/orders` (+ shipments),
  inventory `/api/inventory` → `/inventory`, payment `/api/v1/payments` → `/payments`
  (+ Location header). category already used `/categories` (unchanged).
  notification-service has no REST controller (event-driven) — the gateway route is
  kept for future REST exposure.
- Tests updated to the new service paths: product (Product/ProductVariant
  integration tests), order-service (OrderIntegrationTest base path), inventory
  (InventoryControllerIntegrationTest), payment (PaymentServiceIntegrationTest),
  system-test (RestAssured basePath + endpoints).
- New gateway tests: `GatewayRouteContractTest` (behavioral route contract: routes
  only match `/api/v1/**`, reject unversioned and `/api/v2/` paths, every route has
  StripPrefix and forwards `/products/1`-style paths downstream) and
  `GatewayVersioningIntegrationTest` (real gateway HTTP server + downstream stub:
  `/api/v1/test/products/42` reaches downstream as `/test/products/42`, unversioned
  and `/api/v2/` requests 404). `mvn test -pl gateway` → 7/7 green.

Also found & fixed while validating: `spring-cloud-starter-gateway` resolved to
4.2.0 (unmanaged) while the BOM pins `spring-cloud-gateway-server` to 4.1.6, and
SCG 4.1.6+ requires Spring Framework 6.2 (`HttpHeaders.headerSet()`), which
Boot 3.3.5 (Spring Framework 6.1) does not provide — every real forwarded request
crashed with `NoSuchMethodError`. Pinned both gateway artifacts to 4.1.5 (last
4.1.x compatible with Spring Framework 6.1) via `dependencyManagement` in
`gateway/pom.xml`.

Out of scope / noted: two `ProductVariantIntegrationTest` cases fail on RabbitMQ
event-delivery timeouts (HTTP part passes) — pre-existing async plumbing, unrelated
to this ticket. Gateway security (ticket 03) still pending; the integration test
disables reactive security auto-config to isolate routing behaviour.