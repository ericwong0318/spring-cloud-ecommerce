# E-Commerce Microservices Platform — Full Build Specification

## Problem Statement

Build a production-ready, portfolio-quality e-commerce platform demonstrating Spring Cloud microservices patterns. The platform must support the core e-commerce flow: browse products → manage categories → reserve inventory → place orders → process payments, with full observability, security, and test coverage.

## Solution

A 9-service Spring Cloud microservices platform with:

- **Infrastructure (3)**: Config Server, Eureka Discovery, API Gateway
- **Auth (1)**: Spring Authorization Server (OAuth2/OIDC, JWT)
- **Domain (5)**: Product, Category, Inventory, Order, Payment
- **Event backbone**: RabbitMQ (topic exchange, Spring Cloud Stream)
- **Database**: PostgreSQL per service
- **API**: REST + OpenAPI 3
- **Tests**: Full pyramid (unit, contract, integration, E2E)
- **Deploy**: Docker Compose locally, Kubernetes-ready

## User Stories

### Infrastructure & Platform
1. As a developer, I want a Config Server serving configuration from Git, so that all services share centralized config with environment-specific overrides.
2. As a developer, I want Eureka service discovery, so that services register themselves and the gateway routes via `lb://service-name`.
3. As a developer, I want an API Gateway handling routing, OAuth2 resource server validation, and rate limiting, so that external clients have a single entry point.
4. As a developer, I want an Authorization Server issuing JWTs with RS256, so that the gateway validates tokens and services trust gateway headers.

### Product & Category
5. As a shopper, I want to browse products with search, filtering, and pagination, so that I can find items to purchase.
6. As a shopper, I want to view product details (name, description, price, attributes, images), so that I can make informed decisions.
7. As a merchant, I want to manage categories in a hierarchy, so that products are organized for navigation.
8. As a merchant, I want to create/update products with variants (size, color) and attributes, so that the catalog reflects my inventory.

### Inventory
9. As a system, I want to track available stock per product per location, so that inventory levels are accurate.
10. As a system, I want to reserve stock for pending orders (15-minute TTL), so that stock isn't oversold.
11. As a system, I want to release reservations on expiry or cancellation, so that stock returns to available.
12. As a system, I want to publish `inventory.stock_reserved` / `inventory.stock_released` events to RabbitMQ, so that downstream services react.

### Order
13. As a shopper, I want to place an order with multiple line items, so that I purchase multiple products in one transaction.
14. As a system, I want to create an order in PENDING state and publish `order.created`, so that inventory reservation begins.
15. As a system, I want to transition order to RESERVED when all items are reserved, so that payment can proceed.
16. As a system, I want to transition order to PAID when payment is authorized, so that fulfillment begins.
17. As a system, I want to cancel orders when inventory publishes `inventory.stock_reservation_failed` or `ReservationExpiredEvent`, so that stale orders don't block stock.
18. As a system, I want to support partial fulfillment (backorders), so that available items ship while others wait.

### Payment
19. As a shopper, I want to pay for my order via a mocked payment gateway, so that the flow completes without real provider setup.
20. As a system, I want to authorize the full order total at order creation, so that funds are reserved.
21. As a system, I want to capture only the reserved total upon order confirmation, so that I comply with consumer protection laws.
22. As a system, I want to support idempotency keys on all payment operations, so that retries don't cause double-charges.
23. As a system, I want to publish `payment.authorized` / `payment.failed` events, so that order service transitions state.

### Cross-Cutting
24. As a developer, I want OpenAPI docs at `/swagger-ui.html` on every service, so that APIs are self-documenting.
25. As an operator, I want actuator health/prometheus endpoints on all services, so that monitoring works.
26. As a developer, I want correlation IDs propagated via `X-Correlation-Id` header, so that distributed tracing works.
27. As a developer, I want unit + integration + contract + E2E tests passing in CI, so that regressions are caught.

## Implementation Decisions

### Modules (9 Services)

| Service | Stack | Port | Purpose |
|---------|-------|------|---------|
| config-server | Spring Cloud Config, WebMVC | 8888 | Git-backed config |
| eureka-server | Netflix Eureka, WebMVC | 8761 | Service discovery |
| gateway | Spring Cloud Gateway, OAuth2 RS | 8080 | Routing, auth validation |
| auth-server | Spring Authorization Server | 9000 | OAuth2/OIDC, JWT issuance |
| product | WebFlux + JPA, PostgreSQL | 8081 | Product catalog |
| category | WebMVC + JPA, PostgreSQL | 8082 | Category hierarchy |
| inventory | WebFlux + R2DBC, PostgreSQL | 8084 | Stock, reservations |
| order | WebFlux + R2DBC, PostgreSQL | 8083 | Order lifecycle, saga |
| payment | WebFlux + JPA, PostgreSQL | 8085 | Payment processing (mock) |

