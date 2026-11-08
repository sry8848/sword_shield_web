# V0.2 交付验证与未通过项

## 实际通过

- `javac --release 17` 编译 10 个新增领域模块及公共纯 Java 范围模型：通过。
- `java ScopeContractCheck`：原有用户本人、指定场次、资源类型分离规则通过。
- `java ExtendedAuthorizationCheck`：6 个断言通过，覆盖 ownerId SELF、E01/E02 指定范围、E03 拒绝、跨租户不匹配、错误资源类型拒绝。
- XML 解析全部 15 个 pom（父工程 + 14 子模块）：通过。
- PyYAML 解析所有 YAML：通过。
- Maven Reactor 的 14 个模块均有对应 pom.xml：通过。
- 12 篇架构规范和 10 篇未来领域 MODULE.md 存在：通过。

## 未进行 / 不可冒充已通过

- 没有 `mvn` / Docker CLI；Spring Boot 完整 Maven 构建和实际启动、Flyway + MySQL HTTP 联调**未验证**。
- 身份模块仍为本地演示账号，不具备真实动态 RBAC、用户注册、refresh、组织或租户成员。
- Activity 目前仅示例场次创建与发布，MVP 报名、凭证、核验、报表仍未实现。
- 多资源数据范围新增纯模型，不代表数据库/复杂 SQL 已实施授权过滤。
- 10 个未来领域仅编译级契约，不包含业务 Controller、Mapper、独立进程或数据迁移。
- 与正式 `openapi.yaml` 尚有端点和 DTO 差异，不应直接替换旧仓库。

## Windows 验收入口

```powershell
cd backend
mvn -B clean verify
# 使用 Git Bash 可先运行：bash scripts/validate-v02.sh
# 启动 MySQL 后按 README 运行 identity、activity、gateway
```

验证正式 MVP 时，还需跑通注册、报名、凭证、核验、统计完整链路；其中并发报名、重复核验、错误租户和错误资源范围必须用真实数据库测试。文档 05、11 列出了需要逐项完成的缺口。
