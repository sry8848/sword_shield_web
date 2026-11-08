# 04 从现有 MVP/旧微服务到完整目标：逐项迁移

## 当前仓库事实

`dev` 根 pom 为 9 个模块：common、api、gateway、user、activity、content、interaction、file、admin。旧 README：Gateway + Dubbo + Nacos、Nacos、RocketMQ、MinIO。现有评审指出 Admin 跨模块直接操作 Activity 表；这点需要修正。

## OpenAPI 契约基线

已有上传的 `openapi.yaml`（0.1.0-draft）包含 `/auth/register`, `/auth/login`, `/admin/me`, `/admin/users/{userId}/roles`, `/admin/roles`, `/admin/permissions` 等。`AssignRolesRequest.roleCodes` 仍是 `SUPER_ADMIN/EVENT_OPERATOR/CHECKIN_STAFF/DATA_OPERATOR` 枚举。未来动态角色需移除开放 API 的硬编码枚举限制。

## 迁移优先级

| 序 | 旧实现/草案 | 新职责 | 要改什么 | 是否破坏业务 |
|---|---|---|---|---|
| 1 | Admin 同时管认证和直接写活动 | 身份由 Identity 管、活动由 Activity 管 | 调整 admin controller 到领域应用服务，禁跨服务 Mapper | 可能改变旧 Admin API 的调用路线，不应改变外部路径 |
| 2 | RoleCode 固定枚举 | 动态角色的稳定字符串 code | DTO/校验/角色字典修改 | API 类型需变更，业务权限码可保持 |
| 3 | `checkin:create` 只有功能权限 | 功能+资源范围+凭证状态 | 添加范围策略与授权持久化 | 需补充查询、核验与测试 |
| 4 | 报名仅依赖场次条件 | 原子名额 + 唯一约束 + 条件变更 | Activity 事务、DAO 与索引 | 业务正确性修复 |
| 5 | 旧系统单用户/单组织假设 | 组织/部门关系扩展 | 增 org/member/org_role/org_owner，改变权限解析，不改变权限码 | 中等；不可承诺零重构 |
| 6 | 直接发布 | 审批后受保护发布 | 增 draft version、approval、handler，新增提交/审核接口 | 内容域中等改造 |
| 7 | 单客户部署 | 多租户共享表 | 账号租户成员、tenantId、查询/缓存/文件/MQ 全链路审计 | 高；越早实施越容易 |
| 8 | 静态前端导航 | 可选动态菜单 | 增 menu/role_menu、响应 menus；权限码不变 | 前端路由层改造，后端业务不动 |

## 代码骨架与既有文档的差异

本包 `backend/` 是最小演示：`/auth/login`、`/admin/me`、`/public/events`、`/admin/events`。它**不实现**已有 OpenAPI 全量接口，也不符合“全部 MVP 完成”的验收条件；需要以 OpenAPI 逐项补齐接口。

为避免成本较高的隐式改变：
1. 新骨架暂保留老 Java 17/Boot 3.2 基线。
2. 保留 Gateway + 领域服务分工；取消 Admin 跨域直连表。
3. 旧代码的 Dubbo 不直接复制成无业务用途的 RPC 模块；确定真实跨服务用例后加 adapter。
4. 内容/文件服务属于目标设计，但不提前生成空 Spring 服务占用资源。
5. 新骨架使用 MyBatis-Plus 基础 Mapper 与显式 tenant_id 条件。后续共享式多租户必须引入经过验证的全链路 TenantContext 与隔离插件，并进行真实 MySQL 集成测试。
