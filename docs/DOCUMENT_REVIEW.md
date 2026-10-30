# Documentation Review — Spring Cloud Microservices Platform

**Date:** 2026-10-28
**Scope:** All Markdown documentation at repo root + `docs/`, cross-checked against `.github/workflows/ci.yml`, the ADR set, and `AGENTS.md` conventions.
**Outcome:** The documentation is comprehensive and well cross-referenced, but contains multiple inconsistencies, outdated claims, and broken references that should be corrected.

---

## 1. Documentation Inventory

### Root level
| File | Role |
|------|------|
| `README.md` | Main project overview, getting started, service table, CI/CD summary |
| `CONTEXT.md` | Project context, service/tech matrix, ADR index, known gaps, quick commands |
| `AGENTS.md` | Agent-facing onboarding: build/test commands, ports, gotchas, git workflow, Lombok removal policy |
| `VALIDATION.md` | Auto-generated Jakarta Validation rules per DTO (CI job `validation-md-generation`) |
| `glossary.md` | Ubiquitous language: domain terms, statuses, API conventions, event naming, ports |
| `DATA_SEEDING_PLAN.md` | Plan for `seed-data` profile CommandLineRunner initializers |

### `docs/`
| File | Role |
|------|------|
| `architecture.md` | Mermaid system diagram, request flow, service tech matrix, DB-per-service, communication patterns |
| `architecture/README.md` | Architecture docs index |
| `architecture/event-driven-architecture.md` | Unified outbox, idempotent processing, saga orchestration, listener abstraction, config |
| `architecture/architecture-improvements-summary.md` | 7 architecture tickets (all marked complete) with test counts |
| `adr/001-*.md` … `adr/013-*.md` | 13 Architecture Decision Records (all Accepted) |
| `pact-cdc-workflow.md` | Pact consumer-driven contract workflow + troubleshooting + CI example |
| `specs/pact-contract-testing-implementation.md` | Pact implementation spec |
| `config-centralization.md` | Config Server native-profile migration guide |
| `K8S_SETUP.md` | OrbStack/Kubernetes deployment guide (Splunk OTel Collector + services) |
| `BUILD_GUIDE.md` | Build/dev workflow, Dockerfiles, Makefile targets, code style, known issues |
| `load-tests.md` | Load test scenarios, CI nightly integration, metrics |
| `SPLUNK_OBSERVABILITY.md` | Splunk Observability setup |
| `OTEL_OPERATOR_TROUBLESHOOTING.md` | OTel operator troubleshooting |
| `TEST_INFRA_NOTES.md` | Testcontainers "Two Databases" conflict notes |
| `FRONTEND_IMPROVEMENTS.md` | Frontend improvement notes |
| `DECISIONS_PAYMENT.md` | Payment authorize/capture/refund decisions |
| `agents/issue-tracker.md` | Local markdown issue tracker conventions (`.scratch/`) |
| `agents/triage-labels.md` | Triage role labels |
| `agents/domain.md` | Domain doc conventions (`CONTEXT.md` + ADRs) |
| `superpowers/plans/2026-09-19-unify-outbox-publisher.md` | Agentic implementation plan for unifying the outbox publisher |

**Referenced but not present:** `docs/domain-model.md` (referenced by several docs; `glossary.md` exists instead), `.scratch/` directory (referenced by AGENTS.md, docs/agents/issue-tracker.md, ADR 011, ADR 013, and the superpowers plan — see §4).

---

## 2. Strengths

- **Breadth:** onboarding (README/AGENTS.md/CONTEXT.md/BUILD_GUIDE.md), architecture (Mermaid diagrams + 13 ADRs), operations (K8s, config centralization, observability), testing (Pact CDC workflow, load tests, TEST_INFRA_NOTES), and agent conventions (issue tracker, triage labels, domain docs).
- **Transparency about known gaps:** `CONTEXT.md` §Known Gaps lists outdated config-server JPA/H2 config, unfinished R2DBC migrations, missing SonarQube quality gate thresholds, and pre-existing OpenAPI constraint issues.
- **Traceability:** ADRs are indexed in `CONTEXT.md` and cross-referenced from multiple docs; CI jobs (`validation-md-generation`, `pact-verification`, `sonarqube`) correspond to documented artifacts.
- **Consistency where it matters:** service ports, frameworks, and DB ports agree across `CONTEXT.md`, `README.md`, `docs/architecture.md`, `glossary.md`, and `K8S_SETUP.md`.

