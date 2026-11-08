# 西湖论剑后端完整领域骨架 V0.2（独立交付，不覆盖旧仓库）

> 基准：用户确认的完整功能清单、MVP/OpenAPI 副本、2026-11-08 查询到的 `sry8848/sword_shield_web` `dev` 分支旧工程。旧工程是 Java 17 + Spring Boot 3.2.5 + Gateway/Dubbo；它与讨论中的新 API 文档并非相同提交。本包属于设计交付，不会替换仓库。

## 交付成果和真实实现状态

**14 个 Maven 模块：** `common`, `gateway`, `identity-service`, `activity-service`, `content-service`, `file-service`, `interaction-service`, `incentive-service`, `limited-seat-service`, `payment-service`, `customer-service`, `analytics-service`, `workflow-service`, `organization-service`。

- **已有 Spring 技术演示：** `gateway`、`identity-service`、`activity-service`，沿用 V0.1 的结构与示例；能配置启动，但尚未在当前环境完成 Spring Maven 构建/联调。
- **真实基础库：** `common`，含 API envelope、错误结构与授权纯 Java 模型。新增 `AuthorizationRequest`/`AuthorizationGrant`/`AuthorizedQuerySpec`，修正了通用 SELF 不能直接比较资源 ID 的建模缺陷。
- **新增未来领域：** 10 个 `jar` 模块，均包含领域值对象、仓储 Port、应用层授权契约和分层目录约束；它们不是空 Maven POM，也不是已经实现的服务，更没有 Spring 启动类。
- **完整文档：** 原有 00–07 规范加上 08–11（完整领域模块矩阵、通用资源授权运行模型、未来重构明细、API 契约差异）。
- **自检：** Java 17 语言级纯 Java 领域模块编译；范围判定测试通过。`mvn clean verify`、HTTP/DB 联调未跑通，详见 VALIDATION。

## 推荐阅读顺序

1. `docs/08-完整领域模块矩阵与依赖.md`：所有业务模块、边界与数据所有者。
2. `docs/00-决策记录与范围.md` 与 `docs/01-总体架构与数据所有权.md`：架构取舍。
3. `docs/02-工程与编码规范.md`：团队统一编码规范。
4. `docs/09-数据权限运行模型.md`：**跨资源（场次、用户、报名等）数据范围**。
5. `docs/10-多租户组织审批演进与重构明细.md`：真实升级时哪些需要改。
6. `docs/11-现有前端OpenAPI契约差异与冻结项.md`：与原前端接口规范对齐的必要工作。
7. 各未来服务的 `MODULE.md`：领域具体扩展责任。

## 依赖与工程使用

```text
backend/pom.xml               # Maven Reactor 14 modules
backend/common                # 纯通用值对象/响应
backend/gateway               # 本地静态路由演示
backend/identity-service      # 演示账号和 JWT（不是正式认证系统）
backend/activity-service      # MySQL + Flyway + MyBatis 活动示例
backend/{其余10个}-service     # 可编译领域契约，按需升级为启动应用
```

本项目未来目标虽保留分布式架构设计，但**Maven module ≠ 需要现在部署的微服务**。本地 MVP 开发不需要启动十个无业务逻辑的进程。

### Windows 本地验证

前提：JDK 17+、Maven 3.9+；若要启动 activity 服务，再准备 Docker Desktop。

```powershell
cd backend
mvn -B -U clean verify
# 如需要启动演示 MySQL
# docker compose up -d mysql
# 分别执行：mvn -pl identity-service spring-boot:run、mvn -pl activity-service spring-boot:run、mvn -pl gateway spring-boot:run
```

**不要把演示管理员凭证、HS256 共享密钥、内存授权数据用于公网或正式系统。** `mvn clean verify` 成功之后仍要执行服务联调、Flyway、跨租户/资源越权测试。

## 与原 OpenAPI 的关系

原 `openapi.yaml` 是唯一正式 API 契约；本工程仍是**技术骨架**，MVP 所需用户注册、报名、QR、核验、统计并未完整实现，参见 `docs/11-现有前端OpenAPI契约差异与冻结项.md`。不要直接用本包替换现有前后端。

## 非目标

- 不提前实现完整组织树/Flowable/租户管理后台。
- 不以创建未来服务文件夹冒充业务实现。
- 不绕开未确认的 SQL Server 赛题条件，当前示例暂按 MySQL。
- 不把 `tenantId` 字段本身等同于安全的 SaaS 隔离。
- 不宣称本项目已经完成 Maven 构建验证。
