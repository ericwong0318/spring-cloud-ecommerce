FROM eclipse-temurin:21-jre-alpine

ARG OTEL_AGENT_VERSION=2.8.0
ARG SPLUNK_AGENT_VERSION=2.13.0
RUN apk add --no-cache wget && \
    mkdir -p /app && \
    wget -q --timeout=30 https://github.com/signalfx/splunk-otel-java/releases/download/v${SPLUNK_AGENT_VERSION}/splunk-otel-javaagent.jar -O /app/splunk-otel-javaagent.jar

WORKDIR /app

COPY target/payment-service-*.jar app.jar

EXPOSE 8085

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/app/splunk-otel-javaagent.jar -jar app.jar"]
