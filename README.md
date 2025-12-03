# sword-shield-web

基于 Java 17 的 Spring Boot 3.2 微服务套件，配合 Vue 3 + Vite 前端。服务通过 Spring Cloud Gateway 暴露，依赖 MySQL、Redis、MinIO，可选接入 Nacos。

## 模块与端口
- Gateway：`sword-shield-gateway`（8080），将 `/api/{service}/**` 路由到本地服务。
- User：`sword-shield-user`（8101）
- Activity：`sword-shield-activity`（8102）
- Content：`sword-shield-content`（8103）
- Interaction：`sword-shield-interaction`（8104）
- File/MinIO：`sword-shield-file`（8105）
- Admin：`sword-shield-admin`（8106）
- 公共库：`sword-shield-common`

## 依赖环境
- JDK 17、Maven 3.9+
- Node.js 18+（仅前端）
- Docker + Docker Compose（本地启动依赖）

## 启动基础设施（推荐）
在仓库根目录执行：
```sh
docker-compose up -d
```
将启动 MySQL、Nacos、Redis、MinIO、Elasticsearch+Logstash+Kibana。
- MySQL：`root / 123456`，数据库 `sword_shield`，端口 `3306`
- Nacos：`8848`
- MinIO：`9000`（API）/ `9001`（Console），账号 `admin / admin123456`

## 后端构建与运行
构建所有模块：
```sh
mvn clean package      # 如需跳过测试，加 -DskipTests
```

运行单个服务（示例 user）：
```sh
mvn spring-boot:run -pl sword-shield-user -am
```
端口如上；网关 8080。Nacos 依赖存在但在默认 `application.yml` 中关闭，路由静态指向本地端口。

配置说明：`src/main/resources/application.yml` 默认的 DB/Nacos/MinIO 地址为 `172.25.255.76`，本地运行请改为 `localhost` 或环境对应地址，或通过环境变量/启动参数覆盖。

健康检查与指标（若模块开启 actuator）：`/actuator/health`、`/actuator/info`、`/actuator/prometheus`。

## 前端（sword-shield-web-ui）
```sh
cd sword-shield-web-ui
npm install
npm run dev      # Vite 开发服务
npm run build    # 生产构建
```

## 开发流程建议
1) `docker-compose up -d` 启动依赖。
2) 按需修改各服务 `application.yml` 的 DB/Nacos/MinIO 地址/凭据。
3) `mvn spring-boot:run -pl <module> -am` 启动需要的服务；网关在 8080。
4) 前端 `npm run dev`，默认端口 5173。
5) 验证：`curl http://localhost:8080/actuator/health`。
