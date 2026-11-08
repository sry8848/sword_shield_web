# payment-service — 支付与退款（目标骨架）

- 开发阶段：**V2**；当前仅作为可编译的领域契约模块，不是已启动的 Spring Boot 服务。
- 本领域负责：支付单、退款单、回调对账。
- 核心对象候选：`PaymentOrder`、`PaymentNotification`；字段及关系在开始该领域业务开发前依据 OpenAPI/验收用例冻结。
- 业务不变量：回调验证签名+幂等；付款状态只由支付域确认；不能由前端设置成功。
- 输入层：未来新增 `interfaces/rest`、`application`、`domain`、`infrastructure/persistence`，不允许其他服务直接引用本模块实现类。
- 外部依赖：通过本领域定义的 Port（接口）访问外部服务；禁止跨领域访问 Mapper 或共享写表。
- 预设数据范围接口：`AuthorizationRequest(permissionCode, resourceType, resourceId, tenantId)`；列表使用 `AuthorizedQuerySpec` 适配查询层，详见 `docs/09-数据权限运行模型.md`。
- 验收：落地本领域后补充至少一个成功、一条越权、一条异常状态和一条幂等/并发用例，未实现前禁止将该模块写成“功能已完成”。

## 本模块已建立的包与边界

`domain/model` 为值类型；`domain/port` 为仓储端口；`application` 是用例授权契约；`interfaces/rest` 和 `infrastructure` 保留实施约束说明（尚无可调用 Controller/Mapper）。
