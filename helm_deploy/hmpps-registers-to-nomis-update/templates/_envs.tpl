    {{/* vim: set filetype=mustache: */}}
{{/*
Environment variables for web and worker containers
*/}}
{{- define "deployment.envs" }}
env:
  - name: SERVER_PORT
    value: "{{ .Values.image.port }}"

  - name: JAVA_OPTS
    value: "{{ .Values.env.JAVA_OPTS }}"

  - name: OAUTH_ENDPOINT_URL
    value: "{{ .Values.env.OAUTH_ENDPOINT_URL }}"

  - name: PRISON_ENDPOINT_URL
    value: "{{ .Values.env.PRISON_ENDPOINT_URL }}"

  - name: COURT_REGISTER_ENDPOINT_URL
    value: "{{ .Values.env.COURT_REGISTER_ENDPOINT_URL }}"

  - name: REGISTERTONOMIS_APPLY_CHANGES
    value: "{{ .Values.env.APPLY_CHANGES }}"

  - name: SPRING_PROFILES_ACTIVE
    value: "logstash"

  - name: APPINSIGHTS_INSTRUMENTATIONKEY
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: APPINSIGHTS_INSTRUMENTATIONKEY

  - name: APPLICATIONINSIGHTS_CONNECTION_STRING
    value: "InstrumentationKey=$(APPINSIGHTS_INSTRUMENTATIONKEY)"

  - name: APPLICATIONINSIGHTS_CONFIGURATION_FILE
    value: "{{ .Values.env.APPLICATIONINSIGHTS_CONFIGURATION_FILE }}"

  - name: HMPPS_AUTH_CLIENT_CLIENT_ID
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OAUTH_CLIENT_ID

  - name: HMPPS_AUTH_CLIENT_CLIENT_SECRET
    valueFrom:
      secretKeyRef:
        name: {{ template "app.name" . }}
        key: OAUTH_CLIENT_SECRET

  - name: HMPPS_SQS_QUEUES_REGISTERS_QUEUE_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-secret
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_REGISTERS_QUEUE_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-secret
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_REGISTERS_QUEUE_NAME
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-secret
        key: sqs_queue_name

  - name: HMPPS_SQS_QUEUES_REGISTERS_DLQ_ACCESS_KEY_ID
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-dl-secret
        key: access_key_id

  - name: HMPPS_SQS_QUEUES_REGISTERS_DLQ_SECRET_ACCESS_KEY
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-dl-secret
        key: secret_access_key

  - name: HMPPS_SQS_QUEUES_REGISTERS_DLQ_NAME
    valueFrom:
      secretKeyRef:
        name: sqs-nomis-update-dl-secret
        key: sqs_queue_name

{{- end -}}
