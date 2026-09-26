# ADR 009: Config Server Native Profile (No External Git)

## Status
Accepted

## Context
The Config Server was originally configured to pull configuration from an external Git repository. This added operational complexity:
- Required separate Git repo management
- Needed credentials/secrets for Git access
- CI/CD pipeline had to clone repo
- Developer onboarding required Git access

## Decision
Use Spring Cloud Config Server **native profile** with local filesystem (`config-server/src/main/resources/config/`) as the configuration backend.

## Consequences

### Positive
- Zero external dependencies for configuration
- All config lives in monorepo (single source of truth)
- Simple developer workflow: `git clone` → run
- No secrets for Config Server access needed
- CI/CD: just build the Config Server JAR with config baked in
- Git history provides audit trail for config changes

### Negative
- Config changes require Config Server rebuild/restart (mitigated by `@RefreshScope`)
- No built-in multi-environment branching (use profiles: dev, docker, prod)
- Large binary files not suitable (config is YAML only)

## Implementation
```yaml
# config-server/src/main/resources/bootstrap.yml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: classpath:/config
```

All service configs placed in `config-server/src/main/resources/config/{service}.yml` with profile-specific sections:
```yaml
# product.yml
---
spring:
  config:
    activate:
      on-profile: docker
  data:
    mongodb:
      uri: mongodb://mongodb:27017
```

Services import config via `bootstrap.yml`:
```yaml
spring:
  application:
    name: product
  cloud:
    config:
      uri: http://config-server:8888
      import: "optional:configserver:"
```

## Related
- `docs/config-centralization.md` (detailed migration guide)
- `config-server/src/main/resources/config/`