---

## 3. Discrepancies & Outdated Information (priority fixes)

### 3.1 Service count is inconsistent across docs
| Source | Claim |
|--------|-------|
| `docs/adr/001-service-decomposition.md:10` | **9 services** (notification excluded) |
| `AGENTS.md:4` | **11 services** (lists 10 + `system-test`) |
| `CONTEXT.md:8-20` | **10 services** in the table |
| `docs/K8S_SETUP.md:99-100` | **7 Deployments** (config-server, eureka-server, gateway, product, category, auth-server, order-service) — **missing inventory-service, payment-service, notification-service** |

### 3.2 README misstates Product's database
`README.md:24` (Core Features): *"Product Management — CRUD operations with **PostgreSQL** persistence"*. Product uses **MongoDB** (`CONTEXT.md:14`, `docs/architecture.md:39`, `README.md:47` all say MongoDB).

### 3.3 CI pipeline documentation is outdated / incomplete
`README.md:488-494`, `AGENTS.md:82`, and `docs/BUILD_GUIDE.md:309-317` all describe a 5-stage pipeline: `validate → test → build → docker → security-scan`.

Actual `.github/workflows/ci.yml` has **11 jobs**: `validate`, `test`, `verify`, `openapi-constraint-check`, `validation-md-generation`, `build`, `fullstack-verification`, `docker`, `sonarqube`, `pact-verification`, `security-scan`.

Docs omit from CI description: Pact contract verification, SonarQube analysis, OpenAPI constraint check, VALIDATION.md generation, and fullstack verification.

### 3.4 Lombok contradiction
- `AGENTS.md:119-127` — **Lombok Removal Policy**: "Lombok is being phased out... Do not add new Lombok annotations... Remove `lombok` dependency and annotation processor."
- `docs/BUILD_GUIDE.md:272` — Code Style: *"Use Lombok for boilerplate reduction (`@Getter`, `@Setter`, @Constructor)"* and *"Use MapStruct for DTO/entity mapping"*. Contradicts the removal policy.
- `docs/pact-cdc-workflow.md:207-209` — Troubleshooting references `maven-compiler-plugin → annotationProcessorPaths` including `org.projectlombok:lombok` and `org.mapstruct:mapstruct-processor`.

### 3.5 `.scratch/` directory does not exist but is referenced everywhere
- `AGENTS.md:102` — Ticket Tracking Rule references `.scratch/*/issues/*.md`.
- `docs/agents/issue-tracker.md:3` — "Issues and specs ... live as markdown files in `.scratch/`."
- `docs/adr/013-ci-pipeline-fixes.md:118-119` — references `.scratch/ci-pipeline-fix/issues/01-...` and `06-...`.
- `docs/adr/011-order-service-r2dbc-migration.md:71` — references `.scratch/order-service-r2dbc-jpa/issues/01-09`.
- `docs/superpowers/plans/2026-09-19-unify-outbox-publisher.md:11` — Spec: `.scratch/architecture/issues/01-unify-outbox-publisher.md`.

`glob .scratch/**/*` returned **no files**. The directory is absent or empty, so the ticket-tracking workflow cannot currently be followed.

### 3.6 Pact CDC documentation was outdated (now fixed)
`docs/pact-cdc-workflow.md` claimed only `ProductPactProviderTest` used `@PactFolder` and that provider tests for Order/Category/Payment/Inventory needed migration to the deprecated `ProviderInfo` API. In reality, **all 5 provider tests already use `@PactFolder`**, and `grep ProviderInfo` returns zero matches. The product→category consumer pact also already exists (`ProductCategoryConsumerPactTest.java`). The documentation has been updated to reflect the actual state, and the full cycle (26 consumer + 26 provider tests) was verified passing.

