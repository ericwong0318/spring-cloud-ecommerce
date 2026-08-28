# Kubernetes Setup Guide

This guide covers deploying the Spring Cloud Microservices E-Commerce platform to Kubernetes using OrbStack.

## Prerequisites

- OrbStack app installed and running (provides Docker + single-node Kubernetes)
- `kubectl` CLI installed
- `helm` CLI installed
- Splunk Observability Cloud access token (for OTel Collector)

## Step 1: Configure kubectl Context

OrbStack bundles a built-in Kubernetes cluster. Verify it's available:

```bash
kubectl config get-contexts
```

You should see an `orbstack` context. Switch to it:

```bash
kubectl config use-context orbstack
```

Verify the cluster is running:

```bash
kubectl get nodes
# Should show: NAME STATUS ROLES AGE VERSION
#              orbstack Ready control-plane <age> v1.34.8+orb1
```

If the API server isn't responding, start it:

```bash
orb k8s
```

Wait a few seconds, then re-check `kubectl cluster-info`.

## Step 2: Install Splunk OTel Collector via Helm

The Splunk OpenTelemetry Collector for Kubernetes is the recommended way to instrument your cluster. It automatically injects the OpenTelemetry Java agent into your Java pods.

### Add the Helm Repository

```bash
helm repo add splunk-otel-collector-chart https://signalfx.github.io/splunk-otel-collector-chart
helm repo update
```

### Install the Collector

```bash
helm install splunk-otel-collector splunk-otel-collector-chart/splunk-otel-collector \
  --namespace splunk-otel \
  --create-namespace \
  --set "splunkObservability.accessToken=SPLUNK_ACCESS_TOKEN" \
  --set "clusterName=java-spring-boot" \
  --set "splunkObservability.realm=sg0" \
  --set "splunkObservability.profilingEnabled=true" \
  --set "environment=prod" \
  --set "operator.enabled=false" \
  --set "operatorcrds.install=false" \
  --set "agent.discovery.enabled=true"
```

**Important**: Set `operator.enabled=false` to avoid webhook race conditions in single-node clusters. This means auto-instrumentation via namespace annotation is still available but the operator's mutating webhook is disabled.

### Verify Installation

```bash
kubectl get pods -n splunk-otel
# Should show:
# splunk-otel-collector-agent-<hash>                1/1     Running
# splunk-otel-collector-k8s-cluster-receiver-<hash> 1/1     Running
```

## Step 3: Deploy Services

### Create the Namespace

```bash
kubectl create namespace ecommerce
kubectl label namespace ecommerce instrumentation.opentelemetry.io/inject-java=true --overwrite
```

The `instrumentation.opentelemetry.io/inject-java=true` label enables auto-instrumentation for Java workloads in this namespace.

### Apply the Deployment Manifests

```bash
kubectl apply -f k8s/deployments.yaml
```

This creates:
- A `ConfigMap` named `otel-config` with OTel environment variables
- 7 `Deployments` (config-server, eureka-server, gateway, product, category, auth-server, order-service)
- 7 `Services` (one per deployment)

### Verify Deployments

```bash
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
```

## Step 4: Build and Load Docker Images

Build all service images with the OpenTelemetry Java agent:

```bash
# Build all images
docker compose build

# Build a single service
docker compose build config-server

# Use the Makefile
make build-all
```

The Dockerfiles include a `wget --timeout=30` with `mkdir -p /app` pre-created to handle OrbStack network limitations.

## Step 5: Access Services

### Forward Ports Locally

```bash
# Gateway (HTTP on port 80)
kubectl port-forward -n ecommerce svc/gateway 8080:80

# Config Server
kubectl port-forward -n ecommerce svc/config-server 8888:8888

# Eureka Dashboard
kubectl port-forward -n ecommerce svc/eureka-server 8761:8761

# Product API
kubectl port-forward -n ecommerce svc/product 8081:8081

# Category API
kubectl port-forward -n ecommerce svc/category 8082:8082

# Auth Server
kubectl port-forward -n ecommerce svc/auth-server 9000:9000

# Order Service
kubectl port-forward -n ecommerce svc/order-service 8083:8083
```

### Verify Health Endpoints

```bash
curl http://localhost:8888/actuator/health   # config-server
curl http://localhost:8761/actuator/health   # eureka-server
curl http://localhost:9000/actuator/health   # auth-server
curl http://localhost:8080/actuator/health   # gateway
curl http://localhost:8081/actuator/health   # product
curl http://localhost:8082/actuator/health   # category
curl http://localhost:8083/actuator/health   # order
```

## Step 6: Verify Observability

### Check Collector Logs

```bash
kubectl logs -n splunk-otel daemonset/splunk-otel-collector-agent -f
```

### Check Application Logs

```bash
kubectl logs -n ecommerce -l app=gateway
```

### Verify Splunk O11y Data Flow

1. Open Splunk Observability Cloud console
2. Navigate to Log Explorer
3. Filter by `service.name` to see traces from each service
4. Check Metrics tab for JVM metrics
5. Check Traces tab for request latency

## Cleanup

```bash
# Remove all services
kubectl delete namespace ecommerce

# Uninstall Splunk OTel Collector
helm uninstall splunk-otel-collector -n splunk-otel
kubectl delete namespace splunk-otel
```

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| config-server | 8888 | Centralized configuration |
| eureka-server | 8761 | Service discovery |
| gateway | 8080 | API routing |
| product | 8081 | Product catalog |
| category | 8082 | Category management |
| auth-server | 9000 | OAuth2 token issuance |
| order-service | 8083 | Order processing |

## Environment Variables

Each service pod receives these OTel environment variables from the `otel-config` ConfigMap:

| Variable | Value |
|----------|-------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector:4317` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` |
| `OTEL_TRACES_EXPORTER` | `otlp` |
| `OTEL_METRICS_EXPORTER` | `otlp` |
| `OTEL_LOGS_EXPORTER` | `otlp` |
| `OTEL_PROPAGATORS` | `w3c,tracecontext` |
| `OTEL_RESOURCE_ATTRIBUTES` | `deployment.environment=prod,service.namespace=ecommerce` |

## Troubleshooting

### Pods stuck in ImagePullBackOff
Verify the Docker images are built and available in OrbStack's Docker daemon:
```bash
docker images | grep -E "(config|eureka|gateway|product|category|auth|order)-server"
```

### Collector not receiving data
Check collector logs for errors:
```bash
kubectl logs -n splunk-otel daemonset/splunk-otel-collector-agent
```

### 401 Unauthorized from Splunk
Verify the access token and realm are correct in the Helm values.

### No data in Splunk O11y
1. Check that `OTEL_LOGS_EXPORTER=otlp` is set
2. Verify the collector pipeline configuration in `otel-collector-config.yaml`
3. Check Splunk O11y realm dashboard for expected service names