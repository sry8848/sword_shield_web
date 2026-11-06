# 西湖论剑大会门户 · 前端工程骨架

这是一个可运行的 Vue 3 + TypeScript 前端 Monorepo 起始工程，包含大会前台、运营后台、共享 API 基础设施、Mock 数据和开发规范。

> 当前交付的是工程骨架和示例业务链路，不是 170 项功能的完整实现。真实后端、真实登录、支付、短信、文件存储和生产级权限仍需按后端接口契约接入。

## 1. 工程结构

```text
apps/portal/       大会前台，默认 http://localhost:5173
apps/admin/        运营后台，默认 http://localhost:5174
packages/shared/   两端共享的领域类型、HTTP 客户端、API 和 Mock
docs/              架构、编码、接口、UI、测试、AI 协作等规范
openapi/           MVP 接口契约草案
tests/e2e/         Playwright 冒烟测试
```

采用 pnpm workspace。两个应用独立启动、独立构建；共享基础能力通过 `@xhlj/shared` 复用。

## 2. 环境要求

- Node.js `>=22.12.0`
- pnpm `10.x`
- 推荐使用 Windows 11 + PowerShell、macOS 或 Linux。
- 安装依赖需要能够访问 npm registry。

Vite 当前文档要求 Node.js 20.19+ 或 22.12+；本项目选用 Node 22.12+。参考：https://vite.dev/guide/

## 3. 安装与启动

```bash
corepack enable
corepack prepare pnpm@10.0.0 --activate
pnpm install
```

启动大会前台：`pnpm dev:portal`

启动运营后台：`pnpm dev:admin`

默认 Mock 已开启。前台和后台分别使用自己的浏览器运行时 Mock 状态；它们不是共享数据库，两个应用之间的数据不会自动同步。

## 4. 演示账号

Mock 模式下使用任意非空密码即可：

| 应用 | 用户名 | 密码 | 角色 |
|---|---|---|---|
| 大会前台 | `user` | `user123` | USER |
| 运营后台 | `admin` | `admin123` | ADMIN |

这些账号只用于本地演示，不得用于生产环境。

## 5. 常用命令

```bash
pnpm dev:portal
pnpm dev:admin
pnpm build
pnpm typecheck
pnpm lint
pnpm lint:fix
pnpm test
pnpm test:e2e
```

首次运行 E2E 测试前安装浏览器：`pnpm exec playwright install chromium`。

## 6. Mock 与真实后端切换

两个应用分别配置：

- `VITE_USE_MOCK=true`：启用 Mock Adapter。
- `VITE_USE_MOCK=false`：请求真实 API。
- `VITE_API_BASE_URL`：API 基础地址，默认空字符串，使用同源 `/api`。
- `VITE_PROXY_TARGET`：仅 Vite 开发代理使用，默认 `http://localhost:8080`。

`VITE_` 前缀变量会进入浏览器构建产物，禁止放入密码、私钥和服务端密钥。

真实后端接入前，必须与后端确认 `openapi/openapi.yaml` 中的响应包装、错误码、认证方式、分页结构和业务状态。当前 OpenAPI 是建议契约，不代表现有后端已经实现。

## 7. 当前骨架包含的示例

前台：首页、活动列表、活动详情、登录、我的报名。

后台：登录、运营概览、活动管理（列表、创建、发布）、报名管理、现场核验、基础统计。

共享层：Axios 实例工厂、统一 API 错误类型、响应解包、领域类型、API 方法和 Mock Adapter。

## 8. 当前明确不包含

真实 Spring Boot / Gateway 接口实现、生产级认证和 Refresh Token、完整 RBAC / DataScope、支付退款、短信邮件、直播服务、全部 170 项功能、生产部署流水线。

## 9. 依赖策略

`package.json` 使用兼容的主版本范围，不附带伪造的 lockfile。首次执行 `pnpm install` 后，应提交生成的 `pnpm-lock.yaml`，团队后续统一使用 `pnpm install --frozen-lockfile`。
