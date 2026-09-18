FROM eclipse-temurin:21-jre-alpine

ARG SPLUNK_AGENT_VERSION=2.13.0
ARG ENABLE_SPLUNK_AGENT=true
RUN apk add --no-cache wget && \
    mkdir -p /app && \
    if [ "$ENABLE_SPLUNK_AGENT" = "true" ]; then \
      wget -q --timeout=30 https://github.com/signalfx/splunk-otel-java/releases/download/v${SPLUNK_AGENT_VERSION}/splunk-otel-javaagent.jar -O /app/splunk-otel-javaagent.jar; \
    fi

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["sh", "-c", "if [ \"$ENABLE_SPLUNK_AGENT\" = \"true\" ]; then java -javaagent:/app/splunk-otel-javaagent.jar -jar app.jar; else java -jar app.jar; fi"]