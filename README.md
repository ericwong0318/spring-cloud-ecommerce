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
                  ┌────────────┘         │       └────────────┐
                  │   Auth Check         │                    │
                  ▼                      ▼                    ▼
      ┌──────────────────┐   ┌──────────────────┐  ┌──────────────────┐
      │ Auth Server :9000│   │ Product Svc :8081│  │ Category Svc:8082│
      │ (OAuth2/JWT)     │   │  (WebFlux + JPA) │  │  (WebMVC + JPA)  │
      └──────────────────┘   └────────┬─────────┘  └────────┬─────────┘
                                     │                     │
                                     │   ┌─────────────────┘
                                     │   │
                                     ▼   ▼
                          ┌────────────────────┐
                          │  Order Svc :8083   │
                          │ (WebFlux + R2DBC)  │
                          └────────┬───────────┘
                                   │ Reactive
                                   ▼
                          ┌────────────────────┐
                          │  PostgreSQL DBs    │
                          │  product_db,       │
                          │  category_db,      │
                          │  order_db,         │
                          │  oauth2_db         │
                          └────────────────────┘

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
1. Client → POST /api/orders (Bearer Token)
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
| **Asynchronous** | RabbitMQ for event-driven flows (order events, product events) |
| **Service Discovery** | Eureka + `@LoadBalanced` `RestClient` |
| **Configuration** | `bootstrap.yml` → Config Server → Git repo |
| **Security** | OAuth2 JWT validated at Gateway; propagated downstream |
| **Database per Service** | Each service owns its PostgreSQL schema (no shared DB) |

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
  | **notification-service** | 8085 | WebFlux + JPA | PostgreSQL (notification_db) | Email notifications via RabbitMQ |

### Service Dependencies & Start Order

```
config-server  →  eureka-server  →  auth-server  →  gateway  →  business services
     (8888)          (8761)           (9000)         (8080)    (8081/8082/8083/8084/8085)
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
curl http://localhost:8085/actuator/health   # notification
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
| Notification | http://localhost:8085/swagger-ui.html | http://localhost:8085/v3/api-docs |
| Auth | http://localhost:9000/swagger-ui.html | http://localhost:9000/v3/api-docs |

### Sample Product API
```bash
# Get all products (via Gateway)
curl http://localhost:8080/api/products

# Get product by ID
curl http://localhost:8080/api/products/1

# Create product (requires OAuth2 token)
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":999.99,"categoryId":1}'
```

---

## Security

### OAuth2 Token Flow

```
┌────────┐                                          ┌──────────────┐
│ Client │ ──── 1. POST /oauth2/token ────────────▶ │ Auth Server  │
│        │ ◀── 2. JWT (RS256) ───────────────────── │  :9000       │
│        │                                          └──────────────┘
│        │ ──── 3. GET /api/orders + Bearer JWT ───▶ ┌──────────────┐
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
4. **docker** — Build & push Docker images
5. **security-scan** — OWASP dependency check + Trivy

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
| notification-service | 7 | 0 |
| system-test | 0 | 4 (Testcontainers) |
| **Total** | **34** | **7** |

Test patterns:
- **Unit** — `@SpringBootTest` with mocked dependencies (Mockito)
- **Integration** — RestAssured with random port + test security config
- **Database** — Testcontainers (PostgreSQL) for integration tests
- **System** — Full stack E2E tests in `system-test` module

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
    → notification-service (8085)
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

# Terminals 5-9: Business services (any order after gateway)
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl inventory-service
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
curl http://localhost:8085/actuator/health   # notification-service
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
