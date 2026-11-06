# 西湖论剑·大会门户 MVP 1.0 接口总览

> 状态：Draft 0.1.0。本文与 `openapi/openapi.yaml` 配套使用，供前后端评审和 Apifox 导入。当前不依据现有后端代码，所有未确认业务决策见 `03-待确认事项.md`。

## 1. MVP 范围

### 1.1 首期纳入

- 大会门户：首页、大会基础信息。
- 议程与场次：场次列表、详情、嘉宾、会场信息。
- 账号：参会用户注册、登录、当前用户信息。
- 普通报名：提交报名、我的报名、报名详情、报名凭证。
- 运营后台：场次创建、编辑、发布、下线；报名列表与详情。
- 现场执行：二维码/凭证核验、核验记录。
- 基础权限：后台当前账号、内置角色、权限字典、账号角色分配。
- 数据统计：报名人数、到场人数、单场统计。

### 1.2 明确暂缓

直播与回放、资讯、展商、成果、资料/媒体、公开数据、限量预约/抢票、候补、支付退款、周边、智能客服、互动社区、投稿、海报、积分抽奖、交流预约、高级报表。若甲方明确要求某项作为首期验收项，再单独变更范围和接口版本。

## 2. 核心业务链路

### 2.1 参会用户

1. 访问首页：`GET /public/home`。
2. 查看大会信息：`GET /public/conference`。
3. 浏览场次：`GET /public/events`。
4. 查看场次详情：`GET /public/events/{eventId}`。
5. 注册/登录：`POST /auth/register`、`POST /auth/login`。
6. 提交报名：`POST /events/{eventId}/registrations`。
7. 查询本人报名：`GET /me/registrations`、`GET /me/registrations/{registrationId}`。
8. 获取二维码凭证：`GET /me/registrations/{registrationId}/credential`。

### 2.2 运营人员

1. 登录后读取后台身份与权限：`GET /admin/me`。
2. 创建并维护场次：`POST /admin/events`、`PATCH /admin/events/{eventId}`。
3. 发布/下线场次：`POST /admin/events/{eventId}/publish`、`POST /admin/events/{eventId}/unpublish`。
4. 查看报名：`GET /admin/registrations`、`GET /admin/registrations/{registrationId}`。
5. 现场核验：`POST /admin/check-ins`；查看核验记录：`GET /admin/check-ins`。
6. 查看报名与到场统计：`GET /admin/statistics/overview`、`GET /admin/statistics/events`。

### 2.3 权限管理员

1. 查询后台账号：`GET /admin/users`。
2. 查询内置角色和权限：`GET /admin/roles`、`GET /admin/permissions`。
3. 为账号分配角色：`PUT /admin/users/{userId}/roles`。

## 3. 接口清单

