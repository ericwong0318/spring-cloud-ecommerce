FROM eclipse-temurin:21-jre-alpine

ARG OTEL_AGENT_VERSION=2.8.0
ARG SPLUNK_AGENT_VERSION=2.13.0
RUN apk add --no-cache wget && \
    mkdir -p /app && \
    wget -q --timeout=30 https://github.com/signalfx/splunk-otel-java/releases/download/v${SPLUNK_AGENT_VERSION}/splunk-otel-javaagent.jar -O /app/splunk-otel-javaagent.jar

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-javaagent:/app/splunk-otel-javaagent.jar", "-jar", "app.jar"]