# 06a — Payment Service: Module Scaffold + Build/CI

**What to build:** New `payment-service` module: Maven module, Spring Boot 3.3, dependencies (WebFlux, R2DBC, Security), Dockerfile, CI pipeline, health/actuator endpoints. Empty domain package structure.

**Blocked by:** None

**Status:** resolved

- [x] Create `payment-service` directory with `pom.xml` inheriting from root parent
- [x] Add dependencies: `spring-boot-starter-webflux`, `spring-boot-starter-data-r2dbc`, `spring-boot-starter-security`, `r2dbc-postgresql`, `spring-boot-starter-actuator`, `spring-boot-starter-validation`
- [x] Configure `application.yml`: server.port=8086, R2DBC PostgreSQL, Spring Security (OAuth2 resource server)
- [x] Create `PaymentServiceApplication` main class
- [x] Add `Dockerfile` (multi-stage: build + runtime)
- [x] Add GitHub Actions workflow: build → test → docker build → security scan
- [x] Add health endpoints: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- [x] Create package structure: `com.example.payment.{domain,service,controller,config,event,repository}`
- [x] Verify: `mvn clean install -pl payment-service` passes; Docker image builds

## Notes
- Debezium connector was superseded by RabbitMQ direct publishing (see ticket 02b)
- Implementation includes full reactive domain model, not just empty package structure
- Added processed_events table for idempotent event consumption
- Added publisher confirms with CorrelationData for reliable RabbitMQ publishing

## Answer
Implemented payment-service scaffold with WebFlux + R2DBC. All requirements met except Debezium (superseded by RabbitMQ direct publishing per ticket 02b). Build passes, tests pass.
