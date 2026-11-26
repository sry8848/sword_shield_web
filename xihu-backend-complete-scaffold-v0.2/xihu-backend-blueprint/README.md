# 西湖论剑后端架构规范与技术骨架

本目录的 `docs/` 是后端架构、工程与编码约束的唯一维护位置。各主题以本索引指向的规范章节为准，其他说明链接到原章节。骨架用于验证构建与服务启动。

## 阅读顺序

1. [总体架构与数据所有权](docs/01-总体架构与数据所有权.md)规定领域职责、服务依赖、数据所有权与事务边界。
2. [工程与编码规范](docs/02-工程与编码规范.md)规定分层、API 契约管理、数据访问、配置、日志、异常与测试。
3. [认证、授权、租户、组织与审批](docs/03-权限数据范围组织租户审批.md)规定身份与授权职责、数据范围及启用未来能力时的安全要求。
4. [待确认事项与契约差异](docs/11-现有前端OpenAPI契约差异与冻结项.md)记录尚未达成一致的事项及其来源。开发对应功能前核对相关条目。
5. [实现参考与历史取舍](docs/07-参考开源实现与取舍.md)收录可选实现和开源参考。采用前按实际业务验证。

前三份是架构约束。待确认事项记录决策缺口，实现参考提供候选路径。

## 资料的权威边界

| 资料                                                                            | 负责的问题                                        |
| ----------------------------------------------------------------------------- | -------------------------------------------- |
| [MVP](../../docs/mvp.md)                                                      | 第一阶段业务范围与交付闭环。                               |
| [完整产品功能清单](../../docs/功能模块清单与微服务划分评审.md)及[菜单与页面功能清单](../../docs/菜单与页面功能清单.md) | 产品完整功能规划与页面组织。服务划分的现行约束见阅读顺序第 1 项。           |
| [OpenAPI](../../xihu-jian-frontend/openapi/openapi.yaml)                      | 对外 API 的唯一机器可读契约。当前文件处于评审阶段，标为待确认的业务规则随评审收敛。 |
| 后端架构规范                                                                        | 后端的领域边界、工程和编码约束。                             |
| 后端代码                                                                          | 上述要求的具体实现。演示实现与已确认规范有差异时，以对应权威资料判断实施缺口。      |

MVP 缩小首期交付范围，各项工程和安全约束仍适用。对未来模块先划清职责，等业务需要再增加代码、运行服务和基础设施。多租户、组织、审批等能力的条件性约束在各自启用时适用。

遇到资料冲突时，暂停受影响的实现，在[待确认事项](docs/11-现有前端OpenAPI契约差异与冻结项.md)记录相关出处、影响和需确认的问题。确认后由对应资料维护其结论。涉及对外接口时按 [API 契约与异常](docs/02-工程与编码规范.md#api-契约与异常)处理。

## 骨架实现状态与本地验证

| Maven 模块 | 当前内容 | 默认端口 |
| --- | --- | --- |
| `gateway` | WebFlux 网关、Portal 认证路由、健康检查和请求追踪 | 8080 |
| `identity-service` | MyBatis-Plus 与 Flyway、Portal 登录和会话、Admin 服务内会话接口、健康检查和请求追踪 | 8081 |
| `activity-service` | 启动入口、健康检查、MVC 错误响应和请求追踪 | 8082 |
| `common` | 框架无关的成功响应、业务失败类型和 UUIDv7 生成入口 | — |
| `common-servlet-web` | Identity 与 Activity 显式导入的 MVC 异常处理、校验和 TraceId | — |

Admin 的 Gateway ForwardAuth 及前端联调、Activity 的业务接口随用例落地。历史演示的交付记录见 [VALIDATION.md](VALIDATION.md)。

本地验证需要 JDK 17 或以上版本和 Maven。在 `backend/` 执行：

```powershell
mvn -B clean verify
```

构建完成后可分别运行以下命令，每个进程占用一个终端：

```powershell
java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar
java -jar identity-service/target/identity-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=dev
java -jar activity-service/target/activity-service-0.1.0-SNAPSHOT.jar
```

服务启动后，分别访问 `http://localhost:8080/actuator/health`、`http://localhost:8081/actuator/health` 和 `http://localhost:8082/actuator/health`。

### Identity 数据库与首位管理员

提前为各环境创建独立的 MySQL 8 数据库，选择一个 Spring Profile 并提供相应环境变量。

| Profile | JDBC URL | 用户名 | 密码 |
| --- | --- | --- | --- |
| `dev` | `IDENTITY_DEV_DB_URL` | `IDENTITY_DEV_DB_USERNAME` | `IDENTITY_DEV_DB_PASSWORD` |
| `test` | `IDENTITY_TEST_DB_URL` | `IDENTITY_TEST_DB_USERNAME` | `IDENTITY_TEST_DB_PASSWORD` |
| `prod` | `IDENTITY_PROD_DB_URL` | `IDENTITY_PROD_DB_USERNAME` | `IDENTITY_PROD_DB_PASSWORD` |

**UUIDv7 开发基线。** 若本地库已执行旧主键迁移，先确认 `dev` Profile、JDBC 主机和库名，停止连接该库的旧版 Identity，重新创建可丢弃的本地 Schema，或创建一个新的空 Schema 并调整 `IDENTITY_DEV_DB_URL`。启动新版后 Flyway 会重建表并写入固定测试种子。旧数据不做转换。验证用的 `test` 库也从空库初始化。发布后的库通过新增迁移版本演进。

设置 `test` 的连接变量后，`mvn -B verify` 会运行真实 MySQL 集成测试。`dev` 和 `test` 的普通账号为 `13800000001`，密码 `LocalPortal-Only!2026`。测试管理员为 `13800000002`，密码 `LocalAdmin-Only!2026`。

生产环境正式迁移完成后，在交互式终端离线生成管理员 UUIDv7 和哈希。

```powershell
java -jar identity-service/target/identity-service-0.1.0-SNAPSHOT.jar --generate-id
java -jar identity-service/target/identity-service-0.1.0-SNAPSHOT.jar --encode-password
```

把[生产预置模板](backend/identity-service/ops/sql/provision_super_admin.sql)复制到仓库外，填写刚生成的 UUIDv7、负责人手机号和哈希。在 `cmd.exe` 使用 MySQL 批处理执行模板，`--no-defaults` 避免本机选项文件改变遇错退出行为。

```bat
mysql --no-defaults --host=PROD_DB_HOST --user=PROVISIONER --password --database=IDENTITY_DB < C:\secure\provision_super_admin.sql
```

表字段、约束及版本策略见[账号基础数据建模变更记录](../../docs/设计/登录设计/账号基础数据建模/账号基础数据建模变更记录.md)的最新版本。
