{{/*
Expand the name of the chart.
*/}}
{{- define "data-claims-reporting.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "data-claims-reporting.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "data-claims-reporting.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "data-claims-reporting.labels" -}}
helm.sh/chart: {{ include "data-claims-reporting.chart" . }}
{{ include "data-claims-reporting.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- if .Values.appLabel }}
app: {{ .Values.appLabel }}
{{- end }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "data-claims-reporting.selectorLabels" -}}
app.kubernetes.io/name: {{ include "data-claims-reporting.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "data-claims-reporting.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "data-claims-reporting.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Return the app label for selector matching
*/}}
{{- define "data-claims-reporting.appLabel" -}}
app.kubernetes.io/name: {{ .Values.nameOverride | default .Chart.Name }}
{{- end }}