### Key Interfaces / APIs

- **Gateway → Services**: Routes via `lb://service-name`; adds `X-User-Id`, `X-User-Roles`, `X-Client-Id` headers
- **Auth**: `POST /oauth2/token` (client_credentials, password grants); JWKS at `/oauth2/jwks`
- **Product**: `GET /api/v1/products`, `GET /api/v1/products/{id}`, `POST /api/v1/products`
- **Category**: `GET /api/v1/categories`, `GET /api/v1/categories/{id}`, tree endpoints
- **Inventory**: `POST /api/v1/reservations`, `DELETE /api/v1/reservations/{id}`, stock queries
- **Order**: `POST /api/v1/orders`, `GET /api/v1/orders/{id}`, `POST /api/v1/orders/{id}/cancel`
- **Payment**: `POST /api/v1/payments/authorize`, `POST /api/v1/payments/{id}/capture`, `POST /api/v1/payments/{id}/refund`

### Event Contracts (RabbitMQ)

**Exchange**: `ecommerce.events` (topic, durable)
**Routing keys**: `{aggregate}.{action}` (e.g., `order.created`, `inventory.stock_reserved`, `payment.authorized`)

| Event | Publisher | Consumers | Payload |
|-------|-----------|-----------|---------|
| `order.created` | order | inventory | orderId, items[], correlationId |
| `inventory.stock_reserved` | inventory | payment | orderId, reservationId, reservedTotal |
| `inventory.stock_reservation_failed` | inventory | order | orderId, reason |
| `ReservationExpiredEvent` | inventory (scheduler) | order | orderId, reservationId |
| `payment.authorized` | payment | order | orderId, paymentId, capturedAmount |
| `payment.failed` | payment | order | orderId, reason |

### Database Schemas (per service)

- **product**: `product`, `product_variant`, `product_attribute`, `category` (FK to category-service via code)
- **category**: `category` (self-referencing hierarchy)
- **inventory**: `inventory` (product_id, location, available, reserved), `reservation` (order_id, product_id, quantity, expires_at)
- **order**: `order`, `order_item` (status: PENDING/RESERVED/SHIPPED/BACKORDERED/CANCELLED)
- **payment**: `payment` (order_id, status: AUTHORIZED/CAPTURED/FAILED/REFUNDED, idempotency_key)
- **auth-server**: `oauth2_registered_client`, `oauth2_authorization_consent`, `oauth2_authorization`

### Auth Architecture

- **Gateway**: `OAuth2ResourceServer` with `JwtDecoder` from auth-server JWKS
- **Services**: Custom `AuthenticationManager` reading `X-User-*` headers; no JWT validation
- **Service-to-service**: Shared secret header `X-Service-Token` (mTLS future)

### Saga Choreography (Place Order)

```
1. Client → POST /api/v1/orders → Gateway → Order Service
2. Order: CREATE order (PENDING) + publish order.created
3. Inventory: consume order.created → RESERVE stock → publish stock_reserved / stock_reservation_failed
4. Payment: consume stock_reserved → AUTHORIZE → publish payment.authorized / payment.failed
5. Order: consume payment.authorized → CONFIRM (PAID) | payment.failed → CANCEL + release reservations
```

### Testing Strategy (per ADR-007)

| Layer | Tool | Scope |
|-------|------|-------|
| Unit | JUnit 5 + Mockito | Domain logic, mappers, services (>80% coverage) |
| Integration | Spring Boot Test + Testcontainers (PG + RabbitMQ) | Repositories, WebClient, saga orchestration |
| Contract | Pact JVM | Gateway ↔ Service APIs (consumer-driven) |
| E2E | Playwright | Browse → Order → Pay → Confirm (critical paths) |

### CI Pipeline

```yaml
jobs:
  test:
    - mvn test -pl <module>           # Unit
    - mvn verify -pl <module>         # Integration (Testcontainers)
    - mvn pact:verify -pl gateway     # Contract
  e2e:
    - docker-compose -f docker-compose.test.yml up -d
    - npx playwright test
```

## Out of Scope

- Notification service (email/SMS)
- User profile/address management (delegated to auth-server)
- Shopping cart (merged into order service)
- Admin dashboard / UI
- Multi-tenancy
- Real payment gateway integration (mocked)
- Advanced fraud detection
- Audit logging (separate compliance module)
- Real-time analytics dashboards

## Further Notes

- **Seam for testing**: The RabbitMQ event exchange (`ecommerce.events`) is the highest seam — contract tests at gateway, integration tests verify event flow, E2E tests the full journey.
- **Lombok removal**: Per policy, convert `@Data`/`@Builder` to explicit code/Java Records; remove lombok dependency per module.
- **Timeline**: 2-3 weeks to running skeleton (`docker-compose up` starts all 9 services).
- **Start order**: config-server → eureka-server → auth-server → gateway → domain services.