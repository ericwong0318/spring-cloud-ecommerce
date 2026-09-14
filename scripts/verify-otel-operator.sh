#!/bin/bash
# verify-otel-operator.sh - Feedback loop for OTel Operator + Java agent injection
# Usage: ./verify-otel-operator.sh
# Returns: 0 if operator Ready AND agent injecting, 1 otherwise

set -euo pipefail

NAMESPACE="ecommerce"
OPERATOR_NAMESPACE="opentelemetry-operator-system"

echo "=== OTel Operator + Java Agent Injection Verification ==="
echo ""

# Check 1: Operator deployment status
echo "--- Check 1: OpenTelemetry Operator Deployment ---"
if kubectl get deployment -n "$OPERATOR_NAMESPACE" opentelemetry-operator-controller-manager 2>/dev/null; then
    OPERATOR_READY=$(kubectl get deployment -n "$OPERATOR_NAMESPACE" opentelemetry-operator-controller-manager -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' 2>/dev/null || echo "Unknown")
    OPERATOR_REPLICAS=$(kubectl get deployment -n "$OPERATOR_NAMESPACE" opentelemetry-operator-controller-manager -o jsonpath='{.status.readyReplicas}' 2>/dev/null || echo "0")
    OPERATOR_DESIRED=$(kubectl get deployment -n "$OPERATOR_NAMESPACE" opentelemetry-operator-controller-manager -o jsonpath='{.spec.replicas}' 2>/dev/null || echo "1")
    
    echo "Operator Ready: $OPERATOR_READY"
    echo "Ready Replicas: $OPERATOR_REPLICAS / $OPERATOR_DESIRED"
    
    if [[ "$OPERATOR_READY" == "True" && "$OPERATOR_REPLICAS" -ge "$OPERATOR_DESIRED" ]]; then
        echo "✓ Operator is Ready"
        OPERATOR_OK=0
    else
        echo "✗ Operator is NOT Ready"
        OPERATOR_OK=1
    fi
else
    echo "✗ Operator deployment not found in namespace $OPERATOR_NAMESPACE"
    OPERATOR_OK=1
fi

echo ""

# Check 2: Cert-manager (required for webhook certs)
echo "--- Check 2: Cert-Manager ---"
if kubectl get deployment -n cert-manager cert-manager 2>/dev/null; then
    CERT_MANAGER_READY=$(kubectl get deployment -n cert-manager cert-manager -o jsonpath='{.status.conditions[?(@.type=="Available")].status}' 2>/dev/null || echo "Unknown")
    echo "Cert-Manager Ready: $CERT_MANAGER_READY"
    
    if [[ "$CERT_MANAGER_READY" == "True" ]]; then
        echo "✓ Cert-Manager is Ready"
        CERT_MANAGER_OK=0
    else
        echo "✗ Cert-Manager is NOT Ready"
        CERT_MANAGER_OK=1
    fi
else
    echo "✗ Cert-Manager deployment not found"
    CERT_MANAGER_OK=1
fi

echo ""

# Check 3: Instrumentation resource
echo "--- Check 3: Instrumentation Resource ---"
INSTRUMENTATION=$(kubectl get instrumentation -n "$NAMESPACE" 2>/dev/null | grep -v NAME || echo "NOT_FOUND")
if [[ "$INSTRUMENTATION" != "NOT_FOUND" ]]; then
    echo "Instrumentation resources found:"
    kubectl get instrumentation -n "$NAMESPACE" -o wide
    
    # Check each instrumentation resource
    INST_NAMES=$(kubectl get instrumentation -n "$NAMESPACE" -o jsonpath='{.items[*].metadata.name}')
    INSTR_OK=0
    for INST in $INST_NAMES; do
        echo "Checking instrumentation: $INST"
        INST_STATUS=$(kubectl get instrumentation "$INST" -n "$NAMESPACE" -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null || echo "Unknown")
        echo "  Status: $INST_STATUS"
        
        # Check spec
        INST_VERSION=$(kubectl get instrumentation "$INST" -n "$NAMESPACE" -o jsonpath='{.spec.java.version}' 2>/dev/null || echo "not set")
        echo "  Java Agent Version: $INST_VERSION"
        
        if [[ "$INST_STATUS" != "True" ]]; then
            INSTR_OK=1
        fi
    done
    
    if [[ $INSTR_OK -eq 0 ]]; then
        echo "✓ All Instrumentation resources are Ready"
    else
        echo "✗ Some Instrumentation resources are NOT Ready"
    fi
else
    echo "✗ No Instrumentation resource found in namespace $NAMESPACE"
    INSTR_OK=1
fi

echo ""

# Check 4: Mutating webhook (required for injection)
echo "--- Check 4: Mutating Webhook Configuration ---"
WEBHOOK=$(kubectl get mutatingwebhookconfiguration opentelemetry-operator-mutating-webhook-configuration 2>/dev/null || echo "NOT_FOUND")
if [[ "$WEBHOOK" != "NOT_FOUND" ]]; then
    echo "MutatingWebhookConfiguration found:"
    kubectl get mutatingwebhookconfiguration opentelemetry-operator-mutating-webhook-configuration -o yaml | grep -A 2 "clientConfig:" | head -10
    
    # Check if webhook has valid caBundle
    CA_BUNDLE=$(kubectl get mutatingwebhookconfiguration opentelemetry-operator-mutating-webhook-configuration -o jsonpath='{.webhooks[0].clientConfig.caBundle}' 2>/dev/null || echo "")
    if [[ -n "$CA_BUNDLE" && "$CA_BUNDLE" != "null" ]]; then
        echo "✓ Webhook has CA bundle"
        WEBHOOK_OK=0
    else
        echo "✗ Webhook missing CA bundle"
        WEBHOOK_OK=1
    fi
else
    echo "✗ MutatingWebhookConfiguration not found"
    WEBHOOK_OK=1
fi

echo ""

# Check 5: Pod injection verification
echo "--- Check 5: Java Agent Injection in Pods ---"
PODS=$(kubectl get pods -n "$NAMESPACE" -l 'app in (config-server,eureka-server,gateway,product,category,auth-server,order-service)' -o jsonpath='{.items[*].metadata.name}' 2>/dev/null || echo "")
if [[ -n "$PODS" ]]; then
    INJECTION_OK=0
    for POD in $PODS; do
        # Check if pod has the annotation
        ANNOTATION=$(kubectl get pod "$POD" -n "$NAMESPACE" -o jsonpath='{.metadata.annotations.instrumentation\.opentelemetry\.io/inject-java}' 2>/dev/null || echo "not-set")
        
        # Check for javaagent in container args
        JAVA_AGENT=$(kubectl get pod "$POD" -n "$NAMESPACE" -o jsonpath='{.spec.containers[0].args}' 2>/dev/null | grep -c "javaagent" || echo "0")
        JAVA_AGENT=$(echo "$JAVA_AGENT" | tr -d '\n' | xargs)
        
        # Check for OTEL_JAVAAGENT env var or volume mount
        AGENT_VOLUME=$(kubectl get pod "$POD" -n "$NAMESPACE" -o jsonpath='{.spec.volumes[*].name}' 2>/dev/null | grep -c "opentelemetry" || echo "0")
        AGENT_VOLUME=$(echo "$AGENT_VOLUME" | tr -d '\n' | xargs)
        AGENT_MOUNT=$(kubectl get pod "$POD" -n "$NAMESPACE" -o jsonpath='{.spec.containers[0].volumeMounts[*].name}' 2>/dev/null | grep -c "opentelemetry" || echo "0")
        AGENT_MOUNT=$(echo "$AGENT_MOUNT" | tr -d '\n' | xargs)
        
        echo "Pod: $POD"
        echo "  inject-java annotation: $ANNOTATION"
        echo "  javaagent in args: $JAVA_AGENT"
        echo "  agent volume: $AGENT_VOLUME"
        echo "  agent mount: $AGENT_MOUNT"
        
        # Check logs for agent startup
        AGENT_LOG=$(kubectl logs "$POD" -n "$NAMESPACE" --tail=50 2>/dev/null | grep -i "opentelemetry.javaagent" | head -1 || echo "NOT_FOUND")
        if [[ "$AGENT_LOG" != "NOT_FOUND" ]]; then
            echo "  Agent log: $AGENT_LOG"
        else
            echo "  Agent log: NOT FOUND in recent logs"
        fi
        
        if [[ "$ANNOTATION" == "true" && ( "$JAVA_AGENT" -gt 0 || "$AGENT_VOLUME" -gt 0 || "$AGENT_MOUNT" -gt 0 ) ]]; then
            echo "  ✓ Agent appears to be injected"
        else
            echo "  ✗ Agent NOT injected"
            INJECTION_OK=1
        fi
    done
else
    echo "✗ No pods found in namespace $NAMESPACE"
    INJECTION_OK=1
fi

echo ""

# Check 6: Collector connectivity
echo "--- Check 6: OTel Collector Connectivity ---"
COLLECTOR_POD=$(kubectl get pods -n "$NAMESPACE" -l app=otel-collector -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [[ -n "$COLLECTOR_POD" ]]; then
    echo "Collector pod: $COLLECTOR_POD"
    COLLECTOR_READY=$(kubectl get pod "$COLLECTOR_POD" -n "$NAMESPACE" -o jsonpath='{.status.conditions[?(@.type=="Ready")].status}' 2>/dev/null || echo "Unknown")
    echo "Collector Ready: $COLLECTOR_READY"
    
    if [[ "$COLLECTOR_READY" == "True" ]]; then
        echo "✓ Collector is Ready"
        COLLECTOR_OK=0
    else
        echo "✗ Collector is NOT Ready"
        COLLECTOR_OK=1
    fi
else
    echo "✗ Collector pod not found"
    COLLECTOR_OK=1
fi

echo ""
echo "=== SUMMARY ==="
echo "Operator Ready: $([ $OPERATOR_OK -eq 0 ] && echo PASS || echo FAIL)"
echo "Cert-Manager Ready: $([ $CERT_MANAGER_OK -eq 0 ] && echo PASS || echo FAIL)"
echo "Instrumentation Ready: $([ $INSTR_OK -eq 0 ] && echo PASS || echo FAIL)"
echo "Webhook Configured: $([ $WEBHOOK_OK -eq 0 ] && echo PASS || echo FAIL)"
echo "Agent Injection: $([ $INJECTION_OK -eq 0 ] && echo PASS || echo FAIL)"
echo "Collector Ready: $([ $COLLECTOR_OK -eq 0 ] && echo PASS || echo FAIL)"

# Overall result
if [[ $OPERATOR_OK -eq 0 && $CERT_MANAGER_OK -eq 0 && $INSTR_OK -eq 0 && $WEBHOOK_OK -eq 0 && $INJECTION_OK -eq 0 && $COLLECTOR_OK -eq 0 ]]; then
    echo ""
    echo "=== ALL CHECKS PASSED ==="
    exit 0
else
    echo ""
    echo "=== SOME CHECKS FAILED ==="
    exit 1
fi