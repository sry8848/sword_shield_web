# 使用 OTel Java Agent 补充数据库/SQL 链路

当前各服务已通过 Micrometer + OTLP 将 HTTP 链路送到 Collector。要把 JDBC/Hibernate 等数据库调用也带上 span（可看到 SQL，默认脱敏参数），推荐用 OpenTelemetry Java Agent。

## 准备 agent
1) 从官方发布页下载 `opentelemetry-javaagent.jar`（2.x 版本）：  
   https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases  
2) 将文件放到仓库的 `otel/opentelemetry-javaagent.jar`（或任意路径，运行时指定）。

## 运行方式 1：脚本启动（推荐开发态）
在仓库根目录执行：
```sh
# 示例启动 user 服务，默认导出到本机 Collector (http://localhost:4318)
./scripts/run-with-otel-agent.sh sword-shield-user
```
脚本会：
- 自动设置 OTLP HTTP 出口到 `http://localhost:4318/v1/traces`（Collector 默认端口）。
- 仅导出 trace，关闭 metrics/logs（减少噪音）。
- 开启 SQL 脱敏（参数替换为 `?`）。
- `OTEL_SERVICE_NAME` 默认为模块名，可按需覆盖：  
  `OTEL_SERVICE_NAME=my-user ./scripts/run-with-otel-agent.sh sword-shield-user`

## 运行方式 2：运行已打包的 JAR
```sh
OTEL_SERVICE_NAME=sword-shield-user \
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 \
OTEL_EXPORTER_OTLP_PROTOCOL=http/protobuf \
OTEL_TRACES_EXPORTER=otlp \
OTEL_METRICS_EXPORTER=none \
OTEL_LOGS_EXPORTER=none \
OTEL_INSTRUMENTATION_COMMON_DB_STATEMENT_SANITIZER_ENABLED=true \
java -javaagent:otel/opentelemetry-javaagent.jar -jar target/sword-shield-user-0.1.0-SNAPSHOT.jar
```
其他服务同理，改 `OTEL_SERVICE_NAME` 和 JAR 路径即可。

## 查看效果
通过网关发请求后，刷新 Jaeger UI (http://localhost:16686)。在某个 trace 的 span 列表中会出现数据库相关 span，`db.system`/`db.statement` 会展示 SQL（已脱敏）。