| 分组 | 方法 | 路径 | 用途 | 认证 |
|---|---|---|---|---|
| 大会门户 | GET | `/public/home` | 首页聚合数据 | 否 |
| 大会门户 | GET | `/public/conference` | 大会基础信息 | 否 |
| 大会门户 | GET | `/public/events` | 场次列表 | 否 |
| 大会门户 | GET | `/public/events/{eventId}` | 场次详情 | 否 |
| 大会门户 | GET | `/public/speakers` | 嘉宾列表 | 否 |
| 大会门户 | GET | `/public/speakers/{speakerId}` | 嘉宾详情 | 否 |
| 大会门户 | GET | `/public/venues` | 会场列表 | 否 |
| 大会门户 | GET | `/public/venues/{venueId}` | 会场指南 | 否 |
| 账号认证 | POST | `/auth/register` | 用户注册 | 否 |
| 账号认证 | POST | `/auth/login` | 用户登录 | 否 |
| 账号认证 | POST | `/auth/refresh` | 刷新访问令牌 | 否（刷新令牌） |
| 账号认证 | POST | `/auth/logout` | 退出登录 | 是 |
| 账号认证 | GET | `/auth/me` | 当前用户信息 | 是 |
| 普通报名 | POST | `/events/{eventId}/registrations` | 提交报名 | 是 |
| 普通报名 | GET | `/me/registrations` | 我的报名列表 | 是 |
| 普通报名 | GET | `/me/registrations/{registrationId}` | 我的报名详情 | 是 |
| 普通报名 | GET | `/me/registrations/{registrationId}/credential` | 获取报名凭证 | 是 |
| 活动管理 | GET | `/admin/me` | 当前后台账号和权限 | 是 |
| 活动管理 | GET | `/admin/events` | 后台场次列表 | 是 |
| 活动管理 | POST | `/admin/events` | 创建场次 | 是 |
| 活动管理 | GET | `/admin/events/{eventId}` | 后台场次详情 | 是 |
| 活动管理 | PATCH | `/admin/events/{eventId}` | 修改场次 | 是 |
| 活动管理 | POST | `/admin/events/{eventId}/publish` | 发布场次 | 是 |
| 活动管理 | POST | `/admin/events/{eventId}/unpublish` | 下线场次 | 是 |
| 报名管理 | GET | `/admin/registrations` | 报名列表 | 是 |
| 报名管理 | GET | `/admin/registrations/{registrationId}` | 报名详情 | 是 |
| 现场核验 | POST | `/admin/check-ins` | 核验并登记到场 | 是 |
| 现场核验 | GET | `/admin/check-ins` | 核验记录列表 | 是 |
| 数据统计 | GET | `/admin/statistics/overview` | 报名与到场概览 | 是 |
| 数据统计 | GET | `/admin/statistics/events` | 场次统计列表 | 是 |
| 数据统计 | GET | `/admin/statistics/events/{eventId}` | 单场统计 | 是 |
| 后台与权限 | GET | `/admin/users` | 后台账号列表 | 是 |
| 后台与权限 | GET | `/admin/users/{userId}` | 后台账号详情 | 是 |
| 后台与权限 | PUT | `/admin/users/{userId}/roles` | 替换账号角色集合 | 是 |
| 后台与权限 | GET | `/admin/roles` | 内置角色列表 | 是 |
| 后台与权限 | GET | `/admin/permissions` | 权限字典 | 是 |

> 共 36 个操作定义。这里的“认证=是”表示需要访问令牌；是否具备具体操作权限还需服务端按权限码校验。

## 4. 页面与接口映射

| 页面/入口 | 主要接口 |
|---|---|
| P01 首页 | `/public/home`、`/public/conference`、`/public/events` |
| P02 全部场次 | `/public/events` |
| P03 场次详情与普通报名 | `/public/events/{eventId}`、`POST /events/{eventId}/registrations` |
| P05 讲者嘉宾 | `/public/speakers`、`/public/speakers/{speakerId}` |
| P06 会场指南 | `/public/venues`、`/public/venues/{venueId}` |
| P21 登录/注册 | `/auth/register`、`/auth/login`、`/auth/refresh`、`/auth/logout` |
| M02 我的报名/凭证 | `/me/registrations`、`/me/registrations/{registrationId}`、`/credential` |
| B01 后台入口 | `/auth/login`、`/admin/me` |
| B03 活动管理 | `/admin/events` 及发布/下线动作 |
| B04 报名管理 | `/admin/registrations` |
| B05 现场核验 | `/admin/check-ins` |
| B27 运营账号与权限 | `/admin/users`、`/admin/roles`、`/admin/permissions` |
| B21 报名与参与报表 | `/admin/statistics/overview`、`/admin/statistics/events` |

## 5. 当前设计边界

- 场次（Event）暂时作为议程中的可报名业务单元；大会本身由 ConferenceInfo 表示。
- 嘉宾和会场在 MVP 中只定义读取接口；其后台维护接口暂不纳入本版。
- 普通报名不包含支付、候补、限量抢票，也暂不定义用户自主取消报名接口。
- 核验接口采用一次请求完成“校验凭证 + 写入到场记录”的方案；是否需要先预览再确认待定。
- RBAC 暂采用内置角色和角色分配，不包含自定义角色编辑、细粒度数据范围配置。
- 统计暂提供基础数量和比例，不包含渠道归因、复杂图表和导出。
- 上传文件接口暂不单独定义；封面字段先使用已上传文件 URL，上传方案后续单独确定。

## 6. 推荐开发顺序

1. 公共数据：大会基础信息、场次列表/详情、嘉宾、会场。
2. 认证：注册、登录、当前用户、令牌刷新。
3. 普通报名：提交报名、本人报名列表/详情、凭证。
4. 运营管理：后台场次 CRUD、发布/下线、报名列表。
5. 现场核验：核验写入、核验记录。
6. RBAC：后台身份、角色/权限字典、角色分配。
7. 统计：概览和场次统计。

前端可按上述顺序使用 Apifox Mock 先行开发。每个接口进入联调前，应确认请求字段、响应字段、异常码和权限规则。
