# sword-shield-web

基于 Java 17 的 Spring Boot 3.2 微服务套件，配合 Vue 3 + Vite 前端。对外统一通过 Spring Cloud Gateway 暴露 HTTP API（`/api/**`），对内服务间调用使用 Dubbo RPC（网关仍保持 HTTP/路由职责）。依赖 MySQL、Redis、MinIO，并使用 Nacos 注册发现（网关通过 `lb://` 路由到各服务）。

## 项目亮点
- **分层清晰**：对外统一走 Gateway HTTP（`/api/**`），对内仅用于服务间的 Dubbo RPC；避免把内部协议暴露给客户端。
- **现代微服务栈**：Java 17 + Spring Boot 3.2 + Spring Cloud 2023，多模块 Maven 工程；Gateway 使用 WebFlux，业务服务使用 MVC。
- **注册发现与流控治理**：Nacos 负责注册发现（可选配置中心）；Sentinel 支持限流/熔断与规则持久化（Nacos 数据源）。
- **Dubbo 最佳实践落地**：Dubbo 注册到 Nacos 独立 group（默认 `DUBBO_GROUP`）并使用独立应用名，避免网关把 Dubbo 实例当 HTTP 实例误路由。
- **统一安全入口**：网关集成 Spring Security（OAuth2 Resource Server）+ JWT（JJWT），对内服务只关注业务与鉴权信息透传。
- **统一接口与错误模型**：统一 `Response<T>` 返回结构与错误码语义；网关与服务端异常均返回一致 JSON，便于前端/调用方处理。
- **可观测一键就绪**：Actuator + Micrometer 指标；Prometheus/Grafana 看板；Jaeger 链路追踪（OTLP）；ELK 日志，通过 `docker-compose` 可快速拉起整套链路。
- **Tracing 防踩坑**：支持 Micrometer Tracing 与 OTel Javaagent 二选一，并通过开关避免“双重埋点”导致重复 span/trace 不一致；全链路统一 W3C `traceparent` 传播。
- **文件与对象存储能力**：MinIO 集成 + 预签名 URL，覆盖上传与访问的常见场景。
- **交付友好**：提供 Dockerfile、`k8s/` 清单与 Helm Chart；探针使用标准 `liveness/readiness`，便于在集群中稳定运行。
- **前端工程化**：Vue 3 + Vite + TypeScript + Element Plus + Axios，前后端分离开发，接口调用与异常兜底策略统一。

## 技术栈（当前实现）
### 后端
- Java 17、Spring Boot 3.2
- Spring Cloud 2023 + Spring Cloud Gateway（WebFlux）
- Spring Data JPA（Hibernate）、MySQL
- Spring Security（OAuth2 Resource Server）+ JWT（JJWT）
- springdoc-openapi（网关聚合 Swagger UI）
- Spring Cloud Alibaba：Nacos（注册发现/可选配置中心）、Sentinel（限流/熔断/规则持久化）
- Dubbo 3

### 前端
- Vue 3 + Vite + TypeScript + Element Plus + Axios

### 可观测与基础设施
- Spring Boot Actuator + Micrometer
- Prometheus + Grafana（HTTP 延迟直方图与分位数）
- OpenTelemetry（Micrometer OTLP 或 Javaagent 二选一）+ Jaeger
- Elasticsearch + Logstash + Kibana（ELK）
- Redis、MinIO

### 交付
- Docker Compose（本地一键拉起依赖）
- `k8s/` 清单与 `helm/` Chart

## 模块与端口
- Gateway：`sword-shield-gateway`（8080），将 `/api/{service}/**` 路由到本地服务。
- User：`sword-shield-user`（8101）
- Activity：`sword-shield-activity`（8102）
- Content：`sword-shield-content`（8103）
- Interaction：`sword-shield-interaction`（8104）
- File/MinIO：`sword-shield-file`（8105）
- Admin：`sword-shield-admin`（8106）
- API 契约：`sword-shield-api`（Jar，放 DTO/RPC 接口定义，无独立端口）
- 公共库：`sword-shield-common`（Jar，公共工具/异常/配置资源，无独立端口）

## 依赖环境
- JDK 17、Maven 3.9+
- Node.js 18+（仅前端）
- Docker + Docker Compose（本地启动依赖）

