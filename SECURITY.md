# Security Policy

## Supported versions

The following versions are currently supported with security updates:

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅        |

## Reporting a vulnerability

If you discover a security vulnerability, please do **not** open a public GitHub issue. Instead, contact the maintainers directly (see repository contact information) with:

- A description of the vulnerability
- Steps to reproduce
- Affected services and versions
- Suggested remediation (if any)

We will acknowledge receipt, investigate, and coordinate a fix and disclosure timeline with you.

## OAuth2 client secrets

The README contains example OAuth2 client credentials for illustration only:

```
curl -X POST http://localhost:9000/oauth2/token \
  -d "grant_type=client_credentials&client_id=product-service&client_secret=secret&scope=read,write"
```

**Never commit real client secrets, passwords, or private keys to the repository.** Secrets are managed through:

- `.env` for local development (excluded from git via `.gitignore`)
- Kubernetes secrets / ConfigMap secrets in production
- Environment variables (`SPRING_*`, `POSTGRES_*`, `RABBITMQ_*`, `MAIL_*`) at runtime

## JWT signing keys

Auth Server (`auth-server`, port 9000) issues RS256 JWTs. Signing keys are configured via Spring Authorization Server properties and should be rotated periodically. The public JWKS endpoint is available at `http://auth-server:9000/oauth2/jwks` for token validation by the Gateway and resource services.

## Secret management

| Layer | Mechanism |
|-------|-----------|
| Local dev | `.env` + `docker-compose.yml` environment |
| Docker Compose | `.env` passed to service containers |
| Kubernetes | `k8s/helm/spring-cloud-project/templates/secret.yaml` + `envFrom secretRef` |
| CI | GitHub Actions secrets (e.g., `PACT_BROKER_TOKEN`, `SONAR_TOKEN`) |

## Dependency security

The CI pipeline runs an OWASP Dependency Check (`security-scan` job) on `payment-service` with `--failOnCVSS 7`. New dependencies should be reviewed for known vulnerabilities before being added to the root `pom.xml` `dependencyManagement`.

## Scope

This policy covers the platform services listed in `AGENTS.md`. Third-party services (Splunk Observability, Pact Broker, Docker Hub) are governed by their own security documentation.
