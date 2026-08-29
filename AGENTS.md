# AGENTS.md - Spring Cloud Microservices Platform

## Project Overview
Multi-module Maven project with 11 services:
- `config-server` - Spring Cloud Config Server (Git backend)
- `eureka-server` - Service discovery (Netflix Eureka)
- `gateway` - Spring Cloud Gateway (routes + OAuth2 resource server)
- `product` - Product domain service (WebFlux + JPA)
- `category` - Category domain service (WebMVC + JPA)
- `auth-server` - Spring Authorization Server (OAuth2/JWT)
- `order-service` - Order domain service (WebFlux + R2DBC)
- `inventory-service` - Inventory domain service (WebFlux + R2DBC)
- `notification-service` - Notification service (WebFlux + JPA)
- `system-test` - System-level integration tests (Testcontainers)

## Build & Test Commands
```bash
# Build all modules (from root)
mvn clean install -DskipTests

# Run tests for specific module
mvn test -pl product

# Run integration tests (requires Testcontainers + Docker/OrbStack)
mvn verify -Dskip.unit.tests=true

# Build Docker images (requires Docker daemon)
mvn spring-boot:build-image -Pdocker

# Code coverage report
mvn jacoco:report
```

## Module Commands
```bash
# Run single service locally
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl config-server
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl auth-server
mvn spring-boot:run -pl order-service
mvn spring-boot:run -pl inventory-service
mvn spring-boot:run -pl notification-service
```

## Key Conventions
- **Java 21**, **Spring Boot 3.3.5**, **Spring Cloud 2023.0.4**
- Root POM manages all versions via `dependencyManagement`
- Each module has its own `pom.xml` inheriting from root
- No `relativePath` in child POMs (parent resolved from repo)

## Service Ports (default)
| Service | Port |
|---------|------|
| config-server | 8888 |
| eureka-server | 8761 |
| gateway | 8080 |
| product | 8081 |
| category | 8082 |
| auth-server | 9000 |
| order-service | 8083 |
| inventory-service | 8084 |
| notification-service | 8085 |

## Configuration
- Local config: `config-server/src/main/resources/config/` (Git repo)
- Each service: `bootstrap.yml` for Config Client, `application.yml` for local overrides
- Profiles: `dev`, `docker`, `prod`

## Testing
- Unit: `*Test.java` (Surefire)
- Integration: `*IntegrationTest.java` (Failsafe + Testcontainers)
- System: `system-test` module (Testcontainers + PostgreSQL)
- Contract: Pact (TODO)

## CI Pipeline (GitHub Actions)
`.github/workflows/ci.yml` runs: `validate -> test -> build -> docker -> security-scan`

## Common Gotchas
1. **Start order matters**: Config Server → Eureka → Auth Server → Gateway → Services
2. **Gateway routes** use `lb://service-name` (service discovery), not localhost
3. **OAuth2 tokens** issued by auth-server (`http://localhost:9000/oauth2/token`)
4. **OpenAPI docs**: `/swagger-ui.html` on each service, `/v3/api-docs` for JSON
5. **Actuator endpoints**: `/actuator/health`, `/actuator/prometheus`, `/actuator/info`
6. **Testcontainers** requires Docker/OrbStack running; uses `docker-java` 3.4+ for Docker API 1.40+

## Development Workflow
1. Modify code in module
2. `mvn test -pl <module>` - run unit tests
3. `mvn verify -pl <module>` - run integration tests (Testcontainers)
4. `mvn clean install -DskipTests` - build all
5. `docker-compose up -d` - run full stack locally

# Unit Testing Rules

## Guidelines
- Follow the **AAA pattern**: Arrange, Act, Assert.
- Use descriptive names like `it('should return X when Y', ...)`.
- Mock all external API calls and database connections.
- Do not test private functions directly.

## Workflow
1. Analyze the target function inputs and outputs.
2. Identify happy path and edge-case scenarios.
3. Write clean, isolated test code.