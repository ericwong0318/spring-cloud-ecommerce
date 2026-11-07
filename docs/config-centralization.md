# Configuration Centralization Guide

This document describes the migration from scattered local configuration to centralized Config Server with native profile.

## Overview

| Before | After |
|--------|-------|
| Each service has full `application.yml` with DB, RabbitMQ, security config | Each service has minimal `application.yml` with only `spring.config.import` |
| Config duplicated across services | Single source of truth in Config Server |
| Environment-specific values hardcoded | Profile-specific sections in Config Server (dev, docker, prod) |
| Secrets in code/config | Secrets via `${ENV_VAR}` placeholders |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CONFIG SERVER :8888                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  classpath:/config/                                 │   │
│  │  ├── product.yml      (dev, docker, prod profiles) │   │
│  │  ├── category.yml     (dev, docker, prod profiles) │   │
│  │  ├── order-service.yml                                │
│  │  ├── inventory-service.yml                           │
│  │  ├── payment-service.yml                             │
│  │  ├── notification-service.yml                        │
│  │  ├── auth-server.yml                                 │
│  │  ├── gateway.yml                                     │
│  │  └── eureka-server.yml                               │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ fetch config
┌──────────────┐ ┌──────────┐ ┌─────────┐ ┌──────────────┐
│  Product     │ │ Category │ │ Order   │ │ ...          │
│  :8081       │ │ :8082    │ │ :8083   │ │              │
└──────────────┘ └──────────┘ └─────────┘ └──────────────┘
```

## Service Configuration Pattern

### Minimal application.yml (All Services)
```yaml
spring:
  config:
    import: optional:configserver:http://config-server:8888
```

### Bootstrap.yml (Legacy Services - Category, Product)
```yaml
spring:
  application:
    name: category
  cloud:
    config:
      uri: http://config-server:8888
      import: "optional:configserver:"
  profiles:
    active: dev
```

## Config Server Profile Structure

Each service config has four profile layers:
```yaml
# product.yml
# ─────────────────────────────────────────
# BASE (fallback when no profile set)
server:
  port: 8081
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017
      database: product_db
  rabbitmq:
    host: localhost
    ...

---
# DEV (local bare-metal on Mac)
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:postgresql://localhost:5432/product_db
  ...
management:
  ...
logging:
  level:
    root: DEBUG

---
# DOCKER (Docker Compose / GitHub Actions CI)
spring:
  config:
    activate:
      on-profile: docker
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_test
  ...
management:
  ...

---
# PROD (production)
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: ${DATASOURCE_URL:jdbc:postgresql://localhost:5432/product_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  ...
management:
  ...
logging:
  level:
    root: INFO
```

### Profile Selection Guide

| Environment | Profile | Command |
|---|---|---|
| Local Mac (bare metal) | `dev` | `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -pl <service>` |
| Local Mac (Docker Compose) | `docker` | `docker compose up -d` (set in compose) |
| GitHub Actions CI | `docker` | `--spring.profiles.active=docker` (in ci.yml) |
| Production | `prod` | `SPRING_PROFILES_ACTIVE=prod` + env vars for credentials |

### Key Differences Between Profiles

| Setting | `dev` | `docker` | `prod` |
|---|---|---|---|
| Database host | `localhost` | `localhost` (Compose port-mapped) | `${DATASOURCE_URL}` |
| Redis host | `localhost` | `redis` (Docker DNS) | Not configured |
| Eureka URL | `http://localhost:8761/eureka/` | `http://eureka-server:8761/eureka/` | `${EUREKA_CLIENT_SERVICE_URL_DEFAULT_ZONE}` |
| Credentials | Hardcoded `test`/`test` | Hardcoded `guest`/`guest` | `${ENV_VAR}` injection |
| Hibernate ddl-auto | `update` / `create` | `update` | `validate` |
| Logging level | `DEBUG` | Framework default | `INFO` |

**Do not merge `dev` and `docker`** — `dev` uses `localhost` networking (bare metal), `docker` uses Docker DNS hostnames. Merging breaks one or the other.

## Docker Compose Environment Variables

After migration, `docker-compose.yml` only contains:
```yaml
services:
  product:
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_CLOUD_CONFIG_URI=http://config-server:8888
      - EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
      - POSTGRES_* (from .env)
      - RABBITMQ_* (from .env)
      - MAIL_* (from .env)
```

## Kubernetes / Helm

Helm chart uses `envFrom` with ConfigMap:
```yaml
# k8s/helm/spring-cloud-project/templates/_deployment.tpl
envFrom:
  - configMapRef:
      name: spring-cloud-ecommerce-config
  - secretRef:
      name: spring-cloud-ecommerce-secrets
env:
  - name: OTEL_SERVICE_NAME
    value: {{ .Values.service.name }}
```

ConfigMap `spring-cloud-ecommerce-config` contains only infra config:
```yaml
data:
  SPRING_PROFILES_ACTIVE: "prod"
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: "http://eureka-server:8761/eureka/"
  SPRING_CLOUD_CONFIG_URI: "http://config-server:8888"
```

## Verification

### Local Development
```bash
# Start config server
mvn spring-boot:run -pl config-server

# Verify config served
curl http://localhost:8888/product/default
curl http://localhost:8888/product/docker

# Start service with config
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run -pl product
```

### Docker Compose
```bash
docker compose -f docker-compose.yml up -d
# All services fetch config from Config Server
```

### Kubernetes
```bash
helm template spring-cloud-ecommerce ./k8s/helm/spring-cloud-project
# Verify deployments use envFrom with spring-cloud-ecommerce-config
```

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| Service fails to start | Config Server not reachable | Check `SPRING_CLOUD_CONFIG_URI` and Config Server health |
| Wrong profile active | `SPRING_PROFILES_ACTIVE` not set | Set to `docker` (compose) or `prod` (k8s) |
| Placeholder not resolved | Env var not set | Check `.env` / Kubernetes secrets |
| Config not updating | Config Server cache | Restart Config Server or use `@RefreshScope` |

## Related ADRs
- ADR 009: Config Server Native Profile
- ADR 013: CI Pipeline Fixes (spring-boot-maven-plugin)
