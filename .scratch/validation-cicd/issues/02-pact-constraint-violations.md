# 02 — Pact Provider Tests for Constraint Violation Responses

**What to build:** Pact provider verification tests for each service that assert HTTP 400 (not 500) with RFC 7807 `ProblemDetailResponse` when a request payload violates `@Size`, `@NotBlank`, `@Pattern`, `@Min`, `@Max`, `@DecimalMin`, `@DecimalMax`, or `@NotNull`.

**Blocked by:** 01 — ArchUnit Validation Architecture Tests (already complete — ArchUnit ensures endpoints have `@Valid` so Pact tests exercise real validation paths).

**Status:** ready-for-agent

- [x] Add Pact v4+ dependencies to `dependencyManagement` (provider:junit5, provider:spring, consumer:junit5)
- [x] Add Pact dependencies to `payment-service` and `category` modules
- [ ] For each service with REST endpoints: create a `@PactTestFor` provider test class
- [ ] Per endpoint: one interaction per constraint type (e.g., `name` field → `@NotBlank`, `@Size(max=255)`)
- [ ] Verify response: status 400, `application/problem+json`, `errors` map contains violating field
- [ ] Publish Pact contracts to broker (or local `target/pacts/`) in CI
- [ ] Add `mvn pact:verify` to CI `verify` phase for each module

## Status

ready-for-agent