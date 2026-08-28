FROM eclipse-temurin:21-jre-alpine

ARG OTEL_AGENT_VERSION=2.8.0
RUN apk add --no-cache wget && \
    mkdir -p /app && \
    wget -q --timeout=30 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar -O /app/opentelemetry-javaagent.jar

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]