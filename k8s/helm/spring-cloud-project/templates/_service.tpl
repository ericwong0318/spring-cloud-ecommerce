{{/*
Service template for Spring Boot services
*/}}
{{- define "spring-cloud-project.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .Chart.Name }}-{{ .Values.name }}
  namespace: {{ .Values.global.namespace }}
  labels:
    {{- include "spring-cloud-project.labels" . | nindent 4 }}
    app: {{ .Values.name }}
spec:
  type: {{ .Values.serviceType | default "ClusterIP" }}
  ports:
    - port: {{ .Values.port }}
      targetPort: http
      protocol: TCP
      name: http
  selector:
    {{- include "spring-cloud-project.selectorLabels" . | nindent 4 }}
    app: {{ .Values.name }}
{{- end -}}