# identity-service — MVP 演示工程到正式工程的落实要求

旧 V0.1 源码在根 package 直接放 Controller/Service/Mapper，这不是目标工程的标准分层，只保留用于技术演示。

正式业务实现必须拆分：

```
interfaces/rest/           HTTP 入口、参数校验、映射
application/               用例、事务、DTO 编排
  port/                     上下游契约
 domain/                    实体、业务不变量、状态机、Port
infrastructure/persistence/ MyBatis Entity/Mapper/Repository adapter
infrastructure/external/    外部客户端
configuration/              Security/租户/数据库/日志配置
```

禁止由于 MVP 演示代码路径简短而继续堆砌根目录类。**本模块不能声称已经实现完整 MVP**。具体功能差异见 `docs/11-现有前端OpenAPI契约差异与冻结项.md`。
