# OpenTelemetry Operator Troubleshooting Guide

This guide covers common issues with the OpenTelemetry Operator not becoming Ready and Java agent injection failing.

## Quick Diagnosis

Run the verification script:
```bash
./scripts/verify-otel-operator.sh
```

## Common Issues and Fixes

### 1. Operator Pod Not Ready

**Symptoms**: `kubectl get pods -n opentelemetry-operator-system` shows operator pod in `Pending`, `CrashLoopBackOff`, or `ImagePullBackOff`

**Causes & Fixes**:

| Cause | Check | Fix |
|-------|-------|-----|
| Image pull failure | `kubectl describe pod -n opentelemetry-operator-system` | Check image registry access; use `imagePullSecrets` if private |
| RBAC issues | `kubectl auth can-i create mutatingwebhookconfiguration --as=system:serviceaccount:opentelemetry-operator-system:opentelemetry-operator` | Ensure ClusterRole/ClusterRoleBinding exists |
| Resource limits | Check pod events | Increase resources in operator deployment |
| Cert-manager not ready | See issue #2 | Install/fix cert-manager first |

**Debug commands**:
```bash
kubectl logs -n opentelemetry-operator-system deployment/opentelemetry-operator
kubectl describe pod -n opentelemetry-operator-system -l app.kubernetes.io/name=opentelemetry-operator
```

### 2. Cert-Manager Not Ready (Most Common Root Cause)

**Symptoms**: Operator pod runs but webhook fails; `MutatingWebhookConfiguration` has empty `caBundle`

**Root Cause**: The operator's mutating webhook requires TLS certificates from cert-manager. If cert-manager isn't fully ready, the webhook can't serve admission requests.

**Check**:
```bash
kubectl get pods -n cert-manager
kubectl get certificates -n opentelemetry-operator-system
kubectl get certificaterequests -n opentelemetry-operator-system
```

**Fix**:
1. Ensure cert-manager v1.13+ is installed
2. Wait for all cert-manager deployments to be Available
3. Check for Certificate resources in operator namespace:
   ```bash
   kubectl get certificate -n opentelemetry-operator-system
   ```
4. If Certificate is stuck in `Pending`, check CertificateRequest:
   ```bash
   kubectl describe certificaterequest -n opentelemetry-operator-system
   ```

### 3. MutatingWebhookConfiguration Missing or Invalid

**Symptoms**: Pods don't get injected; annotation `instrumentation.opentelemetry.io/inject-java: "true"` has no effect

**Check**:
```bash
kubectl get mutatingwebhookconfiguration opentelemetry-operator -o yaml
```

**Expected**: `clientConfig.caBundle` should be populated (base64 encoded cert)

**Fix**: If caBundle is empty:
1. Delete the webhook config: `kubectl delete mutatingwebhookconfiguration opentelemetry-operator`
2. Restart operator: `kubectl rollout restart deployment/opentelemetry-operator -n opentelemetry-operator-system`
3. Wait for cert-manager to re-create it

### 4. Instrumentation Resource Not Ready

**Symptoms**: `kubectl get instrumentation -n ecommerce` shows status not Ready

**Check**:
```bash
kubectl get instrumentation -n ecommerce -o wide
kubectl describe instrumentation ecommerce-instrumentation -n ecommerce
```

**Common Issues**:
- **Wrong API version**: Must use `opentelemetry.io/v1beta1` (not v1alpha1)
- **Missing java.image**: Since v0.100.0, defaults removed - must specify `java.image`
- **Namespace mismatch**: Instrumentation must be in same namespace as target pods (or cluster-wide)
- **Invalid envConfig**: Check exporter endpoint format

**Fix**: Apply corrected Instrumentation resource:
```bash
kubectl apply -f k8s/instrumentation.yaml
```

### 5. Java Agent Not Injecting

**Symptoms**: Pods have annotation but no `javaagent` in JAVA_TOOL_OPTIONS

**Check pod**:
```bash
kubectl get pod <pod-name> -n ecommerce -o yaml | grep -A 20 "env:"
kubectl logs <pod-name> -n ecommerce | grep -i opentelemetry
```

**Causes**:
| Cause | Check | Fix |
|-------|-------|-----|
| Webhook not called | Check MutatingWebhookConfiguration | Fix cert-manager/webhook (issue #3) |
| Pod created before webhook ready | Check pod creation timestamp vs operator ready | Restart pods after operator ready |
| Namespace not selected | Check webhook namespaceSelector | Ensure namespace has label or matches selector |
| Container not Java | Webhook only injects Java containers | Ensure container uses Java base image |

**Force re-injection**:
```bash
kubectl rollout restart deployment -n ecommerce
```

### 6. Collector Connectivity Issues

**Symptoms**: Agent injects but no traces in Splunk

**Check**:
```bash
# From inside a pod
kubectl exec -n ecommerce <pod-name> -- curl -v http://spring-cloud-project-otel-collector:4317

# Collector logs
kubectl logs -n ecommerce deployment/spring-cloud-project-otel-collector
```

**Fix**: Ensure collector Service name matches `OTEL_EXPORTER_OTLP_ENDPOINT`

## Diagnostic Flowchart

```
Operator Not Ready?
├─ Yes → Check cert-manager (Issue #2)
│         └─ Fix cert-manager → Restart operator
└─ No → Webhook caBundle populated?
        ├─ No → Delete webhook → Restart operator
        └─ Yes → Instrumentation Ready?
                ├─ No → Check/fix Instrumentation resource
                └─ Yes → Pods have inject-java annotation?
                        ├─ No → Add annotation to deployments
                        └─ Yes → Agent in JAVA_TOOL_OPTIONS?
                                ├─ No → Restart pods (rollout restart)
                                └─ Yes → Check collector connectivity
```

## Version Compatibility Matrix

| Operator Version | Java Agent Version | CRD Version | Notes |
|-----------------|-------------------|-------------|-------|
| v0.114.0 | 2.8.0+ | v1beta1 | Current recommended |
| v0.100.0+ | Must specify image | v1beta1 | No default images |
| v0.90.0-v0.99.0 | Auto-default | v1alpha1 | Deprecated API |

## Useful Commands

```bash
# Full operator status
kubectl get all -n opentelemetry-operator-system

# Webhook cert status
kubectl get certificate,certificaterequest -n opentelemetry-operator-system

# Force webhook cert renewal
kubectl delete certificate -n opentelemetry-operator-system --all

# Debug webhook admission
kubectl logs -n opentelemetry-operator-system deployment/opentelemetry-operator -f

# Check what webhook sees
kubectl get mutatingwebhookconfiguration opentelemetry-operator -o yaml | grep -A 5 clientConfig

# Test injection dry-run
kubectl apply -f deployment.yaml --dry-run=server -o yaml | grep -A 5 "javaagent"
```

## Related Resources

- [OpenTelemetry Operator Docs](https://github.com/open-telemetry/opentelemetry-operator)
- [Instrumentation CRD Spec](https://github.com/open-telemetry/opentelemetry-operator/blob/main/docs/rfcs/instrumentation-v1beta1.md)
- [Java Auto-instrumentation Images](https://github.com/open-telemetry/opentelemetry-operator/pkgs/container/autoinstrumentation-java)