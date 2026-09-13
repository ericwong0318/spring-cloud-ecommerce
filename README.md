# Spring Cloud Microservices E-Commerce Platform

A production-ready, cloud-native E-Commerce microservices platform built with **Spring Boot 3.3.5** and **Spring Cloud 2023.0.4** on **Java 21**.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Database Schema](#database-schema)
- [Deployment](#deployment)
- [CI/CD](#cicd)

---

## Overview

This platform is a distributed E-Commerce system demonstrating modern microservice patterns. It supports **product catalog management**, **order processing**, and **category organization** with full OAuth2 security, service discovery, and centralized configuration.

### Core Features
- 🛍️ **Product Management** — CRUD operations with PostgreSQL persistence
- 📦 **Order Management** — Reactive order processing with R2DBC + PostgreSQL
- 🏷️ **Category Management** — Categorize and organize products
- 🔐 **OAuth2 Authorization** — JWT-based authentication with Spring Authorization Server
- 🌐 **API Gateway** — Centralized routing with Spring Cloud Gateway
- 🔍 **Service Discovery** — Netflix Eureka for dynamic service registration
- ⚙️ **Centralized Configuration** — Spring Cloud Config Server (Git backend)
- 📊 **Observability** — OpenTelemetry, Splunk Observability Cloud
- 🐳 **Containerization** — Multi-stage Docker builds
- ♻️ **Resilience** — Resilience4j circuit breakers, retries, rate limiters

---

## Architecture

### High-Level System Diagram

```
┌──────────────────────────┐
      │      Client (Browser)    │
      └────────────┬─────────────┘
                   │ HTTPS
                   ▼
      ┌──────────────────────────┐
      │   API Gateway :8080      │
      │  (Spring Cloud Gateway)  │
      │  + OAuth2 Resource Srv   │
      └────┬─────────┬───────┬───┘
           │         │       │
   ┌───────┘         │       └───────────┐
   │   Auth Check    │                   │
   ▼                 ▼                   ▼
┌────────────┐ ┌─────────────┐   ┌────────────────┐
│ Auth Srv :9000│ │Product Svc :8081│   │Category Svc:8082│
│ (OAuth2/JWT)  │ │ (WebFlux+JPA) │   │ (WebMVC+JPA)   │
└────────────┘ └──────┬────────┘   └───────┬────────┘
                     │                    │
                     │   ┌────────────────┘
                     │   │
                     ▼   ▼
          ┌────────────────────┐
          │  Order Svc :8083   │
          │ (WebFlux + R2DBC)  │
          └────────┬───────────┘
                   │ Reactive
                   ▼
          ┌─────────────────────────────────────┐
          │  Payment Svc :8085  │ Inventory :8084│
          │ (WebFlux + R2DBC)   │ (WebFlux+R2DBC) │
          └────────┬────────────┴────────┬───────┘
                   │                     │
          ┌────────┴────────────┐        │
          ▼                     ▼        ▼
   ┌───────────────┐    ┌─────────────────┐
   │ RabbitMQ      │    │ PostgreSQL DBs  │
   │ (Event Bus)   │    │ product_db,     │
   └───────────────┘    │ category_db,    │
                        │ order_db,       │
                        │ payment_db,     │
                        │ inventory_db,   │
                        │ notification_db,│
                        │ oauth2_db       │
                        └─────────────────┘

┌─────────────────────────────────────────────────────────────────┐
    │                    INFRASTRUCTURE LAYER                          │
    │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐   │
    │  │ Config Srv :8888 │  │ Eureka Srv :8761 │  │ Splunk OTel  │   │
    │  │ (Git backend)    │  │ (Service Reg.)   │  │  Collector   │   │
    │  └──────────────────┘  └──────────────────┘  └──────────────┘   │
    └─────────────────────────────────────────────────────────────────┘
```

### Request Flow (Authenticated Order Placement)

```
1. Client → POST /api/v1/orders (Bearer Token)
       │
       ▼
2. Gateway validates JWT (via Auth Server public key)
       │
       ▼
3. Gateway routes via lb://order-service
       │
       ▼
4. Order Service → R2DBC → PostgreSQL (order_db)
       │
       ▼
5. (Optional) Order Service → Product Service (HTTP)
       │
       ▼
6. Response → Client
```

### Service Interaction Patterns

| Pattern | Implementation |
|---------|----------------|
| **Synchronous** | `WebClient` / `RestClient` between services |
| **Asynchronous** | RabbitMQ for event-driven flows (order events, product events, payment events, inventory events) |
| **Service Discovery** | Eureka + `@LoadBalanced` `RestClient` |
| **Configuration** | `bootstrap.yml` → Config Server → Git repo |
| **Security** | OAuth2 JWT validated at Gateway; propagated downstream |
| **Database per Service** | Each service owns its PostgreSQL schema (no shared DB) |

---

## Domain Model Specification

This section describes the core domain model for Order Management & Payments.

### Problem Statement

The system needs a robust, compliant order-to-payment flow that respects legal/regulatory constraints (charging only for stock that is actually reserved) while maintaining loose coupling between Order, Inventory, and Payment bounded contexts.

### Solution

A decoupled, event-driven architecture where:

- **Order** is the aggregate root; it owns OrderItems with per-line status (PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED).
- **Inventory** manages stock and reserves via `reservedQuantity`; it publishes `ReservationExpiredEvent` when a reservation exceeds 15 minutes.
- **Payment** authorizes the full `orderTotal` at order creation but captures only `reservedTotal` (sum of RESERVED OrderItems) upon order confirmation. Backordered items trigger a separate billing event.
- **Events** are published directly to RabbitMQ with publisher confirms; consumers use database idempotency tables.
- **ShipmentItems** are denormalized copies linked by `orderItemId` (no cross-aggregate cascades).
- **Cancellation** is driven by Inventory → `ReservationExpiredEvent` → order-service transitions Order to CANCELLED.

### User Stories

1. **Place Order** — As a customer, I want to place an order with multiple line items in one transaction
2. **Order Status** — As a customer, I want to see my order status (PENDING, RESERVED, SHIPPED, DELIVERED, CANCELLED)
3. **Reserve Inventory** — As a merchant, I want to reserve inventory for each order item before confirming payment
4. **Capture Payment for Reserved Only** — As a merchant, I want to capture payment only for reserved items (not backordered ones)
5. **Auto-Cancel Expired Reservations** — As a system, I want to automatically cancel orders whose reservations expire after 15 minutes
6. **Multi-Shipping** — As a system, I want to ship orders in multiple shipments from different warehouses
7. **Shipment Tracking** — As a system, I want to track shipments with tracking numbers and carrier information
8. **Notifications** — As a customer, I want to receive notifications at key milestones (order placed, reserved, shipped, delivered)
9. **Auth/Capture Flow** — As a payment provider, I want to authorize full order amount at creation, capture only reserved portion upon confirmation
10. **Idempotency Keys** — As a payment service, I want to support idempotency keys to ensure exactly-once charging
11. **Backorders** — As a system, I want to handle backorders transparently — reserve what's available, backorder the remainder
12. **Partial Refunds** — As a system, I want to support partial refunds for cancelled orders
13. **Consolidated View** — As an operator, I want to view all active orders, reservations, and shipments
14. **Bounded Contexts** — As a developer, I want clear bounded contexts with well-defined responsibilities

### Technical Clarifications

- **Reservation TTL**: Fixed 15 minutes; no extension mechanism. Payment delays beyond 15 min result in reservation expiry and potential oversell (business risk accepted).
- **Payment Authorization**: Full `orderTotal` is authorized at order creation; only `reservedTotal` (sum of RESERVED OrderItems) is captured upon confirmation. Backordered items are charged separately via a backorder billing event.
- **Idempotency**: Every payment request includes a unique `idempotencyKey` (UUID) to guarantee exactly-once charging.
- **ShipmentItems**: Denormalized copy with `orderItemId` reference; no cross-aggregate cascades (Inventory → ShipmentItems via OrderItem status transitions).
- **Event Delivery**: RabbitMQ direct publishing with publisher confirms; consumers use `processed_events(event_id PK, processed_at)` table for idempotency.
- **Outbox**: Replaced by direct RabbitMQ publishing (simpler, lower latency). Outbox pattern was considered but rejected due to operational complexity.

### Schema Changes

- **OrderItem** — add `status` (PENDING, RESERVED, SHIPPED, BACKORDERED, CANCELLED), `reservedAt` (timestamp)
- **Order** — no structural changes (already has status enum)
- **Inventory** — no structural changes (already has `reservedQuantity`)
- **Payment** — new `PaymentEvent` enum values (SUCCESS, FAILED, REFUNDED)
- **New event**: `ReservationExpiredEvent` (triggered by Inventory when reservation > 15 min)

### API Contracts

- **OrderService** — `POST /orders` (creates Order + OrderItems), `PUT /orders/{id}/cancel`, `GET /orders/{id}`, `POST /orders/{id}/reserve` (internal), `POST /orders/{id}/capture` (internal)
- **PaymentService** — `POST /api/v1/payments/authorize`, `POST /api/v1/payments/{id}/capture`, `POST /api/v1/payments/{id}/refund`
- **Event consumption** — `ReservationExpiredEvent` consumed by `order-service` to transition Order to CANCELLED

---

## Services

| Service | Port | Framework | Database | Purpose |
|---------|------|-----------|----------|---------|
| **config-server** | 8888 | Spring Cloud Config | — | Centralized configuration (Git backend) |
| **eureka-server** | 8761 | Netflix Eureka | — | Service discovery & registration |
| **gateway** | 8080 | Spring Cloud Gateway | — | API routing + OAuth2 resource server |
| **auth-server** | 9000 | Spring Authorization Server | PostgreSQL (oauth2_db) | OAuth2 token issuance (JWT) |
| **product** | 8081 | WebFlux + JPA | PostgreSQL (product_db) | Product catalog CRUD |
| **category** | 8082 | WebMVC + JPA | PostgreSQL (category_db) | Category management CRUD |
| **order-service** | 8083 | WebFlux + R2DBC | PostgreSQL (order_db) | Reactive order processing |
| **inventory-service** | 8084 | WebFlux + R2DBC | PostgreSQL (inventory_db) | Inventory management, stock reservation |
| **payment-service** | 8085 | WebFlux + R2DBC | PostgreSQL (payment_db) | Payment authorization, capture, refunds |
| **notification-service** | 8086 | WebFlux + JPA | PostgreSQL (notification_db) | Email notifications via RabbitMQ |

### Service Dependencies & Start Order

```
config-server  →  eureka-server  →  auth-server  →  gateway  →  business services
     (8888)          (8761)           (9000)         (8080)    (8081/8082/8083/8084/8085/8086)
```

---

## Technology Stack

| Category | Technologies |
|----------|--------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.3.5, Spring Cloud 2023.0.4 |
| **Build** | Maven (multi-module), Maven Wrapper |
| **Databases** | PostgreSQL 16, H2 (test only) |
| **Persistence** | Spring Data JPA, Spring Data R2DBC (reactive) |
| **Migrations** | Flyway |
| **Service Discovery** | Netflix Eureka |
| **API Gateway** | Spring Cloud Gateway (reactive) |
| **Security** | Spring Security, Spring Authorization Server (OAuth2/JWT) |
| **Resilience** | Resilience4j (Circuit Breaker, Retry, Rate Limiter, Bulkhead) |
| **Message Broker** | RabbitMQ |
| **Observability** | OpenTelemetry, Splunk Observability Cloud |
| **API Docs** | SpringDoc OpenAPI 3 (Swagger UI) |
| **Container** | Docker (multi-stage builds), Docker Compose |
| **Testing** | JUnit 5, Mockito, RestAssured, Testcontainers |
| **CI/CD** | GitHub Actions |

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose (for full stack)
- PostgreSQL 16+ (or use Docker)

### Quick Start (Docker)

```bash
# Clone repository
git clone https://github.com/your-org/spring-cloud-project.git
cd spring-cloud-project

# Start entire platform
docker-compose up -d

# Verify health
curl http://localhost:8888/actuator/health   # config-server
curl http://localhost:8761/actuator/health   # eureka-server
curl http://localhost:9000/actuator/health   # auth-server
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # product
curl http://localhost:8082/actuator/health   # category
curl http://localhost:8083/actuator/health   # order
curl http://localhost:8084/actuator/health   # inventory
curl http://localhost:8085/actuator/health   # payment
curl http://localhost:8086/actuator/health   # notification
```

### Local Development (without Docker)

```bash
# Start infrastructure first
docker-compose up -d postgres rabbitmq config-server eureka-server

# Then start services in order
mvn spring-boot:run -pl config-server
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl auth-server
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl notification-service
```

### Build & Test

```bash
# Build all modules
mvn clean install -DskipTests

# Build all Docker images
mvn spring-boot:build-image -Pdocker

# Run unit tests
mvn test

# Run integration tests (requires Docker for Testcontainers)
mvn verify -Dskip.unit.tests=true

# Code coverage report
mvn jacoco:report
```

#### Makefile targets
```bash
make build-all    # build all service images with --network=host
make build <svc>  # build a specific service image
make test         # run unit tests
make verify       # run integration tests
make up           # docker-compose up -d
make down         # docker-compose down
make logs         # follow docker-compose logs
make clean        # Maven clean + docker prune
make help         # list all targets
```

---

## API Documentation

Each service exposes OpenAPI/Swagger documentation:

| Service | Swagger UI | OpenAPI JSON |
|---------|-----------|--------------|
| Product | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Category | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Order | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
| Inventory | http://localhost:8084/swagger-ui.html | http://localhost:8084/v3/api-docs |
| Payment | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| Notification | http://localhost:8086/swagger-ui.html | http://localhost:8086/v3/api-docs |
| Auth | http://localhost:9000/swagger-ui.html | http://localhost:9000/v3/api-docs |

### Validation Rules Reference

Auto-generated from Jakarta Validation annotations on DTO fields:
- **[VALIDATION.md](VALIDATION.md)** — Per-module Markdown tables with Field | Annotation | Parameters | Schema Path | Valid Example | Invalid Example

### Sample Product API
```bash
# Get all products (via Gateway)
curl http://localhost:8080/api/v1/products

# Get product by ID
curl http://localhost:8080/api/v1/products/1

# Create product (requires OAuth2 token)
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":999.99,"categoryId":1}'
```

### API Versioning

All public API routes are **versioned at the gateway** using a path-based scheme:

- Public (external): `GET /api/v1/products`, `POST /api/v1/orders`, etc.
- The gateway applies `StripPrefix=2` and forwards an **unversioned** path to the
  downstream service (e.g. `/products`, `/orders`), so services stay
  version-agnostic.
- Introducing `api/v2` later only requires new gateway routes — no service changes.

| Resource | Public (via Gateway :8080) | Service (internal) |
|----------|-----------------------------|---------------------|
| Product  | `/api/v1/products/**`       | `/products/**`      |
| Category | `/api/v1/categories/**`     | `/categories/**`    |
| Order    | `/api/v1/orders/**`         | `/orders/**`        |
| Inventory| `/api/v1/inventory/**`      | `/inventory/**`     |
| Payment  | `/api/v1/payments/**`       | `/payments/**`      |

---

## Security

### OAuth2 Token Flow

```
┌────────┐                                          ┌──────────────┐
│ Client │ ──── 1. POST /oauth2/token ────────────▶ │ Auth Server  │
│        │ ◀── 2. JWT (RS256) ───────────────────── │  :9000       │
│        │                                          └──────────────┘
│        │ ──── 3. GET /api/v1/orders + Bearer JWT ───▶ ┌──────────────┐
│        │                                          │   Gateway    │
│        │                                          │   :8080      │
│        │                                          │  (validates) │
│        │                                          └──────┬───────┘
│        │                                                 │
│        │                                          ┌──────▼───────┐
│        │ ◀── 4. Response ───────────────────────  │ Order Svc    │
└────────┘                                          │   :8083      │
                                                    └──────────────┘
```

### Get a Token
```bash
# Client credentials flow
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=product-service&client_secret=secret&scope=read,write"
```

### Registered Clients
- `product-service` / `secret` (scope: read, write)
- `category-service` / `secret` (scope: read, write)

---

## Database Schema

### product_db
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL PK | Product ID |
| name | VARCHAR(255) | Product name |
| description | TEXT | Description |
| price | DECIMAL(10,2) | Price |
| category_id | BIGINT | FK → category |
| created_at | TIMESTAMP | Creation time |

### category_db
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL PK | Category ID |
| name | VARCHAR(255) | Category name |
| description | TEXT | Description |

### order_db
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL PK | Order ID |
| customer_id | VARCHAR(255) | Customer reference |
| status | VARCHAR(50) | Order status |
| total_amount | DECIMAL(10,2) | Total amount |
| created_at | TIMESTAMP | Creation time |
| updated_at | TIMESTAMP | Last update |

### oauth2_db
Standard Spring Authorization Server schema (clients, users, tokens, etc.)

---

## Deployment

### Docker Images
Each service has a multi-stage Dockerfile with OpenTelemetry Java agent (v2.8.0).

Build all images locally (requires Docker daemon):
```bash
# Build all images
docker compose build

# Build a single service
docker compose build config-server

# OR use the Makefile
make build-all
```

> **OrbStack**: The OTel agent download may fail in some network setups. The Dockerfile uses `wget --timeout=30` with `mkdir -p /app` pre-created to handle this.

### Docker Compose
```bash
docker-compose up -d        # start full stack (with otel-collector)
docker-compose down         # stop
docker-compose logs -f      # view logs
docker-compose build        # rebuild images
```

### Kubernetes (OrbStack Recommended)
This project includes Kubernetes manifests in `k8s/` for deploying to OrbStack's built-in Kubernetes cluster.

#### Prerequisites
- OrbStack app running (provides Docker + single-node K8s)
- `kubectl` configured for `orbstack` context: `kubectl config use-context orbstack`

#### Deploy Splunk OTel Collector
```bash
helm repo add splunk-otel-collector-chart https://signalfx.github.io/splunk-otel-collector-chart
helm repo update

helm install splunk-otel-collector splunk-otel-collector-chart/splunk-otel-collector \
  --namespace splunk-otel --create-namespace \
  --set "splunkObservability.accessToken=YOUR_TOKEN,\
  clusterName=java-spring-boot,\
  splunkObservability.realm=sg0,\
  splunkObservability.profilingEnabled=true,\
  environment=prod,\
  operator.enabled=false,\
  operatorcrds.install=false,\
  agent.discovery.enabled=true"
```

> **Note**: The operator is disabled (`operator.enabled=false`) to avoid webhook race conditions in single-node clusters. Auto-instrumentation via namespace annotation is still available.

#### Deploy Services
```bash
# Create namespace with auto-instrumentation label
kubectl create namespace ecommerce
kubectl label namespace ecommerce instrumentation.opentelemetry.io/inject-java=true --overwrite

# Apply all service deployments
kubectl apply -f k8s/deployments.yaml

# Check status
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
```

#### Build & Load Images into OrbStack
```bash
# Build all images with host networking (required for OrbStack)
./build-images.sh

# Images are now available in OrbStack's Docker daemon
# No registry push needed for local dev
```

#### Access Services
```bash
# Gateway (exposed on port 80)
kubectl port-forward -n ecommerce svc/gateway 8080:80

# Config Server
kubectl port-forward -n ecommerce svc/config-server 8888:8888

# Eureka Dashboard
kubectl port-forward -n ecommerce svc/eureka-server 8761:8761

# Product API
kubectl port-forward -n ecommerce svc/product 8081:8081
```

#### Verify Observability
```bash
# Check OTel agent status
kubectl logs -n splunk-otel daemonset/splunk-otel-collector-agent

# Check cluster receiver
kubectl logs -n splunk-otel deploy/splunk-otel-collector-k8s-cluster-receiver

# View app logs with OTel annotations
kubectl logs -n ecommerce -l app=gateway
```

### Cleanup
```bash
# Remove all resources
kubectl delete namespace ecommerce
helm uninstall splunk-otel-collector -n splunk-otel
kubectl delete namespace splunk-otel
```

---

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`):
1. **validate** — Maven compile + syntax check
2. **test** — Unit tests + integration tests (Testcontainers)
3. **build** — Full Maven build
4. **validation-md-generation** — Generate VALIDATION.md from Jakarta Validation annotations
5. **docker** — Build & push Docker images
6. **security-scan** — OWASP dependency check + Trivy

### Pre-commit Hook

To regenerate `VALIDATION.md` locally before committing:

```bash
# Install the pre-commit hook
ln -sf ../../ci/validation/pre-commit-hook.sh .git/hooks/pre-commit

# Or run manually
mvn exec:java -pl ci/validation -Dexec.mainClass=com.example.validation.ValidationMarkdownGenerator -Dexec.args="VALIDATION.md"
```

---

## Testing

| Module | Unit Tests | Integration Tests |
|--------|-----------|-------------------|
| config-server | 0 | 0 |
| eureka-server | 0 | 0 |
| gateway | 0 | 0 |
| product | 1 | 1 |
| category | 1 | 2 |
| auth-server | 0 | 0 |
| order-service | 12 | 0 |
| inventory-service | 14 | 0 |
| payment-service | 0 | 8 |
| notification-service | 7 | 0 |
| system-test | 0 | 4 (Testcontainers) |
| **Total** | **35** | **15** |

Test patterns:
- **Unit** — `@SpringBootTest` with mocked dependencies (Mockito)
- **Integration** — RestAssured with random port + test security config
- **Database** — Testcontainers (PostgreSQL) for integration tests
- **System** — Full stack E2E tests in `system-test` module

### Testing Decisions

- **External behavior testing**: Tests verify order state transitions, reservation expiry, payment authorization/capture, and cancellation flow.
- **Unit tests**: Each service module has unit tests for core logic (status transitions, reservation calculations, payment flows).
- **Integration tests**: End-to-end tests for order → reserve → capture → cancel → cancellation flow; payment authorization/capture; shipment creation.
- **Prior art**: Existing `system-test` module already uses Testcontainers for PostgreSQL; similar pattern is used for payment and order integration tests.
- **Idempotency tests**: Verify that duplicate payment requests with same idempotency key produce the same result.
- **Reservation expiry tests**: Simulate time passing beyond 15 minutes and verify order cancellation.
- **Backorder flow**: Test partial reservation + backorder scenario; verify backordered items are charged separately.

### Running Tests

```bash
# Run all unit tests (no Docker required)
mvn test

# Run tests for a specific module
mvn test -pl product
mvn test -pl order-service
mvn test -pl inventory-service
mvn test -pl notification-service

# Run integration tests (requires Docker/OrbStack for Testcontainers)
mvn verify -Dskip.unit.tests=true

# Run system tests (full stack E2E)
mvn test -pl system-test

# Code coverage report
mvn clean verify jacoco:report
# Reports at: */target/site/jacoco/index.html
```

---

## Testcontainers Setup (Docker/OrbStack)

Testcontainers is used for integration/system tests requiring real PostgreSQL containers.

### Prerequisites
- **Docker** or **OrbStack** running
- Docker daemon accessible at `unix:///var/run/docker.sock`

### OrbStack Configuration (macOS)
OrbStack provides Docker-compatible API. No special config needed:

```bash
# Verify OrbStack is running
docker version
# Should show: Server: Docker Engine - Community, API Version: 1.54

# Test Testcontainers connection
docker run --rm testcontainers/ryuk:0.12.0
```

### Testcontainers Version
This project uses **Testcontainers 1.21.4** (configured in root `pom.xml`):
- Includes `docker-java 3.4.2` — supports Docker API 1.40+
- Compatible with OrbStack / Docker 29.x

### Troubleshooting
| Issue | Solution |
|-------|----------|
| `Could not find a valid Docker environment` | Ensure OrbStack/Docker is running; check `docker ps` |
| `Ryuk container failed to start` | Increase Docker resources (memory ≥ 4GB) |
| `Connection refused to unix:///var/run/docker.sock` | OrbStack: Settings → General → "Expose Docker socket" |
| Tests timeout on CI | Set `TESTCONTAINERS_RYUK_DISABLED=true` and add cleanup |

---

## Out of Scope

The following are explicitly out of scope for the current domain model implementation:
- **Notification service enhancements** (beyond basic email/SMS templates)
- **Advanced fraud detection** (beyond basic idempotency)
- **Multi-region deployment** (infrastructure concerns, not domain model)
- **Audit logging** (separate compliance module)
- **Real-time analytics dashboards** (operational tooling)

---

## Further Notes

- The `ReservationExpiredEvent` is the key seam connecting Inventory → OrderService → Payment (indirectly). This is the highest-seeming seam for testing.
- Payment authorization captures the full order amount but only captures reserved stock — this prevents charging for backordered items.
- ShipmentItems are denormalized for performance; the primary source of truth remains OrderItem status.
- All event publishing uses RabbitMQ with publisher confirms; consumers rely on idempotency tables for safety.
- The `order-service` is responsible for cancelling orders when reservations expire, keeping the bounded context clean.

---

## Service Startup Order

Services must start in dependency order:

```
config-server (8888) 
    → eureka-server (8761) 
    → auth-server (9000) 
    → gateway (8080) 
    → product (8081) 
    → category (8082) 
    → order-service (8083) 
    → inventory-service (8084) 
    → payment-service (8085) 
    → notification-service (8086)
```

### Why This Order?
| Service | Depends On | Reason |
|---------|------------|--------|
| eureka-server | config-server | Fetches config from Config Server |
| auth-server | config-server, eureka | Config + service registration |
| gateway | config-server, eureka, auth-server | Routes need auth + discovery |
| business services | config-server, eureka | Config + registration + gateway routing |

### Docker Compose
Handles dependencies automatically via `depends_on` with health checks:

```bash
docker-compose up -d  # starts in correct order
```

### Local Development (Manual)
```bash
# Terminal 1: Config Server
mvn spring-boot:run -pl config-server

# Terminal 2: Eureka Server (wait for config-server health)
mvn spring-boot:run -pl eureka-server

# Terminal 3: Auth Server (wait for eureka health)
mvn spring-boot:run -pl auth-server

# Terminal 4: Gateway (wait for auth-server health)
mvn spring-boot:run -pl gateway

# Terminals 5-10: Business services (any order after gateway)
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl payment-service
mvn spring-boot:run -pl notification-service
```

### Verify Health Before Proceeding
```bash
# Check each service is UP before starting dependents
curl http://localhost:8888/actuator/health   # config-server
curl http://localhost:8761/actuator/health   # eureka-server
curl http://localhost:9000/actuator/health   # auth-server
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # product
curl http://localhost:8082/actuator/health   # category
curl http://localhost:8083/actuator/health   # order-service
curl http://localhost:8084/actuator/health   # inventory-service
curl http://localhost:8085/actuator/health   # payment-service
curl http://localhost:8086/actuator/health   # notification-service
```

### Common Startup Issues
| Symptom | Cause | Fix |
|---------|-------|-----|
| `Connection refused` to Config Server | Started eureka before config | Start config-server first |
| `No instances available` in Eureka | Service not registered | Wait for health check to pass |
| `401 Unauthorized` at Gateway | Auth server not ready | Wait for auth-server health |
| Tests fail with `Could not resolve placeholder` | Config not loaded | Ensure bootstrap.yml active |

## License
MIT
