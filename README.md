# sword-shield-web

基于 Java 17 的 Spring Boot 3.2 微服务套件，配合 Vue 3 + Vite 前端。服务通过 Spring Cloud Gateway 暴露，依赖 MySQL、Redis、MinIO，并使用 Nacos 进行注册发现（网关通过 `lb://` 路由到各服务）。

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
mvn clean install      #
```

运行单个服务（示例 user）：

```sh
cd sword-shield-user && mvn spring-boot:run
```

端口如上；网关 8080。各服务默认会注册到 Nacos（`spring.cloud.nacos.discovery.enabled: true`），网关路由使用 `lb://sword-shield-*` 通过 Nacos 发现实例。

配置说明：`src/main/resources/application.yml` 默认 DB/Nacos/MinIO 地址均为 `localhost`。如在容器或远程环境运行，请改为对应地址，或通过环境变量/启动参数覆盖（例如 `--spring.cloud.nacos.discovery.server-addr=...`）。

健康检查与指标：`/actuator/health`、`/actuator/info`、`/actuator/prometheus`。网关额外暴露 `/actuator/gateway/routes` 用于查看路由。

Nacos 验证：

- 查看注册实例：`curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=sword-shield-user"`
- 网关健康：`curl http://localhost:8080/actuator/health`
- 如果未看到实例，先确保 Nacos 已启动，再重启对应服务/网关。

## 前端（sword-shield-web-ui）

```sh
cd sword-shield-web-ui
npm install
npm run dev      # Vite 开发服务
npm run build    # 生产构建
```

## 开发流程建议

1) 拉取仓库`git clone -b dev https://github.com/Penryn/sword_shield_web.git`
2) 把解压包的data、db、logs文件夹复制到当前目录并运行`docker-compose up -d` 启动依赖。
3) 后端首先在根目录执行`mvn clean package && mvn clean install`然后`cd <module> &&mvn spring-boot:run` 依次启动所有服务，最后启动网关。（上面`模块与端口`带有端口的就是可以启动的服务）
4) 前端 `cd sword-shield-web-ui && npm i && npm run dev`，默认端口 5173。
5) 验证：浏览器访问`http://localhost:5173`。
6) 保存本地：`git fetch && git add . && git commit -m "本次更改内容" `
7) 上传github：`git push`
