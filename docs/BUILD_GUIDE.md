# Build and Development Guide

This guide covers building, testing, and running the Spring Cloud Microservices platform.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker & Docker Compose (for full stack)
- PostgreSQL 16+ (optional, for local dev)
- OrbStack (recommended for macOS) or Docker Desktop

## Quick Start (Local Docker)

### 1. Start Infrastructure

```bash
# Start database + core infrastructure
docker compose up -d postgres config-server eureka-server

# Verify they're healthy
docker compose logs -f postgres config-server eureka-server
```

### 2. Build Services

```bash
# Build all service images
docker compose build

# OR build specific services
docker compose build config-server eureka-server

# OR use Makefile
make build-all
```

### 3. Start All Services

```bash
docker compose up -d
```

### 4. Verify Health

```bash
curl http://localhost:8888/actuator/health   # config-server
curl http://localhost:8761/actuator/health   # eureka-server
curl http://localhost:9000/actuator/health   # auth-server
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # product
curl http://localhost:8082/actuator/health   # category
curl http://localhost:8083/actuator/health   # order
```

## Development Workflow

### Running Services Locally (without Docker)

```bash
# Start infrastructure first
docker compose up -d postgres config-server eureka-server

# Then run services in separate terminals
mvn spring-boot:run -pl config-server
mvn spring-boot:run -pl eureka-server
mvn spring-boot:run -pl auth-server
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl product
mvn spring-boot:run -pl category
mvn spring-boot:run -pl order-service
```

### Building Images

```bash
# Standard build (works on most Docker setups)
docker compose build

# With cache busting
docker compose build --no-cache

# Build specific service
docker compose build config-server

# Using Makefile
make build-all
make build config-server
```

### Testing

```bash
# Unit tests only
mvn test

# Unit + integration tests
mvn verify

# Integration tests only (requires Docker for Testcontainers)
mvn verify -Dskip.unit.tests=true

# Skip tests
mvn clean install -DskipTests
```

### Code Quality

```bash
# Checkstyle (if configured)
mvn checkstyle:check

# SpotBugs (if configured)
mvn spotbugs:check

# JaCoCo coverage report
mvn jacoco:report
# Then open target/site/jacoco/index.html
```

## Docker Build Details

Each service uses a multi-stage Dockerfile with OpenTelemetry Java agent instrumentation.

### Dockerfile Structure

```dockerfile
FROM eclipse-temurin:21-jre-alpine

ARG OTEL_AGENT_VERSION=2.8.0
RUN apk add --no-cache wget && \
    mkdir -p /app && \
    wget -q --timeout=30 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar -O /app/opentelemetry-javaagent.jar

WORKDIR /app
COPY target/*.jar app.jar
EXPOSE <service-port>

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
```

### Environment Variables for OTel

Set in `docker-compose.yml`:
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=docker
  - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
  - OTEL_EXPORTER_OTLP_PROTOCOL=grpc
  - OTEL_SERVICE_NAME=<service-name>
  - OTEL_RESOURCE_ATTRIBUTES=deployment.environment=local,service.name=<service-name>
  - OTEL_TRACES_EXPORTER=otlp
  - OTEL_METRICS_EXPORTER=otlp
  - OTEL_LOGS_EXPORTER=otlp
  - OTEL_PROPAGATORS=w3c,tracecontext
```

## Makefile Targets

```bash
# Build all images
make build-all

# Build specific service
make build <service-name>

# Run unit tests
make test

# Run integration tests
make verify

# Start stack
make up

# Stop stack
make down

# Follow logs
make logs

# Clean everything
make clean

# Show help
make help
```

## Kubernetes Build & Deploy

### Build Images for K8s

```bash
# Build all images
docker compose build

# Images are now in OrbStack's Docker daemon
# Load them into containerd (OrbStack's container runtime)
# No need to push to registry for local dev
```

### Deploy Services

```bash
# Create namespace with instrumentation label
kubectl create namespace ecommerce
kubectl label namespace ecommerce instrumentation.opentelemetry.io/inject-java=true --overwrite

# Apply deployments
kubectl apply -f k8s/deployments.yaml