### 3.7 Reservation TTL inconsistency
- `README.md:96` (Technical Clarifications): *"Reservation TTL: Fixed **15 minutes**"* (also echoed in CONTEXT.md domain model).
- `docs/event-driven-architecture.md:116` — `ReservationExpiryScheduler` detects reservations older than **30 minutes**; config block at `:315` shows `saga.reservation-ttl: 30min`.

### 3.8 Test-count conflict
- `README.md:512-527` testing table: common 7, order-service 24, inventory-service 14, notification-service 6, architecture-tests 5, total 96.
- `docs/architecture-improvements-summary.md:194-205` results: common 14, order-service 52, inventory-service 13, notification-service 5, architecture-tests 5.

These cannot both be current; one source is stale.

### 3.9 Broken ADR references
`docs/event-driven-architecture.md:329-334` References section lists:
- `ADR 001: Outbox Pattern` → `../adr/001-outbox-pattern.md` (does not exist; 001 is `001-service-decomposition.md`)
- `ADR 002: Saga Orchestration` → `../adr/002-saga-orchestration.md` (does not exist; 002 is `002-database-strategy.md`)
- `ADR 003: Common Module Strategy` → `../adr/003-common-module.md` (does not exist; 003 is `003-cross-service-consistency.md`)

### 3.10 Missing referenced file
`docs/architecture-improvements-summary.md:214` references `../domain-model.md` — not present. `glossary.md` is the closest equivalent.

### 3.11 Event exchange naming mismatch
- `docs/glossary.md:55` — RabbitMQ uses topic exchange `ecommerce.events` with routing key `{aggregate}.{action}`.
- `docs/architecture.md:89-94` — services publish to per-service exchanges: `product.events`, `category.events`, `order.events`, `inventory.events`, `payment.events`, `notification.events`.
- `docs/event-driven-architecture.md:118` — routing key logic `aggregateType.toLowerCase() + "." + eventType.toLowerCase()`.

Unclear whether there is one exchange or six; docs disagree.

### 3.12 Integration tests do not run on PRs
`.github/workflows/ci.yml:77` — the `verify` job (integration/fuzz tests via Testcontainers) has `if: github.event_name == 'schedule'`, so it runs **nightly only**, not on push/PR. Docs (`AGENTS.md:26`, `README.md:229`) imply integration tests run in CI generally.

### 3.13 K8s setup covers only 7 of 10 services
`docs/K8S_SETUP.md:99` documents 7 Deployments/Services; inventory-service, payment-service, notification-service are absent. Also, `helmfile.yaml` (root), `helm template ./k8s/helm/spring-cloud-project` (README), and `kubectl apply -f k8s/deployments.yaml` (K8S_SETUP.md) are three deployment approaches with no explanation of which is canonical.

### 3.14 CONTEXT.md known gaps (acknowledged, still open)
- `CONTEXT.md:85` — `config-server/order-service.yml` still has JPA/H2 config (outdated).
- `CONTEXT.md:86` — Category, Inventory, Notification, Auth Server still use JPA (not migrated to R2DBC).
- `CONTEXT.md:87` — SonarQube quality gate thresholds not defined (yet `.github/workflows/ci.yml:425-477` runs a SonarQube job).
- `CONTEXT.md:88` — OpenAPI constraint verification job has pre-existing issues.

---

## 4. Missing Documentation / Gaps

| Gap | Notes |
|-----|-------|
| `CONTRIBUTING.md` | No contributing guide despite documented agent conventions (conventional commits, PR flow, ticket tracking). |
| `SECURITY.md` | No security policy; OAuth2 client secrets appear in README examples (`client_secret=secret` at `README.md:330`). |
| Release / versioning doc | `BUILD_GUIDE.md:319-325` has a release bullet but no full release/changelog process. |
| Frontend integration doc | `frontend/README.md` exists; no doc on how frontend integrates with the gateway/auth flows. |
| Incident runbook | No on-call runbook (circuit-breaker tripping, RabbitMQ DLX replay, reservation expiry remediation). |
| Pact broker (production) | `pact-cdc-workflow.md` covers local Docker broker only; no production broker setup/promotion flow. |
| `.scratch/` issue tracker | Directory missing; all ticket references are dangling. |

---

## 5. Contradictions Summary (quick index)

