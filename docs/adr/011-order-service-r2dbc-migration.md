# ADR 011: Order Service R2DBC Migration (Removed JPA from Common Module)

## Status
Accepted

## Context
Order Service was a hybrid JPA/R2DBC service using:
- R2DBC for reactive `OrderRepository` 
- JPA (via common module) for `OutboxEventRepository` and `ProcessedEventRepository`

This caused Spring context initialization failures in tests because:
- JPA entities (`OutboxEvent`, `ProcessedEvent`) in common module required `spring-boot-starter-data-jpa`
- JPA repositories in common module required `JpaRepository`
- Order Service test context couldn't exclude JPA without excluding it from common

## Decision
**Remove all JPA dependencies from the common module** and convert to pure R2DBC/reactive patterns:
1. Convert JPA entities → plain Java records
2. Convert JPA repositories → R2DBC repositories (interfaces only)
3. Replace `@Transactional` with `TransactionalOperator` (R2DBC)
4. Create reactive equivalents: `ReactiveOutboxEventPublisher`, `ReactiveIdempotentEventProcessor`

## Consequences

### Positive
- Pure reactive stack in Order Service (no JPA classpath pollution)
- Common module becomes framework-agnostic (no Spring Data JPA dependency)
- Test context starts cleanly without JPA auto-configuration conflicts
- Consistent R2DBC patterns across services (Payment, Order)

### Negative
- Other services using common module's JPA entities must migrate (Category, Inventory, Notification, Auth Server)
- More boilerplate for R2DBC repositories (no derived queries)
- Manual transaction management with `TransactionalOperator`

## Changes Made

### Common Module (`common/`)
- Removed `spring-boot-starter-data-jpa` dependency
- `OutboxEvent` → record with R2DBC `@Table` annotation
- `ProcessedEvent` → record with R2DBC `@Table` annotation
- `OutboxEventRepository` → `R2dbcOutboxEventRepository` (interface)
- `ProcessedEventRepository` → `R2dbcProcessedEventRepository` (interface)
- `OutboxEventPublisher` → `ReactiveOutboxEventPublisher` (uses `TransactionalOperator`)
- `IdempotentEventProcessor` → `ReactiveIdempotentEventProcessor` (uses `TransactionalOperator`)

### Order Service (`order-service/`)
- Removed `spring-boot-starter-data-jpa` and HikariCP dependencies
- Removed `@EnableJpaRepositories`, `@EntityScan` from application
- Updated `BaseIntegrationTest` to exclude JPA auto-configuration
- Uses common module's `ReactiveIdempotentEventProcessor` and `ReactiveOutboxEventPublisher`
- Added Pact provider test (`OrderPactProviderTest`)

## Test Verification
```bash
# Unit tests pass (52 tests)
mvn test -pl order-service

# Integration tests need Docker (Testcontainers PostgreSQL)
# mvn verify -pl order-service
```

## Migration Checklist for Other Services
- [ ] Category Service: Migrate to R2DBC or keep JPA (if keeping JPA, duplicate entities locally)
- [ ] Inventory Service: Migrate to R2DBC or keep JPA locally
- [ ] Notification Service: Migrate to R2DBC or keep JPA locally
- [ ] Auth Server: Migrate to R2DBC or keep JPA locally
- [ ] Payment Service: Already R2DBC ✅

## Related
- `.scratch/order-service-r2dbc-jpa/issues/01-09`
- `common/src/main/java/com/example/common/event/`
- `order-service/src/main/java/com/example/order/`
