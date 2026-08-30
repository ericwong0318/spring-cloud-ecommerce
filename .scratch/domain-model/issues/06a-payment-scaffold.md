# 06a — Payment Service: Module Scaffold + Build/CI

**What to build:** New `payment-service` module: Maven module, Spring Boot 3.3, dependencies (WebFlux, R2DBC, Security, Debezium), Dockerfile, CI pipeline, health/actuator endpoints. Empty domain package structure.

**Blocked by:** 02 — Debezium CDC Setup for Event Publishing

**Status:** ready-for-agent

- [ ] Create `payment-service` directory with `pom.xml` inheriting from root parent
- [ ] Add dependencies: `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `spring-boot-starter-security`, `r2dbc-postgresql`, Debezium connector, `spring-boot-starter-actuator`, `spring-boot-starter-validation`
- [ ] Configure `application.yml`: server.port=8086, R2DBC PostgreSQL, Spring Security (OAuth2 resource server), Debezium outbox table
- [ ] Create `PaymentServiceApplication` main class
- [ ] Add `Dockerfile` (multi-stage: build + runtime)
- [ ] Add GitHub Actions workflow: build → test → docker build → security scan
- [ ] Add health endpoints: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- [ ] Create package structure: `com.example.payment.{domain,service,controller,config,event,repository}`
- [ ] Verify: `mvn clean install -pl payment-service` passes; Docker image builds