## 快速开始（本地开发）
### 1) 启动基础设施（推荐）
在仓库根目录执行：
```sh
docker-compose up -d
```
将启动 MySQL、Nacos、Redis、MinIO、Elasticsearch+Logstash+Kibana、Jaeger + OTel Collector、Sentinel、Prometheus + Grafana。
- MySQL：`root / 123456`，数据库 `sword_shield`，端口 `3306`
- Nacos：`8848`
- MinIO：`9000`（API）/ `9001`（Console），账号 `admin / admin123456`
- OTel Collector：OTLP gRPC `4317` / HTTP `4318`
- Jaeger UI：`16686`
- Sentinel Dashboard：`8858`
- Sentinel 规则：`docker-compose` 启动时 `sentinel-nacos-init` 会把初始网关流控规则写入 Nacos，Data ID `sword-shield-gateway-sentinel-flow`，Group `SENTINEL_GROUP`，内容见 `scripts/sentinel/gateway-flow.json`。

### 2) 构建后端
```sh
mvn clean install   # 如需跳过测试，加 -DskipTests
```

### 3) 运行后端（两种 tracing 模式二选一，避免双重埋点）
本项目支持两种链路追踪模式：**Micrometer（默认）** 或 **OpenTelemetry Javaagent（可选）**。请只选一种运行。

#### A. 推荐：Micrometer Tracing（默认）
直接在模块目录运行（示例 user）：
```sh
cd sword-shield-user
mvn spring-boot:run -DskipTests
```

建议启动顺序：先启动各业务服务，再启动网关（`sword-shield-gateway`）。

#### B. 可选：OpenTelemetry Javaagent（自动埋点）
使用脚本启动（示例 user）：
```sh
./scripts/run-with-otel-agent.sh sword-shield-user
```
说明：
- 需要 `otel/opentelemetry-javaagent.jar`（脚本会检查并提示下载方式）
- 脚本已默认设置 `MANAGEMENT_TRACING_ENABLED=false`，用于关闭 Spring Boot/Micrometer tracing，避免重复 span / trace 不一致
- 默认导出到 `http://localhost:4318`（OTel Collector，HTTP/protobuf）

端口如上；网关 8080。各服务默认会注册到 Nacos（`spring.cloud.nacos.discovery.enabled: true`），网关路由使用 `lb://sword-shield-*` 通过 Nacos 发现实例。

配置说明（集中化）：
- 各服务 `src/main/resources/application.yml` 会通过 `spring.config.import` 引入：
  - `classpath:common.yml`：统一 DB/Nacos/Sentinel/Jackson/MVC 默认值
  - `classpath:observability.yml`：Actuator/Micrometer/Tracing 默认值
  - `classpath:dubbo.yml`：Dubbo（仅内部 RPC）默认值
- 推荐通过环境变量覆盖（无需改代码）：
  - 数据库：`MYSQL_URL` / `MYSQL_USERNAME` / `MYSQL_PASSWORD`
  - 注册发现：`NACOS_SERVER_ADDR`
  - Sentinel：`SENTINEL_DASHBOARD` / `SENTINEL_GROUP_ID`
  - 时区：`APP_TIME_ZONE`（默认 `Asia/Shanghai`）
  - Tracing：`MANAGEMENT_TRACING_ENABLED` / `TRACING_SAMPLING_PROBABILITY` / `OTLP_TRACING_ENDPOINT`
  - Dubbo：`DUBBO_PORT`（本地默认随机端口；K8s/生产建议固定如 `20880`）、`DUBBO_NACOS_GROUP`（默认 `DUBBO_GROUP`）

健康检查与指标：
- 基本：`/actuator/health`、`/actuator/info`、`/actuator/prometheus`
- K8s 探针（更标准）：`/actuator/health/liveness`、`/actuator/health/readiness`
- 网关额外：`/actuator/gateway/routes`

Nacos 验证：
- 查看注册实例：`curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=sword-shield-user"`
- 网关健康：`curl http://localhost:8080/actuator/health`
- 如果未看到实例，先确保 Nacos 已启动，再重启对应服务/网关。

### 4) 运行前端（sword-shield-web-ui）
```sh
cd sword-shield-web-ui
npm install
npm run dev      # Vite 开发服务
npm run build    # 生产构建
```

