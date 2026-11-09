# V0.2 交付验证与未通过项

> 本页保留 v0.2 骨架的交付验证记录。现行开发要求见 [后端架构规范](README.md#阅读顺序)。

## 交付时通过的静态检查

- `javac --release 17` 编译 10 个新增领域模块及公共纯 Java 范围模型：通过。
- `java ScopeContractCheck`：原有用户本人、指定场次、资源类型分离规则通过。
- `java ExtendedAuthorizationCheck`：6 个断言通过，覆盖 ownerId SELF、E01/E02 指定范围、E03 拒绝、跨租户不匹配、错误资源类型拒绝。
- XML 解析全部 15 个 pom（父工程 + 14 子模块）：通过。
- PyYAML 解析所有 YAML：通过。
- Maven Reactor 的 14 个模块均有对应 pom.xml：通过。

## 未进行 / 不可冒充已通过

- 没有 `mvn` / Docker CLI；Spring Boot 完整 Maven 构建和实际启动、Flyway + MySQL HTTP 联调**未验证**。
- 多资源数据范围新增纯模型，不代表数据库/复杂 SQL 已实施授权过滤。
- 10 个未来领域仅编译级契约，不包含业务 Controller、Mapper、独立进程或数据迁移。

## Windows 验收入口

```powershell
cd backend
mvn -B clean verify
# 使用 Git Bash 可先运行：bash scripts/validate-v02.sh
# 启动 MySQL 后按 README 运行 identity、activity、gateway
```

正式开发的质量要求见 [测试与发布](docs/02-工程与编码规范.md#测试与发布)，当前接口差异见 [待确认事项与契约差异](docs/11-现有前端OpenAPI契约差异与冻结项.md)。
