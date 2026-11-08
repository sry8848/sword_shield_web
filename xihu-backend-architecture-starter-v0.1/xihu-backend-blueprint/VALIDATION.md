# 交付自检（2026-11-08）

已执行：
- `bash backend/scripts/static-check.sh`：通过。JDK 21 编译纯 Java 的资源授权模型，并测试用户本人、指定场次、禁止跨资源类型以及默认拒绝。
- 使用 lxml 检查 Maven 各模块 POM XML：通过。
- 使用 PyYAML 解析全部 YAML：通过。
- 检查 8 篇架构文档、数据库 V1 迁移文件、Maven Reactor 和示例脚本存在：通过。

**尚未验证：** `mvn clean verify`、Spring Boot 启动、MySQL Flyway 真实迁移、Gateway/Identity/Activity HTTP 联调、Docker Compose。运行环境没有 Maven、Docker CLI 且不能获取外网依赖。因此工程状态标记为**待构建验证**，不是已实测的可运行发布版。

**当前范围：** 仅示范登录、公开活动列表、活动草稿创建/发布、基本权限码 + 资源范围策略；未交付 MVP 所需的报名、凭证、核验、报表全链路。后续开发前还需按 `docs/04-MVP到完整版迁移表.md` 对照 OpenAPI。
