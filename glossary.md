# Glossary — Ubiquitous Language

## Core Domain Terms

| Term | Definition | Owner Service |
|------|------------|---------------|
| **Product** | A sellable item with SKU, name, description, price, attributes | product |
| **Category** | Hierarchical classification for products (tree structure) | category |
| **Inventory** | Available stock quantity per product per location | inventory |
| **Reservation** | Temporary hold on inventory for a pending order (TTL: 15 min) | inventory |
| **Order** | Customer's purchase intent; contains line items, totals, status | order |
| **Order Line Item** | Single product + quantity + price snapshot within an order | order |
| **Payment** | Financial transaction authorizing/capturing funds for an order | payment |
| **Saga** | Long-running transaction across services via choreographed events | order (orchestrator) |
| **Outbox Event** | Durable event stored in `outbox_event` table for reliable publishing | all domain services |

## Order Statuses
| Status | Meaning | Next Valid States |
|--------|---------|-------------------|
| `PENDING` | Created, awaiting inventory reservation | `RESERVED`, `CANCELLED` |
| `RESERVED` | Stock held, awaiting payment | `PAID`, `CANCELLED` |
| `PAID` | Payment authorized, order confirmed | `FULFILLED`, `REFUNDED` |
| `FULFILLED` | Shipped/delivered | — |
| `CANCELLED` | Aborted before payment | — |
| `REFUNDED` | Payment reversed after fulfillment | — |

## Inventory States
| State | Meaning |
|-------|---------|
| `AVAILABLE` | On hand, can be reserved |
| `RESERVED` | Held for pending order (TTL applies) |
| `ALLOCATED` | Confirmed for paid order |
| `UNAVAILABLE` | Damaged, lost, or otherwise unsellable |

## Payment States
| State | Meaning |
|-------|---------|
| `AUTHORIZED` | Funds held, not yet captured |
| `CAPTURED` | Funds transferred (order → PAID) |
| `FAILED` | Authorization declined |
| `REFUNDED` | Captured funds returned |
| `VOIDED` | Authorization released without capture |

## Technical Terms

| Term | Definition |
|------|------------|
| **Gateway** | Single entry point (Spring Cloud Gateway); routes, auth, rate limiting |
| **Service Discovery** | Eureka; clients register, gateway discovers via `lb://service-name` |
| **Config Server** | Centralized config from Git; services fetch on startup/refresh |
| **Auth Server** | Spring Authorization Server; issues JWTs, manages clients/users |
| **JWT** | JSON Web Token; access token with claims (sub, roles, client_id) |
| **JWKS** | JSON Web Key Set; public keys for JWT verification at `/oauth2/jwks` |
| **Outbox Poller** | Background task reading unpublished outbox events → message broker |
| **Idempotency Key** | Client-generated UUID ensuring duplicate requests are safe |
| **Correlation ID** | Trace ID propagated via headers (`X-Correlation-Id`) for distributed tracing |
| **Testcontainers** | Library spinning real PostgreSQL in Docker for integration tests |
| **Pact** | Consumer-driven contract testing framework |

## API Conventions

| Concept | Convention |
|---------|------------|
| **Base Path** | `/api/v1/{resource}` |
| **Collection** | `GET /api/v1/products?cursor=&limit=20` |
| **Single Resource** | `GET /api/v1/products/{id}` |
| **Create** | `POST /api/v1/orders` → `201 Location: /api/v1/orders/{id}` |
| **Update** | `PUT /api/v1/products/{id}` (full), `PATCH` (partial) |
| **Delete** | `DELETE /api/v1/products/{id}` → `204` |
| **Error Format** | RFC 7807 `application/problem+json` |
| **Auth Header** | `Authorization: Bearer <jwt>` |

## Event Naming
```
{aggregate}.{action}     (past tense)
Examples:
  order.created
  inventory.stock_reserved
  payment.authorized
  order.cancelled
```

## Service Ports (Default)
| Service | Port | Context Path |
|---------|------|--------------|
| config-server | 8888 | / |
| eureka-server | 8761 | / |
| gateway | 8080 | / |
| auth-server | 9000 | / |
| product | 8081 | /api/v1 |
| category | 8082 | /api/v1 |
| inventory | 8084 | /api/v1 |
| order | 8083 | /api/v1 |
| payment | 8085 | /api/v1 |

---
*This glossary is the single source of truth for domain language. Update when new concepts emerge.*