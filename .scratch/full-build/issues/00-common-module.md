# 00 — Common Module

**What to build:** Shared library (`org.example:common`) with event classes, DTOs, idempotency utilities, security config, and exception handling.

**Blocked by:** None — can start immediately.

**Status:** ready-for-agent

- [ ] Event classes: `OrderEvent`, `InventoryEvent`, `PaymentEvent`, `ProductEvent` with `eventId`, `correlationId`, `causationId`
- [ ] `ProcessedEvent` entity + `ProcessedEventRepository` for idempotency
- [ ] `IdempotentEventListener` base class (checks/inserts `processed_events`)
- [ ] DTOs: `AuthorizeRequest`, `CaptureRequest`, `RefundRequest`, `ReservationRequest`
- [ ] Security: `HeaderAuthenticationFilter` reading `X-User-*` headers, `ServiceTokenFilter` for `X-Service-Token`
- [ ] Exception handling: RFC 7807 Problem Details (`application/problem+json`)
- [ ] MapStruct mappers for common DTO ↔ Entity conversions
- [ ] Lombok removal: explicit constructors, getters, builders (per policy)
- [ ] Unit tests for all utilities
- [ ] Published as Maven artifact to local repo