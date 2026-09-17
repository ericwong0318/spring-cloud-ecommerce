{{- /*
Deployment template for Spring Boot services
*/ -}}
{{- define "spring-cloud-project.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Chart.Name }}-{{ .Values.name }}
  namespace: {{ .Values.global.namespace }}
  labels:
    {{- include "spring-cloud-project.labels" . | nindent 4 }}
    app: {{ .Values.name }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      {{- include "spring-cloud-project.selectorLabels" . | nindent 6 }}
      app: {{ .Values.name }}
  template:
    metadata:
      labels:
        {{- include "spring-cloud-project.selectorLabels" . | nindent 8 }}
        app: {{ .Values.name }}
      annotations:
        instrumentation.opentelemetry.io/inject-java: "true"
    spec:
      serviceAccountName: {{ .Values.serviceAccountName | default "default" }}
      securityContext:
        {{- toYaml .Values.podSecurityContext | nindent 8 }}
      containers:
        - name: {{ .Values.name }}
          image: "{{ .Values.global.imageRegistry }}{{ .Values.image }}:{{ .Values.tag }}"
          imagePullPolicy: {{ .Values.global.imagePullPolicy }}
          ports:
            - name: http
              containerPort: {{ .Values.port }}
              protocol: TCP
          envFrom:
            - configMapRef:
                name: {{ .Chart.Name }}-config
            - configMapRef:
                name: otel-config
            - secretRef:
                name: {{ .Chart.Name }}-secrets
          env:
            - name: OTEL_SERVICE_NAME
              value: {{ .Values.name }}
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: {{ .Values.global.otel.endpoint }}
            - name: OTEL_EXPORTER_OTLP_PROTOCOL
              value: {{ .Values.global.otel.protocol }}
            - name: OTEL_PROPAGATORS
              value: {{ .Values.global.otel.propagators | default "w3c,tracecontext" }}
            - name: OTEL_METRICS_EXPORTER
              value: {{ .Values.global.otel.metricsExporter | default "otlp" }}
            - name: OTEL_LOGS_EXPORTER
              value: {{ .Values.global.otel.logsExporter | default "otlp" }}
            {{- range $key, $value := .Values.env }}
            - name: {{ $key }}
              value: {{ $value | quote }}
            {{- end }}
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 20
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          resources:
            {{- toYaml .Values.resources | nindent 12 }}
      {{- with .Values.nodeSelector }}
      nodeSelector:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.affinity }}
      affinity:
        {{- toYaml . | nindent 8 }}
      {{- end }}
      {{- with .Values.tolerations }}
      tolerations:
        {{- toYaml . | nindent 8 }}
      {{- end }}
{{- end -}}