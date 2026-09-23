import re

# Fix DistributedTracingVerificationTest.java
with open('/home/ubuntu/spring-cloud-ecommerce/system-test/src/test/java/com/example/system/tracing/DistributedTracingVerificationTest.java', 'r') as f:
    content = f.read()

# Add import for System if not present
if "import java.util.List;" in content and "import java.lang.System;" not in content:
    content = content.replace("import java.util.List;", "import java.util.List;\nimport java.lang.System;")

# Replace paths
content = content.replace(
    'String dockerComposePath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/docker-compose.yml";',
    'String dockerComposePath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/docker-compose.yml";'
)
content = content.replace(
    'String k8sConfigMapPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/deployments.yaml";',
    'String k8sConfigMapPath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/k8s/deployments.yaml";'
)
content = content.replace(
    'String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";',
    'String otelConfigPath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/otel-collector/otel-collector-config.yaml";'
)

with open('/home/ubuntu/spring-cloud-ecommerce/system-test/src/test/java/com/example/system/tracing/DistributedTracingVerificationTest.java', 'w') as f:
    f.write(content)

# Fix LogTraceCorrelationVerificationTest.java
with open('/home/ubuntu/spring-cloud-ecommerce/system-test/src/test/java/com/example/system/tracing/LogTraceCorrelationVerificationTest.java', 'r') as f:
    content = f.read()

if "import java.util.List;" in content and "import java.lang.System;" not in content:
    content = content.replace("import java.util.List;", "import java.util.List;\nimport java.lang.System;")

content = content.replace(
    'String dockerComposePath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/docker-compose.yml";',
    'String dockerComposePath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/docker-compose.yml";'
)
content = content.replace(
    'String otelConfigPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/otel-collector/otel-collector-config.yaml";',
    'String otelConfigPath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/otel-collector/otel-collector-config.yaml";'
)
content = content.replace(
    'String k8sDeploymentPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/deployments.yaml";',
    'String k8sDeploymentPath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/k8s/deployments.yaml";'
)
content = content.replace(
    'String helmValuesPath = "/Users/ericw/spring-cloud-ecommerce-worktrees/main/k8s/helm/spring-cloud-project/values.yaml";',
    'String helmValuesPath = System.getProperty("project.root", "/home/ubuntu/spring-cloud-ecommerce") + "/k8s/helm/spring-cloud-project/values.yaml";'
)

with open('/home/ubuntu/spring-cloud-ecommerce/system-test/src/test/java/com/example/system/tracing/LogTraceCorrelationVerificationTest.java', 'w') as f:
    f.write(content)

print("Fixed both test files")