| # | Doc A | Doc B | Conflict |
|---|-------|-------|----------|
| 1 | `docs/adr/001-service-decomposition.md` | `AGENTS.md` / `CONTEXT.md` | 9 vs 11 vs 10 services |
| 2 | `README.md:24` | `CONTEXT.md` / `architecture.md` | Product DB: PostgreSQL vs MongoDB |
| 3 | `README/AGENTS/BUILD_GUIDE` CI summary | `.github/workflows/ci.yml` | 5 stages vs 11 jobs |
| 4 | `AGENTS.md` Lombok removal | `BUILD_GUIDE.md:272`, `pact-cdc-workflow.md:207` | Lombok banned vs Lombok encouraged/configured |
| 5 | `README.md:96` | `docs/event-driven-architecture.md:116,315` | TTL 15 min vs 30 min |
| 6 | `README.md:512-527` | `docs/architecture-improvements-summary.md:194-205` | Test counts per module differ |
| 7 | `docs/event-driven-architecture.md:329-334` | `docs/adr/` contents | Referenced ADRs 001-003 don't exist with those names |
| 8 | `docs/glossary.md:55` | `docs/architecture.md:89-94` | One `ecommerce.events` exchange vs six per-service exchanges |

---

## 6. Recommended Actions

1. **Create `.scratch/` structure** (per `docs/agents/issue-tracker.md`) and port the dangling references from ADR 011, ADR 013, AGENTS.md, and the superpowers plan into tracked tickets. Then tick checkboxes as work completes.
2. **Reconcile service count** — update ADR 001, AGENTS.md, CONTEXT.md, and K8S_SETUP.md to agree (actual: 10 deployable services + system-test).
3. **Fix README Product DB claim** (`README.md:24`) → MongoDB.
4. **Update CI documentation** — README, AGENTS.md, BUILD_GUIDE.md CI sections to reflect all 11 ci.yml jobs (especially Pact, SonarQube, OpenAPI check, VALIDATION.md generation).
5. **Resolve Lombok contradiction** — align BUILD_GUIDE.md code style and pact-cdc-workflow troubleshooting with the AGENTS.md Lombok Removal Policy (or formally reverse the policy).
6. **Fix Reservation TTL** — pick 15 min or 30 min and update README, event-driven-architecture.md config block, and any code constants consistently.
7. **Fix broken ADR references** in `docs/event-driven-architecture.md:329-334` and `domain-model.md` reference in `architecture-improvements-summary.md:214`.
8. **Clarify event exchange topology** — one exchange vs per-service exchanges; update glossary or architecture.md.
9. **Decide integration-tests-on-PR** — either change ci.yml `verify` job condition or update docs to state integration tests are nightly-only.
10. **Complete K8s coverage** — add inventory-service, payment-service, notification-service to K8S_SETUP.md (or document they are intentionally out of scope), and reconcile the three deployment approaches (helmfile / helm template / raw yaml).
11. **Close CONTEXT.md known gaps** — remove outdated JPA/H2 config from config-server/order-service.yml, define SonarQube quality gate thresholds, triage the OpenAPI constraint issue.
12. **Add missing docs** — CONTRIBUTING.md, SECURITY.md, incident runbook, production Pact broker guide.

---

## 7. Items Worth Mentioning (marked in `./docs`)

This review file itself (`docs/DOCUMENT_REVIEW.md`) is the record of items worth mentioning. The actionable items above (§6) should be converted to tickets under `.scratch/` once that directory exists, per the repo's ticket-tracking rule (`AGENTS.md:101-102`).

---

## 8. Fixes Applied (this session)

