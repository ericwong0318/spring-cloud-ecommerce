# 04 — OpenAPI Constraint Verification CI Job

**What to build:** A CI job that starts all services via `docker-compose.test.yml`, fetches `/v3/api-docs` from each, and verifies every Jakarta Validation annotation on DTO fields has a corresponding OpenAPI schema constraint (`minLength`/`maxLength` for `@Size`, `pattern` for `@Pattern`, `minimum`/`maximum`/`exclusiveMinimum`/`exclusiveMaximum` for `@Min`/`@Max`/`@DecimalMin`/`@DecimalMax`, `required` for `@NotNull`/`@NotBlank`).

**Blocked by:** None — can start immediately (independent of other tickets).

**Status:** ready-for-agent

- [x] Write verification script (`OpenApiConstraintVerifier.java`) in `ci/validation/`
- [x] Script: reflectively scan `..dto`/`..request` classes for Jakarta Validation annotations
- [x] Script: fetch `/v3/api-docs` from each running service, map DTO → schema component
- [x] Script: diff annotation parameters vs. schema constraints; exit non-zero on mismatch
- [x] Add GitHub Actions job `openapi-constraint-check` in `.github/workflows/ci.yml`
- [x] Job runs after `docker-compose.test.yml` up, before `mvn verify`
- [x] Fail fast: any missing constraint mapping fails the build