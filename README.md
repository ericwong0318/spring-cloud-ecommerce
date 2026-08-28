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
- 📊 **Observability** — Actuator, Prometheus metrics, OpenAPI/Swagger
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
   │  │ Config Srv :8888 │  │ Eureka Srv :8761 │  │  Prometheus  │   │
   │  │ (Git backend)    │  │ (Service Reg.)   │  │  + Grafana   │   │
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
| **Asynchronous** | (Future) Kafka/RabbitMQ for event-driven flows |
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

### Service Dependencies & Start Order

```
config-server  →  eureka-server  →  auth-server  →  gateway  →  business services
    (8888)          (8761)           (9000)         (8080)    (8081/8082/8083)
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
| **Observability** | Spring Actuator, Micrometer, Prometheus |
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
```

### Local Development (without Docker)

```bash
# Start infrastructure first
docker-compose up -d postgres config-server eureka-server

# Then start services in order
mvn spring-boot:run -pl config-server
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl auth-server
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl order-service
```

### Build & Test

```bash
# Build all modules
mvn clean install -DskipTests

# Run unit tests
mvn test

# Run integration tests (requires Docker for Testcontainers)
mvn verify -Dskip.unit.tests=true

# Code coverage report
mvn jacoco:report
```

---

## API Documentation

Each service exposes OpenAPI/Swagger documentation:

| Service | Swagger UI | OpenAPI JSON |
|---------|-----------|--------------|
| Gateway | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| Product | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Category | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Order | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |
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
Each service has a multi-stage Dockerfile. Build all images:
```bash
mvn spring-boot:build-image -Pdocker
```

### Docker Compose
```bash
docker-compose up -d        # start
docker-compose down         # stop
docker-compose logs -f      # view logs
```

### Kubernetes (future)
Manifests will live under `k8s/` (TODO).

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
| config-server | 1 | 0 |
| eureka-server | 1 | 0 |
| gateway | 1 | 0 |
| product | 1 | 1 |
| category | 1 | 2 |
| order-service | 0 | 0 (TODO) |
| auth-server | 0 | 0 (TODO) |
| **Total** | **5** | **3** |

Test patterns:
- **Unit** — `@SpringBootTest` with `contextLoads()`
- **Integration** — RestAssured with random port + test security config
- **Database** — Testcontainers (PostgreSQL) for integration tests

---

## License
MIT
