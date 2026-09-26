# Spring Cloud Microservices Platform - Project Context

## Overview
Multi-module Maven project with 11 Spring Boot 3.3.x microservices using Spring Cloud 2023.0.x.

## Services

| Service | Port | Framework | Database | Port | Reactive | Circuit Breaker |
|---------|------|-----------|----------|------|----------|-----------------|
| Config Server | 8888 | Spring Cloud Config | - | - | - | - |
| Eureka Server | 8761 | Netflix Eureka | - | - | - | - |
| Gateway | 8080 | Spring Cloud Gateway (WebFlux) | Redis | 6379 | ✅ | Resilience4j |
| Auth Server | 9000 | Spring Authorization Server (WebMVC) | PostgreSQL | 5432 | ❌ | - |
| Product | 8081 | WebFlux | MongoDB | 27017 | ✅ | - |
| Category | 8082 | WebMVC | PostgreSQL | 5436 | ❌ | - |
| Order | 8083 | WebFlux | PostgreSQL (R2DBC) | 5437 | ✅ | Resilience4j |
| Inventory | 8084 | WebMVC | PostgreSQL | 5433 | ❌ | Resilience4j |
| Payment | 8086 | WebFlux | PostgreSQL (R2DBC) | 5438 | ✅ | Resilience4j |
| Notification | 8087 | WebMVC | PostgreSQL | 5434 | ❌ | - |

## Key Architecture Decisions (ADRs)

| ADR | Title | Status |
|-----|-------|--------|
| 001 | Service Decomposition | Accepted |
| 002 | Database Strategy | Accepted |
| 003 | Cross-Service Consistency | Accepted |
| 004 | API Style | Accepted |
| 005 | Auth Architecture | Accepted |
| 006 | Event Backbone | Accepted |
| 007 | Test Strategy | Accepted |
| 008 | Remove H2, Use PostgreSQL | Accepted |
| 009 | Config Server Native Profile | Accepted |
| 010 | Pact Consumer-Driven Contracts | Accepted |
| 011 | Order Service R2DBC Migration | Accepted |
| 012 | Payment Service SecurityConfig Conflict | Accepted |
| 013 | CI Pipeline Fixes | Accepted |

## Recent Completed Work (Sep 2024)

### Pact Contract Testing (Complete)
- All 5 consumer-provider pairs implemented and verified
- Modern `@PactFolder` approach (no broker required for local dev)
- Consumer tests generate pacts in `target/pacts/`
- Provider tests verify against local pacts
- `docs/pact-cdc-workflow.md` - complete workflow guide

### Config Centralization (Complete)
- Config Server uses native profile (local filesystem)
- All 11 services import config via `spring.config.import`
- Profile-specific configs: dev, docker, prod
- `docs/config-centralization.md` - migration guide

### CI Pipeline Fixes (Complete)
- spring-boot-maven-plugin moved to pluginManagement + explicit per-service declaration
- OWASP Dependency Check JAVA_HOME fixed (removed hardcoded path)
- All 11 service JARs properly repackaged

### Payment Service Pact Tests (Complete)
- Fixed validation exception handling (400 vs 500)
- 17/17 provider tests pass
- WebFlux validation with `@ControllerAdvice` + `WebExchangeBindException`

### Order Service R2DBC Migration (Complete)
- Removed JPA from common module
- Converted to pure reactive stack
- Reactive outbox pattern with `TransactionalOperator`

## Current Status

### Working (No Docker Required)
- ✅ All unit tests pass (`mvn test` per module)
- ✅ All compilation succeeds (`mvn clean install -DskipTests`)
- ✅ Pact consumer tests generate contracts
- ✅ Pact provider tests verify (Product, Payment, Inventory, Category, Order)
- ✅ Config Server serves all configs
- ✅ Helm chart renders correctly (`helm template`)

### Require Docker/OrbStack
- ⚠️ Integration tests (Testcontainers: PostgreSQL, MongoDB, RabbitMQ)
- ⚠️ Full docker-compose stack verification
- ⚠️ k8s dev overlay deployment

### Known Gaps
- config-server/order-service.yml still has JPA/H2 config (outdated)
- Category, Inventory, Notification, Auth Server still use JPA (not migrated to R2DBC)
- SonarQube quality gate thresholds not defined
- OpenAPI constraint verification job has pre-existing issues

## Documentation

| File | Purpose |
|------|---------|
| `docs/architecture.md` | System diagrams, request flows, tech matrix |
| `docs/pact-cdc-workflow.md` | Complete CDC workflow with troubleshooting |
| `docs/config-centralization.md` | Config Server migration guide |
| `docs/adr/` | Architecture Decision Records (13) |
| `BUILD_GUIDE.md` | Build and test commands |
| `K8S_SETUP.md` | Kubernetes deployment |
| `SPLUNK_OBSERVABILITY.md` | Observability setup |

## Quick Commands

```bash
# Build all
mvn clean install -DskipTests

# Run unit tests
mvn test -pl <module>

# Pact CDC workflow
mvn clean install -pl common -DskipTests
mvn test -pl order-service -Dtest=*ConsumerPactTest
mvn test -pl product -Dtest=ProductPactProviderTest

# Config Server
mvn spring-boot:run -pl config-server

# Helm template
helm template spring-cloud-ecommerce ./k8s/helm/spring-cloud-project
```
