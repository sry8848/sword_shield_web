# 西湖论剑大会门户 · 前端工程骨架

这是一个 Vue 3 + TypeScript 前端 Monorepo 骨架，包含大会前台、运营后台、共享 HTTP 基础设施和开发规范。

> 当前两端入口仅用于验证应用启动。后续开发按 [前端架构基线](docs/01-技术选型与架构设计.md)执行，已实现能力和待迁移事项见 [骨架整改清单](docs/14-待确认事项.md#前端骨架后续整改)。

## 1. 工程结构

```text
apps/portal/       大会前台，默认 http://localhost:5173
apps/admin/        运营后台，默认 http://localhost:5174
packages/shared/   Axios HTTP 客户端、错误处理和大会时区工具
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

## 4. 常用命令

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

## 5. 接口接入

两端 Vite 开发服务器为 `/api` 保留代理，`VITE_PROXY_TARGET` 默认指向 `http://localhost:8080`。`VITE_API_BASE_URL` 是后续接入应用 HTTP 实例时使用的配置项，留空表示同源。当前入口没有业务请求。

`VITE_` 前缀变量会进入浏览器构建产物，禁止放入密码、私钥和服务端密钥。

[OpenAPI](openapi/openapi.yaml) 是接口 DTO 的唯一契约来源，当前版本处于评审阶段。接入真实后端前，按 [接口与数据规范](docs/06-接口与数据规范.md#13-接入现有后端时的处理)核对实现。

## 6. 骨架保留的能力

- Portal 和 Admin 各有独立的 Vue 入口、Vite 配置、全局样式和构建命令。
- Shared 提供 Axios 客户端、认证回调、并发刷新协调、响应解包、错误转换和大会时区格式化。认证回调由后续实际接入的应用提供。
- Vitest 验证共享 HTTP 的刷新和错误行为，Playwright 验证两个应用可在浏览器中挂载。

## 7. 规范入口

从 [项目总览与执行约定](docs/00-项目总览与执行约定.md)查看规范分工。接入实际业务前阅读 [业务模块接入指南](docs/12-业务模块接入指南.md)。
