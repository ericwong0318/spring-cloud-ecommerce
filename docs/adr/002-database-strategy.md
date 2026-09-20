# ADR-002: Database Strategy — PostgreSQL Per Service (MongoDB for Product)

## Status
Accepted (Amended)

## Context
Each service needs its own data store. Need to choose database technology and ownership model.

## Decision
**PostgreSQL per service** — each service owns its schema and data. No shared databases.
**Exception**: Product service uses MongoDB for flexible document storage.

| Service | Database | Access Pattern |
|---------|----------|----------------|
| product | MongoDB | Spring Data MongoDB Reactive |
| category | PostgreSQL | Spring Data JPA (blocking) |
| inventory | PostgreSQL | Spring Data R2DBC (reactive) |
| order | PostgreSQL | Spring Data R2DBC (reactive) |
| payment | PostgreSQL | Spring Data JPA (blocking) |
| auth-server | PostgreSQL | Spring Data JPA (blocking) |

Config server and Eureka use in-memory/embedded stores. Gateway is stateless.

## Rationale
- **Primary technology**: PostgreSQL handles both JPA and R2DBC, reducing operational burden
- **Product service exception**: MongoDB chosen for flexible product catalog with variable attributes, JSON-native queries
- **ACID per service**: Strong consistency within service boundaries
- **Testcontainers**: First-class PostgreSQL and MongoDB support for integration tests
- **Production parity**: Same DB locally, in CI, and in production
- **Team autonomy**: Each service team chooses schema evolution strategy

## Consequences
- **Positive**: Consistent tooling for PostgreSQL services, familiar SQL, mature ecosystem, supports both reactive and blocking
- **Negative**: Multiple database technologies (PostgreSQL + MongoDB), resource usage, schema duplication risk
- **Mitigation**: Docker Compose with shared PostgreSQL container (multiple databases) + MongoDB container, Flyway per PostgreSQL service

## Alternatives Considered
- **H2 for reactive services**: R2DBC H2 support is limited; not production-representative
- **MongoDB for all services**: Adds second technology broadly; PostgreSQL JSONB sufficient for most
- **Shared database**: Violates service autonomy, creates coupling
