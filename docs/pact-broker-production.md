# Production Pact Broker Guide

This guide covers running Pact consumer-driven contract testing against a production Pact Broker (self-hosted or Pactflow) in CI/CD, extending the local workflow in `docs/pact-cdc-workflow.md`.

## Prerequisites

- A Pact Broker instance (self-hosted `pactfoundation/pact-broker` or Pactflow)
- CI pipeline with access to the broker (`.github/workflows/ci.yml` `pact-verification` job)
- Consumer and provider Pact tests implemented in each service module

## Required CI secrets

| Secret | Description | Example |
|--------|-------------|---------|
| `PACT_BROKER_URL` | Pact Broker base URL | `https://pact-broker.company.com` or `https://yourorg.pactflow.io` |
| `PACT_BROKER_TOKEN` | Bearer token for broker authentication | Pactflow API token or broker personal access token |

Add these to GitHub repository secrets (Settings → Secrets and variables → Actions).

## Contract flow (production)

```
1. Consumer tests generate pacts        →  mvn test (consumer module)
2. Publish pacts to broker              →  mvn pact:publish
3. Provider verifies against broker     →  mvn test (provider module, @PactBroker)
4. Broker records verification result   →  pact broker web UI / API
5. Can-i-deploy check (optional)        →  pact-broker can-i-deploy
```

## Step 1: Generate consumer pacts

Run consumer tests to generate Pact JSON files. In CI:

```bash
mvn clean install -pl common -DskipTests
mvn test -B -pl order-service,notification-service,product \
  -Dtest='*PactConsumerTest' -DfailIfNoTests=false
```

This creates `.json` contract files in each consumer module's `target/pacts/`.

## Step 2: Publish pacts to the broker

```bash
mvn pact:publish \
  -Dpact.broker.url=${PACT_BROKER_URL} \
  -Dpact.broker.token=${PACT_BROKER_TOKEN} \
  -Dpact.consumer.version=$(git rev-parse --short HEAD)
```

Publish from each consumer module after its consumer tests run. The broker records the latest pact version per consumer.

## Step 3: Provider verification against the broker

Provider tests use `@PactBroker` instead of `@PactFolder` (which reads local files):

```java
@Provider("product-service")
@PactBroker(
    url = "${pactbroker.url}",
    token = "${pactbroker.token}"
)
class ProductPactProviderTest { ... }
```

Run in CI:

```bash
mvn test -B -pl order-service,payment-service,category,inventory-service,product \
  -Dtest='*PactProviderTest' -DfailIfNoTests=false
```

Each provider test downloads the latest pact from the broker for its consumer(s) and verifies the provider honors them. The broker records the verification result.

## Step 4: Can-i-deploy (optional)

Before deploying a consumer or provider, run a can-i-deploy check:

```bash
# Check if a consumer version can safely be deployed
pact-broker can-i-deploy \
  --broker-url ${PACT_BROKER_URL} \
  --broker-token ${PACT_BROKER_TOKEN} \
  --consumer product-service \
  --consumer-version $(git rev-parse --short HEAD) \
  --to-environment production
```

If the provider has verified the latest consumer pact, deployment is allowed. Otherwise, it fails and blocks deployment until contract verification passes.

## Migration from local `@PactFolder` to broker

`docs/pact-cdc-workflow.md` notes that only `ProductPactProviderTest` uses `@PactFolder` today; Order, Category, Payment, and Inventory provider tests still use the deprecated `ProviderInfo` API. The migration path is:

1. Implement missing consumer pacts (e.g. product → category, tracked in `.scratch/pact-provider-migration/`)
2. Migrate each provider test from `ProviderInfo` to `@PactBroker`
3. Enable provider tests in CI (remove `-DfailIfNoTests=false` masking)
4. Add `mvn pact:publish` step to the CI `pact-verification` job
5. Add can-i-deploy checks to deployment pipelines

## Related

- `docs/pact-cdc-workflow.md` — local Pact CDC workflow (file-based pacts)
- `.github/workflows/ci.yml` — `pact-verification` job
- Pact JVM docs: https://docs.pact.io/implementation_guides/jvm/
- Pact Broker docs: https://docs.pact.io/pact_broker/
