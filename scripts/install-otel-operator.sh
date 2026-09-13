#!/bin/bash
# install-otel-operator.sh - Install OpenTelemetry Operator with cert-manager
# Usage: ./install-otel-operator.sh

set -euo pipefail

echo "=== Installing OpenTelemetry Operator ==="
echo ""

# Step 1: Install cert-manager (required for webhook TLS certificates)
echo "--- Step 1: Installing cert-manager ---"
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.5/cert-manager.yaml

echo "Waiting for cert-manager to be ready..."
kubectl wait --for=condition=Available deployment/cert-manager -n cert-manager --timeout=120s
kubectl wait --for=condition=Available deployment/cert-manager-cainjector -n cert-manager --timeout=120s
kubectl wait --for=condition=Available deployment/cert-manager-webhook -n cert-manager --timeout=120s

echo "✓ cert-manager is ready"
echo ""

# Step 2: Install OpenTelemetry Operator
echo "--- Step 2: Installing OpenTelemetry Operator ---"
kubectl apply -f https://github.com/open-telemetry/opentelemetry-operator/releases/download/v0.114.0/opentelemetry-operator.yaml

echo "Waiting for OpenTelemetry Operator to be ready..."
kubectl wait --for=condition=Available deployment/opentelemetry-operator -n opentelemetry-operator-system --timeout=120s

echo "✓ OpenTelemetry Operator is ready"
echo ""

# Step 3: Verify installation
echo "--- Step 3: Verifying Installation ---"
echo "Operator pods:"
kubectl get pods -n opentelemetry-operator-system

echo ""
echo "Cert-manager pods:"
kubectl get pods -n cert-manager

echo ""
echo "MutatingWebhookConfiguration:"
kubectl get mutatingwebhookconfiguration opentelemetry-operator

echo ""
echo "CRDs installed:"
kubectl get crd | grep opentelemetry

echo ""
echo "=== Installation Complete ==="
echo "Next steps:"
echo "1. Apply the Instrumentation resource: kubectl apply -f k8s/helm/spring-cloud-project/templates/instrumentation.yaml"
echo "2. Run verification script: ./scripts/verify-otel-operator.sh"