# 西湖论剑大会门户 · 前端工程骨架

这是一个 Vue 3 + TypeScript 前端 Monorepo 骨架，包含大会前台、运营后台、共享 HTTP 基础设施、Mock 数据和开发规范。

> 当前代码包含骨架和示例链路。后续开发按 [前端架构基线](docs/01-技术选型与架构设计.md)执行，已实现能力和待迁移事项见 [骨架整改清单](docs/14-待确认事项.md#前端骨架后续整改)。

## 1. 工程结构

```text
apps/portal/       大会前台，默认 http://localhost:5173
apps/admin/        运营后台，默认 http://localhost:5174
packages/shared/   当前包含 HTTP 客户端、集中式业务 API、手写类型和 Mock
docs/              架构、编码、接口、UI、测试、AI 协作等规范
openapi/           MVP 接口契约草案
tests/e2e/         Playwright 冒烟测试
```

两个应用已可独立启动和构建，通过 `@xhlj/shared` 复用基础能力。目标 Feature 目录和共享边界见 [工程结构规范](docs/02-工程结构规范.md)。

## 2. 环境要求

- Node.js `>=22.12.0`
- pnpm `10.x`
- 推荐使用 Windows 11 + PowerShell、macOS 或 Linux。
- 安装依赖需要能够访问 npm registry。

## 3. 安装与启动

当前骨架尚无已提交的锁文件。以下普通安装命令用于启动现有骨架，锁文件入库后按 [开发与部署规范](docs/11-开发与部署规范.md#1-本地环境)使用冻结安装。

```bash
corepack enable
corepack prepare pnpm@10.0.0 --activate
pnpm install
```

启动大会前台：`pnpm dev:portal`

启动运营后台：`pnpm dev:admin`

默认 Mock 已开启。前台和后台分别使用自己的浏览器运行时 Mock 状态；它们不是共享数据库，两个应用之间的数据不会自动同步。

## 4. 演示账号

Mock 模式下使用以下演示账号和对应密码。

| 应用 | 用户名 | 密码 | 角色 |
|---|---|---|---|
| 大会前台 | `user` | `user123` | 参会用户 |
| 运营后台 | `admin` | `admin123` | `SUPER_ADMIN` |
| 运营后台 | `checkin` | `checkin123` | `CHECKIN_STAFF` |

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

[OpenAPI](openapi/openapi.yaml) 是接口 DTO 的唯一契约来源，当前版本处于评审阶段。接入真实后端前，按 [接口与数据规范](docs/06-接口与数据规范.md#13-接入现有后端时的处理)核对实现。

## 7. 当前骨架包含的示例

前台：首页、活动列表、活动详情、登录、我的报名。

后台：登录、运营概览、活动管理（列表、创建、发布）、报名管理、现场核验、基础统计。

共享层已提供 Axios 实例工厂、令牌注入和刷新协调、错误转换、响应解包、手写类型、集中式 API 和 Mock Adapter。

## 8. 规范入口

从 [项目总览与执行约定](docs/00-项目总览与执行约定.md)查看规范分工。接入实际业务前阅读 [业务模块接入指南](docs/12-业务模块接入指南.md)。
