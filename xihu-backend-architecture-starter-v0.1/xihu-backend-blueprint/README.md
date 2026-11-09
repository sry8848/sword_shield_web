# 西湖论剑后端架构与工程骨架（独立交付包）

> 设计基准：`sry8848/sword_shield_web` 默认 `dev` 分支的旧工程 + 会话中已形成的 MVP、功能划分、前端接口规范及 `openapi.yaml` 文档副本。
> **本包没有覆盖 GitHub 仓库，也不是已完成 MVP。** 它包含架构决策、开发规范、扩展性设计和最小可联调的多模块示范工程。

## 交付内容

- `docs/00-决策记录与范围.md`：冻结/待冻结、事实来源、不可背离的边界。
- `docs/01-总体架构与数据所有权.md`：服务、模块、依赖图、调用、事务和数据归属。
- `docs/02-工程与编码规范.md`：Java、API、数据库、日志、配置、安全、测试。
- `docs/03-权限数据范围组织租户审批.md`：多资源授权、未来部门、审批、租户的增量路线。
- `docs/04-MVP到完整版迁移表.md`：对旧项目及现有 OpenAPI 的逐项改造。
- `docs/05-验收与缺口.md`：当前真实实现、测试计划、开工门槛。
- `docs/06-权限关系表与接口约定.md`：多资源授权表与接口设计。
- `docs/07-参考开源实现与取舍.md`：参考方案与取舍依据。
- `backend/`：Maven 多模块示范工程，含 common、gateway、identity、activity。

## 本次示范工程与完整版的区别

| 能力      | 本包实际实现                                                      | 后续业务完成条件                                     |
| ------- | ----------------------------------------------------------- | -------------------------------------------- |
| Gateway | 本地静态路由                                                      | 配置中心/服务发现/限流按需补充                             |
| JWT     | identity 签发 HS256 演示 Token、activity 校验                      | 生产切换 RS256/密钥托管、refresh、撤权、租户成员读取            |
| 用户/角色   | identity 演示账号与权限内存映射                                        | 将角色、权限、组织成员关系转到数据库与缓存并建立变更生效机制               |
| 数据权限    | 真实 `Permission + resource-type + operation + scope` 的演示授权服务 | 增加数据库资源授权与所有列表/统计查询的安全约束                     |
| 活动      | 真实 MySQL 表 + MyBatis-Plus Mapper，查询、草稿创建、发布（演示）             | 完善大会、嘉宾、会场、报名、凭证、核验、统计业务链路                   |
| 租户      | 请求身份中保留 `tenantId` 结构，activity 租户过滤                         | 完整 tenant member、SQL/文件/缓存/MQ 安全隔离测试后方可宣称多租户 |
| 审批/组织   | 仅扩展边界和示例策略                                                  | 按需求创建组织模型、审批实例与版本化草稿                         |

**禁止把这里的 JWT 演示、单租户默认值或 scope 示例当作生产多租户安全实现。**

## 本地运行（Windows PowerShell）

前提：JDK 17、Maven 3.9+、Docker Desktop。项目保留 Java 17 / Spring Boot 3.2.5 / Spring Cloud 2023.0.3，与现有代码基线一致；上线前必须核对安全补丁和版本兼容性。

```powershell
cd backend
# 启动 MySQL
docker compose up -d mysql
# 编译、运行单元测试并安装本地模块
mvn -B clean install
```

然后分别打开三个 PowerShell 终端，在 `backend` 目录下启动服务：

```powershell
# 终端一
cd identity-service
mvn spring-boot:run

# 终端二（从 backend 目录进入）
cd activity-service
mvn spring-boot:run

# 终端三（从 backend 目录进入）
cd gateway
mvn spring-boot:run
```

三个服务分别使用 8081、8082、8080 端口；MySQL 由 Docker 发布到宿主机的 3307 端口。本地启动顺序建议 MySQL → Identity → Activity → Gateway。

测试身份（仅示例运行）：`admin` / `DemoPass123!`。JWT 共享密钥由 `APP_JWT_SECRET` 提供（需至少 32 字节），**不能把开发默认密钥用于部署**。为本地演示便捷性提供的默认值必须由部署环境覆盖。

```powershell
$body = @{ username='admin'; password='DemoPass123!' } | ConvertTo-Json
$session = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/login' -ContentType 'application/json' -Body $body
$token = $session.data.accessToken
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/admin/me' -Headers @{ Authorization="Bearer $token" }
Invoke-RestMethod -Uri 'http://localhost:8080/api/v1/public/events'
```

此演示尚未完成原始 OpenAPI 的所有接口，**不能直接替换现有前端后端**。完整端点对照见 `docs/04-MVP到完整版迁移表.md`。

## 验证情况

当前工作环境有 JDK 21，但没有 `mvn`、Docker CLI，外网依赖下载亦不可用。因此交付前做了文件、XML、代码结构的静态校验；**尚未在本环境实际完成 Maven 构建和启动运行**。必须在 Windows 环境执行 `mvn verify`，修复编译/集成问题后再将其作为可运行工程验收。