## 可观测（Observability）
### 指标（Prometheus）
- 采集端点：各服务 `GET /actuator/prometheus`
- 本地 Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`（默认 `admin/admin`），可导入看板：Spring Boot 3 Micrometer `10280`、JVM `4701`、Gateway `12900`

### 链路追踪（Jaeger/OTLP）
- Jaeger UI：`http://localhost:16686`
- 默认使用 W3C `traceparent` 传播；同时保留 `X-Trace-Id` 作为兼容/排障头
- 关键环境变量：
  - `MANAGEMENT_TRACING_ENABLED=true|false`：关闭/开启 Micrometer tracing（Javaagent 模式建议 `false`）
  - `TRACING_SAMPLING_PROBABILITY=0.0~1.0`：采样率
  - `OTLP_TRACING_ENDPOINT=http://<collector>:4318/v1/traces`：Micrometer OTLP traces 上报地址

### 日志（ELK）
- Logstash 目的地址可配置：`LOGSTASH_DESTINATION=host:port`（k8s 默认 `logstash:5000`，本地默认 `localhost:15000`）
- Kibana：`http://127.0.0.1:5601`

## 部署（K8s / Helm）
- Docker 镜像构建：
  - 各后端服务 Dockerfile 使用多阶段缓存构建（先 `COPY pom.xml` 再离线下载依赖，再拷贝源码编译），可显著提升二次构建速度。
  - 注意：Docker build 的 context 必须是仓库根目录（因为 Dockerfile 会 `COPY` 根 `pom.xml` 与各模块 `pom.xml`）。
  - 示例（user 服务）：
    ```sh
    docker build -f sword-shield-user/Dockerfile -t sword-shield-user:dev .
    ```
- K8s 探针已使用：`/actuator/health/liveness`、`/actuator/health/readiness`
- Helm tracing 模式开关：`helm/sword-shield/values.yaml` 的 `observability.tracing.mode`
  - `micrometer`（默认）：使用 `MANAGEMENT_OTLP_TRACING_ENDPOINT`
  - `agent`：自动注入 `JAVA_AGENT_OPTS=-javaagent:/app/opentelemetry-javaagent.jar` 并关闭 Micrometer tracing

## 常用控制台/可视化地址
- MinIO 控制台：`http://localhost:9001/browser`（admin / admin123456）
- Nacos 控制台：`http://127.0.0.1:8848/nacos`
- Sentinel Dashboard：`http://localhost:8858`（默认账号/密码 `sentinel/sentinel`）
- Kibana（ES 可视化）：`http://127.0.0.1:5601`
- 网关聚合 Swagger UI：`http://localhost:8080/swagger-ui.html`（汇总各服务 OpenAPI）
- Jaeger（链路追踪）：`http://localhost:16686`
- Prometheus：`http://localhost:9090`
- Grafana：`http://localhost:3000`（默认账户 `admin/admin`，已预置 Prometheus 数据源） 可导入常用看板：Spring Boot 3 Micrometer `10280`、JVM `4701`、Gateway `12900`。

## 补充
统一返回结构：
- 统一使用 `Response<T>`：`{ "code": number, "message": string, "data": any }`
- `code=0` 表示成功；非 0 为错误（通常与语义一致的错误码，如 `400/401/403/404/429/500/503` 等）

时间与序列化：
- 默认时区：`APP_TIME_ZONE`（默认 `Asia/Shanghai`）
- Jackson：默认输出 ISO-8601 字符串（非时间戳），前端使用 `new Date(isoString)` 解析即可

Sentinel 规则持久化：
- Flow 规则存于 Nacos，Group=`SENTINEL_GROUP`，Data ID=`<spring.application.name>-sentinel-flow`（示例：`sword-shield-gateway-sentinel-flow`），格式 `json`。
- 典型规则 JSON：
  ```json
  [{
    "resource": "/api/user/**",
    "limitApp": "default",
    "grade": 1,
    "count": 50,
    "strategy": 0,
    "controlBehavior": 0
  }]
  ```
  `grade=1` 表示 QPS，`count` 为阈值。Dashboard 修改后会写回 Nacos，对应服务热更新。
