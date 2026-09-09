# 13 — E2E Tests (Playwright)

**What to build:** Playwright test suite covering critical user journeys against docker-compose stack.

**Blocked by:** 11-docker-compose, 12-contract-tests

**Status:** ready-for-agent

- [ ] Playwright config with baseURL `http://localhost:8080` (gateway)
- [ ] Test: Browse products → view details
- [ ] Test: Create order with multiple items → reserve inventory → authorize payment → confirm order
- [ ] Test: Payment failure → order cancelled → inventory released
- [ ] Test: Reservation expiry (15 min) → order cancelled
- [ ] Test: Partial fulfillment (backorder) flow
- [ ] Auth flow: get token from auth-server → use in requests
- [ ] Runs against `docker-compose -f docker-compose.test.yml up -d`
- [ ] CI: `npx playwright test` in GitHub Actions