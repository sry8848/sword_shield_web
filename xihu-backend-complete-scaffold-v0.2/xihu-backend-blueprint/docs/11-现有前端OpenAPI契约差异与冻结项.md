# 11 前端、MVP OpenAPI 与本工程差异清单

基准：随会话提供的 `openapi.yaml` 文档副本（125263 bytes）、MVP.md、现有 `dev` 分支旧工程。**本包没有修改原前端，也没有宣称 API 已完全兼容。**

## 1. 当前 OpenAPI 业务接口覆盖

```
POST /auth/register             POST /auth/login
POST /auth/refresh              POST /auth/logout
GET  /auth/me                   GET  /public/events
GET  /public/events/{eventId}  POST /events/{eventId}/registrations
GET  /me/registrations         GET  /me/registrations/{registrationId}
GET  /me/registrations/{registrationId}/credential
POST/GET /admin/events         GET/PATCH /admin/events/{eventId}
POST /admin/events/{eventId}/publish
POST /admin/events/{eventId}/unpublish
GET  /admin/registrations      GET /admin/registrations/{registrationId}
POST/GET /admin/check-ins      GET /admin/statistics/overview
GET /admin/statistics/events  GET /admin/statistics/events/{eventId}
```

所有路径以 `/api/v1` 为前缀。

## 2. 本骨架中的实现状态（必须按照真实状态报）

- `/api/v1/auth/login`：演示管理员内存账号、JWT；与原 OpenAPI 所要求的真实账号注册/刷新协议**不完整兼容**。
- `/api/v1/admin/me`：演示用户、角色、权限；未实现数据库动态 RBAC。
- `/api/v1/public/events`：简化只读已发布活动，响应字段未覆盖正式前台需求。
- `/api/v1/admin/events`：简化活动草稿创建。
- `/api/v1/admin/events/{id}/publish`：演示发布动作。
- 其它 MVP 端点：**未实现**。

## 3. 开发前必须冻结的契约差异

1. MVP 文档 3 个后台角色，现有 OpenAPI/规范含 `DATA_OPERATOR`：统一角色矩阵；角色代码不可永久固定在枚举中。
2. 登陆请求是普通用户账号还是后台独立账号，以及 refresh 会话结构，需要由身份服务统一。
3. 核验员被授权的场次：角色共享范围或团队授权，核验接口必须服务端检查，当前 OpenAPI 缺少配置端点。
4. `resource:action` 稳定授权码与多资源数据范围必须以文档 09 为准；不能以 JWT 中静态长寿命的范围快照作为唯一授权依据。
5. 注册、报名并发名额、凭证 QR preview/confirm 与统计口径，以已确认的业务决策为准修订 OpenAPI。
6. SQL Server 在早期赛题中提及，而骨架默认 MySQL：课程硬性验收条件先核实。

## 4. 规则

OpenAPI 仍是前后端的单一机器可读契约；文档改动应先在仓库评审提交，再改 Controller，不能直接用新示范接口替代正式契约。
