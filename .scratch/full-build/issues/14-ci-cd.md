# 14 — CI/CD Pipeline (GitHub Actions)

**What to build:** GitHub Actions workflow running unit, integration, contract, and E2E tests.

**Blocked by:** 12-contract-tests, 13-e2e-tests

**Status:** ready-for-agent

- [ ] `.github/workflows/ci.yml` with jobs: test, contract, e2e
- [ ] Test job: matrix over 9 services → `mvn test` (unit) → `mvn verify` (integration, Testcontainers)
- [ ] Contract job: `mvn pact:verify -pl gateway`
- [ ] E2E job: starts docker-compose.test.yml → `npx playwright test`
- [ ] Artifacts: JaCoCo reports, Pact contracts, Playwright HTML report
- [ ] Caching: Maven dependencies, node_modules, Docker layers
- [ ] Runs on push to main, PRs
- [ ] Security scan (dependency-check) as separate job