| # | Item | File(s) | Change |
|---|------|---------|--------|
| 1 | Product DB claim | `README.md` | PostgreSQL → MongoDB |
| 2 | CI pipeline summary | `README.md`, `AGENTS.md` | 5 stages → 11 ci.yml jobs (verify nightly-only, Pact, SonarQube, OpenAPI check, VALIDATION.md gen, fullstack-verification) |
| 3 | Service count | `AGENTS.md` | "11 services" → "10 services plus system-test module" |
| 4 | `.scratch/` location note | `AGENTS.md` | Added note that `.scratch/` lives in main branch worktree, not this feature worktree |
| 5 | Reservation TTL | `docs/architecture/event-driven-architecture.md` | Scheduler + `saga.reservation-ttl`: 30min → 15min (matches domain model) |
| 6 | Broken ADR refs | `docs/architecture/event-driven-architecture.md` | Removed non-existent ADR 001/002/003; pointed to ADR 006 Event Backbone + ADR 001 Service Decomposition; `domain-model.md` → `glossary.md` |
| 7 | Broken doc ref | `docs/architecture/architecture-improvements-summary.md` | `../domain-model.md` → `../../glossary.md` |
| 8 | Event exchange topology | `glossary.md` | Single `ecommerce.events` exchange → per-service exchanges (`product.exchange`, `order.exchange`, `payment.exchange`, `inventory.exchange`, `ecommerce.exchange`) |
| 9 | K8s service coverage | `docs/K8S_SETUP.md` | 7 → 10 Deployments/Services; added inventory-service (8084), payment-service (8086), notification-service (8087) to ports table, port-forwards, health checks, and ImagePullBackOff grep |
| 10 | Test-count conflict | `README.md` | Removed dubious "Total 96"; added note referencing improvements-summary baseline (common 14, order-service 52, inventory-service 13, notification-service 5, architecture-tests 5) |
| 11 | Lombok contradiction | `docs/BUILD_GUIDE.md` | Code style: "Use Lombok" → Lombok phased out, no new annotations, prefer Records/explicit constructors |
| 12 | Missing docs — CONTRIBUTING.md | `CONTRIBUTING.md` | Created root-level contributing guide (conventional commits, git workflow, build/test, Lombok policy, testing rules, ticket tracking) |
| 13 | Missing docs — SECURITY.md | `SECURITY.md` | Created root-level security policy (vulnerability reporting, OAuth2 secrets, JWT keys, secret management per layer) |
| 14 | Missing docs — incident runbook | `docs/INCIDENT_RUNBOOK.md` | Created runbook: circuit breakers, RabbitMQ DLX replay, reservation expiry remediation, startup order, Testcontainers issues |
| 15 | Missing docs — production Pact broker guide | `docs/pact-broker-production.md` | Created production Pact Broker guide (setup, CI secrets, publish/verify flow, `@PactBroker`, can-i-deploy, migration from `@PactFolder`) |
| 16 | Pact CDC docs outdated | `docs/pact-cdc-workflow.md` | Updated to reflect reality: all 5 providers use `@PactFolder`, product→category consumer pact exists, all tests pass. Updated contract table, step commands, verification checklist, CI example, and related files. |
| 17 | `.scratch/` created | `.scratch/` | Created issue-tracker structure per `docs/agents/issue-tracker.md` with specs + tickets for documentation-cleanup, context-known-gaps, and pact-provider-migration efforts. Four documentation tickets + five Pact tickets resolved. |
| 18 | K8s deployment reconciliation | `docs/K8S_SETUP.md` | Helm chart via `helmfile.yaml` declared canonical; `k8s/deployments.yaml` kept as lightweight raw-YAML alternative. |
| 19 | SonarQube quality gate thresholds | `sonar-project.properties` | Added coverage ≥ 80%, bugs/vulnerabilities/hotspots/smells/duplication (new) = 0 thresholds. |
| 20 | CONTEXT.md known gaps — JPA/H2 config | `CONTEXT.md` | Marked resolved — order-service.yml already uses R2DBC, no JPA/H2 config present. |
| 21 | CONTEXT.md known gaps — R2DBC migration | `CONTEXT.md` | Decision recorded: keep JPA locally for Category/Inventory/Notification/Auth Server (self-contained JPA, per ADR 011). |
| 22 | OpenAPI constraint verification | `ci/validation/.../OpenApiConstraintVerifier.java`, `common-dto/.../OrderDto.java` | Fixed verifier to not expect default-value constraints springdoc doesn't emit (@Size minLength:0, @DecimalMin exclusiveMinimum:false, @Min minimum:0). Added `@NotNull` to `OrderDto.items` to match `@Schema(requiredMode = REQUIRED)`. |

### Still open (require code changes or decisions)


---
*End of review.*