# Services will use images from OrbStack's Docker daemon
```

## Troubleshooting Build Issues

### Maven Dependency Resolution Errors

If you see `401 Unauthorized` from `repo.spring.io`:

1. Maven Central is preferred in `pom.xml`
2. The `spring-releases` repo is kept for specific Spring dependencies
3. Run `mvn clean install -U` to force update snapshots

### Docker Build Failures

If `docker build` fails with "exit code 1" when downloading OTel agent:

1. Check network connectivity to `github.com`
2. The Dockerfile uses `wget --timeout=30` for reliability
3. For OrbStack: ensure Docker daemon is running (`orb docker`)

### Image Size Optimization

Current base image: `eclipse-temurin:21-jre-alpine` (~289MB)
- Multi-stage build keeps final image small (~120MB)
- No build tools in final runtime image
- OpenTelemetry agent adds ~15MB

## Service Start Order

For local development without Docker Compose:

1. `postgres` (database)
2. `config-server` (centralized configuration)
3. `eureka-server` (service discovery)
4. `auth-server` (OAuth2 tokens)
5. `gateway` (API routing)
6. Business services (product, category, order-service)

Health check dependencies are configured in Docker Compose to enforce this order.

## Configuration Sources

Each service loads configuration from:

1. **Application properties**: `src/main/resources/application.yml`
2. **Profile-specific**: `application-{profile}.yml` (e.g., `application.docker.yml`)
3. **Spring Cloud Config Server**: Git-backed (local: `config-server/src/main/resources/config/`)
4. **Environment variables**: Highest precedence
5. **Command-line args**: Override everything

### Local Dev vs Docker vs K8s Profiles

- `dev`: Local machine (H2 database, no Docker)
- `docker`: Docker Compose (PostgreSQL, service discovery via Docker DNS)
- `kubernetes`: K8s deployment (PostgreSQL via service DNS)

## Code Style

- Follow existing code style in each module
- Use Lombok for boilerplate reduction (`@Getter`, `@Setter`, `@Constructor`)
- Use MapStruct for DTO/entity mapping
- Services use constructor injection
- Controllers return `Mono<ResponseEntity<>>` or `Flux<>`
- Error handling via `@ControllerAdvice` (where implemented)

## API Documentation

Each service exposes OpenAPI 3.0 docs:

| Service | Swagger UI | OpenAPI JSON |
|---------|-----------|--------------|
| Gateway | `http://localhost:8080/swagger-ui.html` | `http://localhost:8080/v3/api-docs` |
| Product | `http://localhost:8081/swagger-ui.html` | `http://localhost:8081/v3/api-docs` |
| Category | `http://localhost:8082/swagger-ui.html` | `http://localhost:8082/v3/api-docs` |
| Order | `http://localhost:8083/swagger-ui.html` | `http://localhost:8083/v3/api-docs` |
| Auth | `http://localhost:9000/swagger-ui.html` | `http://localhost:9000/v3/api-docs` |

## Testing Strategy

### Unit Tests
- Located in `src/test/java`
- Use `@SpringBootTest` with `@MockBean` for dependencies
- Focus on business logic and validation
- Naming: `*Test.java`

### Integration Tests
- Use Testcontainers for real PostgreSQL
- Use RestAssured for HTTP API testing
- Located in `src/test/java` with `*IntegrationTest.java`
- Requires Docker daemon

### Testcontainers Configuration
- PostgreSQL container with Flyway migrations
- Random port assignment to avoid conflicts
- Automatically cleaned up after test

## CI/CD Pipeline

See `.github/workflows/ci.yml`:

1. **Validate**: `mvn validate -B`
2. **Test**: `mvn test -B`
3. **Build**: `mvn clean install -DskipTests -B`
4. **Docker**: Build & push images (requires secrets)
5. **Security Scan**: OWASP Dependency Check + Trivy

## Release Process

1. Update version in `pom.xml`
2. Tag release: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
3. Push tag: `git push origin vX.Y.Z`
4. GitHub Actions builds and publishes Docker images
5. Create GitHub Release with changelog

## Known Issues

### Order Service Delete Compilation

The `OrderController.deleteOrder()` method has a complex reactive return type that requires explicit casting to `ResponseEntity<Void>`. See the source for details.

### LSP Errors in system-test/pom.xml

These are stale Eclipse m2e cache errors. The Maven build works fine. Refresh the project in your IDE or run `mvn clean` to clear caches.

### PostgreSQL Version Compatibility

Tested with PostgreSQL 16. Older versions may require different JDBC driver versions.

## Performance Notes

### Startup Times
- Config Server: ~2s
- Eureka Server: ~1s
- Gateway: ~3s (due to route definitions)
- Product/Category/Auth: ~2s each
- Order Service: ~2s (R2DBC startup)

### Memory Usage (approx)
- Each service: 200-300MB RSS
- PostgreSQL: 150-250MB RSS
- OTel Agent: 50-100MB RSS

### Scaling Considerations
- Stateless services scale horizontally
- Use Kubernetes HPA based on CPU/memory
- Database connections scale with instance count
- Consider connection pooling limits (HikariCP/R2DBC)

## License

MIT License - see LICENSE file for details.
