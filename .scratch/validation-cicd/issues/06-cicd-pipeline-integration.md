# 06 — CI/CD Pipeline Integration for Validation Gates

**What to build:** Wire all validation layers into `.github/workflows/ci.yml` with correct phases, timeouts, and required status checks.

**Blocked by:** 02, 03, 04 — each layer must be implemented before it can be added to CI.

**Status:** ready-for-agent

- [ ] Phase 1 (unit): ArchUnit tests run in existing `test` job (`mvn test -pl architecture-tests`)
- [ ] Phase 2 (integration): Pact + fuzz tests run in existing `verify` job (`mvn verify -pl system-test -Pfuzz` + `mvn pact:verify`)
- [ ] Phase 3 (contract): OpenAPI constraint check runs in new job `openapi-constraint-check` after services start
- [ ] Phase 4 (docs): VALIDATION.md generation runs in new job `generate-validation-docs` (can run in parallel with Phase 3)
- [ ] Add all four jobs as required status checks on `main` and `develop` branch protection rules
- [ ] Configure timeouts: unit (5m), integration (30m), openapi (10m), docs (5m)
- [ ] Add failure annotations: link ArchUnit failures to rule name, Pact failures to interaction, fuzz failures to seed