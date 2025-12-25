#!/usr/bin/env bash
set -euo pipefail

MODULE="${1:-}"
AGENT_PATH="${2:-otel/opentelemetry-javaagent.jar}"

if [[ -z "${MODULE}" ]]; then
  echo "用法: $0 <module> [agent_path]" >&2
  echo "示例: $0 sword-shield-user" >&2
  exit 1
fi

# 规范化 agent 路径为绝对路径，避免切换目录后找不到
AGENT_PATH="$(cd "$(dirname "${AGENT_PATH}")" && pwd)/$(basename "${AGENT_PATH}")"

if [[ ! -f "${AGENT_PATH}" ]]; then
  echo "未找到 Java Agent: ${AGENT_PATH}" >&2
  echo "请从 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases 下载 opentelemetry-javaagent.jar 并放到该路径，或显式传入 agent 路径。" >&2
  exit 2
fi

# 默认导出到本机 Collector (HTTP 4318)，只导出 trace，脱敏 SQL
export OTEL_SERVICE_NAME="${OTEL_SERVICE_NAME:-${MODULE}}"
export OTEL_EXPORTER_OTLP_ENDPOINT="${OTEL_EXPORTER_OTLP_ENDPOINT:-http://localhost:4318}"
export OTEL_EXPORTER_OTLP_PROTOCOL="${OTEL_EXPORTER_OTLP_PROTOCOL:-http/protobuf}"
export OTEL_TRACES_EXPORTER="${OTEL_TRACES_EXPORTER:-otlp}"
export OTEL_METRICS_EXPORTER="${OTEL_METRICS_EXPORTER:-none}"
export OTEL_LOGS_EXPORTER="${OTEL_LOGS_EXPORTER:-none}"
export OTEL_PROPAGATORS="${OTEL_PROPAGATORS:-tracecontext,baggage}"
export OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED="${OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED:-true}"

# 使用 Javaagent 时关闭 Spring Boot/Micrometer Tracing，避免双重埋点导致重复 span / trace 不一致
export MANAGEMENT_TRACING_ENABLED="${MANAGEMENT_TRACING_ENABLED:-false}"

# 在模块目录内运行，避免 spring-boot:run 作用于父 POM
if [[ ! -d "${MODULE}" ]]; then
  echo "模块目录不存在: ${MODULE}" >&2
  exit 3
fi

pushd "${MODULE}" >/dev/null
mvn spring-boot:run \
  -DskipTests \
  -Dspring-boot.run.jvmArguments="-javaagent:${AGENT_PATH}"
popd >/dev/null
