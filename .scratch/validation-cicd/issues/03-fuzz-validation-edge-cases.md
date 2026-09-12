# 03 — Fuzz Tests for Validation Edge Cases

**What to build:** Structure-aware fuzz tests (JQF) that send malformed JSON to every `@PostMapping`/`@PutMapping` endpoint and assert RFC 7807 400 responses. Covers edge cases annotations miss: oversized payloads, regex false negatives/positives, numeric overflow, nested object validation.

**Blocked by:** 02 — Pact Provider Tests for Constraint Violations (Pact tests establish the 400 contract; fuzz tests explore the boundaries of that contract).

**Status:** ready-for-agent

- [ ] Add `jqf-spring` and `jqf-zest` to `dependencyManagement`
- [ ] Create `ValidationFuzzTest` base class in `system-test` module
- [ ] Per service: generate fuzz drivers for each controller endpoint
- [ ] Seed corpus from existing integration test payloads (`CategoryIntegrationTest`, `ProductIntegrationTest`, etc.)
- [ ] Run with `-Djqf.duration=600` (10 min per service) in `mvn verify -pl system-test -Pfuzz`
- [ ] CI: run fuzz profile nightly; fail build on any non-400 response or 5xx
- [ ] Document found edge cases in `VALIDATION.md` (see ticket 05)