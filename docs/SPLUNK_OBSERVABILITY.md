# Splunk Observability Cloud Guide

This guide covers the observability stack for the Spring Cloud Microservices platform.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Spring Boot Apps                          │
│  (config-server, eureka-server, gateway, product, category,       │
│   auth-server, order-service)                                    │
│                                                                  │
│  Each app runs with -javaagent:opentelemetry-javaagent.jar      │
│  Auto-instrumentation: Spring, JDBC, HTTP, JVM                   │
│                                                                  │
│  OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317          │
│  OTEL_LOGS_EXPORTER=otlp                                        │
│  OTEL_SERVICE_NAME=<service-name>                                │
└─────────────────────────────┬───────────────────────────────────┘
                              │ OTLP (gRPC, port 4317)
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│           Splunk OTel Collector Agent (DaemonSet)                 │
│           Listens on host port 4317                               │
│                                                                  │
│  Receives: traces, metrics, logs from Java agents               │
└─────────────────────────────┬───────────────────────────────────┘
                              │ OTLP HTTP (traces + metrics)
                              │ X-SF-Token header with access token
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              Splunk Observability Cloud                           │
│              Realm: sg0                                          │
│                                                                  │
│  🔍 Infrastructure Map    — Service topology                     │
│  📊 APM / Traces           — Distributed tracing                 │
│  📈 Metrics                — JVM, HTTP, DB metrics              │
│  📝 Logs                   — Application logs                     │
│  🚨 Alerts                 — Anomaly detection                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│        Splunk OTel Collector (Cluster Receiver)                   │
│        (Kubernetes metrics, kubelet stats)                        │
│        Runs as Deployment in splunk-otel namespace                │
└─────────────────────────────────────────────────────────────────┘
```

## Signal Flow

### Traces
1. HTTP request hits a service
2. OpenTelemetry Java Agent auto-instrumentation creates spans
3. Spans exported via OTLP gRPC to local OTel Agent (port 4317)
4. Agent batches and forwards via OTLP HTTP to Splunk O11y
5. View in: Splunk O11y → APM → Traces

### Metrics
1. Micrometer metrics exposed at `/actuator/prometheus`
2. OTel Collector Agent scrapes `/actuator/prometheus` every 15s
3. Forwarded to Splunk O11y Metrics
4. View in: Splunk O11y → Infrastructure Overview → Hosts / Containers

### Logs
1. Application logs written to stdout
2. OTel Java Agent auto-collects logs via `OTEL_LOGS_EXPORTER=otlp`
3. Exported via OTLP to OTel Agent (port 4317)
4. Forwarded to Splunk O11y Logs
5. View in: Splunk O11y → Log Explorer

## Service Resource Attributes

Each service is tagged with:

| Attribute | Value |
|-----------|-------|
| `deployment.environment` | `prod` |
| `service.namespace` | `ecommerce` |
| `service.name` | Per service (e.g., `gateway`, `product`) |
| `service.version` | App version |
| `host.name` | Pod hostname |

## Key Metrics Available

### JVM Metrics
- `jvm.memory.used` / `jvm.memory.max`
- `jvm.gc.pause`
- `jvm.threads`
- `jvm.classes.loaded`

### HTTP Metrics
- `http.server.requests` (count, latency)
- `http.client.requests` (outbound calls)

### Database Metrics
- `db.client.connections.*` (HikariCP / R2DBC)
- `db.postgresql.connections.*`

### Spring Boot Actuator
- `spring.security.login.*`
- `process.uptime`
- `system.cpu.*`

## Environment Variables

These are set in `k8s/deployments.yaml` via the `otel-config` ConfigMap:

```yaml
OTEL_EXPORTER_OTLP_ENDPOINT: "http://otel-collector:4317"
OTEL_EXPORTER_OTLP_PROTOCOL: "grpc"
OTEL_TRACES_EXPORTER: "otlp"
OTEL_METRICS_EXPORTER: "otlp"
OTEL_LOGS_EXPORTER: "otlp"         # REQUIRED for log export
OTEL_PROPAGATORS: "w3c,tracecontext"  # W3C trace context for distributed tracing
OTEL_RESOURCE_ATTRIBUTES: "deployment.environment=prod,service.namespace=ecommerce"
```

## Collector Configuration

The Helm chart manages the OTel Collector configuration. Key settings:

```yaml
# Splunk OTel Collector Helm values
splunkObservability:
  accessToken: <your-token>
  realm: sg0
  profilingEnabled: true

environment: prod
clusterName: java-spring-boot

agent:
  discovery:
    enabled: true    # Auto-detect instrumented pods

operator:
  enabled: false     # Disabled to avoid webhook race conditions
```

## Verifying Data Flow

### Check Collector Logs
```bash
kubectl logs -n splunk-otel daemonset/splunk-otel-collector-agent -f
```

Look for:
- `Exporter started` messages
- `Received span` counts
- Any `ERROR` or `WARN` messages

### Check Application Logs
```bash
kubectl logs -n ecommerce -l app=gateway
```

Look for OTel agent initialization:
```
[opentelemetry.javaagent] INFO io.opentelemetry.javaagent - Java agent version: 2.8.0
```

### Trigger Test Traffic
```bash
# Health checks
curl http://localhost:8080/actuator/health

# Product API
curl http://localhost:8080/api/v1/products

# Category API
curl http://localhost:8080/api/v1/categories
```

Then check Splunk O11y for new traces.

## Troubleshooting

### No data in Splunk O11y

1. **Check collector is running:**
   ```bash
   kubectl get pods -n splunk-otel
   ```

2. **Check logs for export errors:**
   ```bash
   kubectl logs -n splunk-otel daemonset/splunk-otel-collector-agent | grep -i error
   ```

3. **Verify endpoint configuration:**
   ```bash
   kubectl exec -n ecommerce deploy/gateway -- env | grep OTEL
   ```

4. **Check network connectivity:**
   ```bash
   kubectl exec -n ecommerce pod/<pod-name> -- curl -s http://otel-collector:4317
   ```

### 401 Unauthorized

- Verify the Splunk access token is correct
- Check the realm (`sg0`) matches your Splunk O11y organization
- Tokens are realm-specific

### Missing traces between services

1. Verify `OTEL_PROPAGATORS=w3c,tracecontext` is set
2. Ensure services use `WebClient` or `RestClient` (not raw HTTP clients)
3. Check Splunk O11y → APM → Infrastructure Map for service connections

### JVM metrics missing

1. Verify `/actuator/prometheus` endpoint is accessible:
   ```bash
   kubectl exec -n ecommerce pod/<pod-name> -- curl localhost:8081/actuator/prometheus
   ```
2. Check Prometheus scrape config in collector logs

## Splunk O11y Navigation

| What you want | Where to go |
|--------------|------------|
| Service map | Infrastructure → Infrastructure Map |
| Traces | APM → Traces |
| Metrics | Infrastructure Overview → By Service |
| Logs | Log Explorer |
| Alerts | Alerts & Detectors |
| Dashboards | Dashboards → (create custom) |

## Performance Considerations

- **Agent overhead**: ~1-3% CPU, ~50MB RAM
- **Batch size**: 1024 spans default (configurable)
- **Prometheus scrape interval**: 15s
- **OTLP protocol**: gRPC (local) → HTTP (Splunk cloud)

## Security Notes

- The Splunk access token is stored in the Helm values
- Consider using Kubernetes Secrets for the token
- The token is passed to the OTel Agent DaemonSet
- No app-level credentials are sent to Splunk O11y
