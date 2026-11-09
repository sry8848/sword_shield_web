采用 MVP 文字明确限定的业务能力 作为第一阶段范围，不要机械地把引用到的整个功能编号都实现。

# 大会门户 + 普通报名 + 现场核验 + 基础运营

核心目标： 让参会者能够了解大会、查找活动、完成报名、获取入场凭证并参加大会；让运营人员能够发布活动、管理报名、核验入场并查看基础数据。

不追求 170 项功能全部上线，而是先交付一个可以真正投入使用的大会平台。

这条路径与你文档中已经定义的关键任务完全对应：

大会首页 / 议程 → 场次详情与报名 → 我的报名与凭证 → 现场核验 → 参与报表。

菜单与页面功能清单(3).md

此前讨论的开发顺序也是先做报名预约主链路，再做管理端、RBAC、报表，之后才是限量预约高并发和智能客服。

下面是具体的 MVP 划分。

# 一、MVP 1.0：必须实现的功能

我把它分成五个部分。

## 1. 大会前台：让用户能够了解和参与大会

Must Have

| 页面       | 对应功能编号          | MVP 实现范围            |
| -------- | --------------- | ------------------- |
| P01 首页   | A01、A12         | 大会介绍、重要通知、近期活动、快捷入口 |
| P02 全部场次 | A02–A04、A28     | 场次列表、按日期和会场筛选       |
| P03 场次详情 | A05–A06、A09–A11 | 查看场次详情、普通报名、报名状态    |
| P05 讲者嘉宾 | A33             | 基础嘉宾列表、嘉宾介绍、关联场次    |
| P06 会场指南 | A31             | 地址、交通、入场信息          |

这里需要特别说明：

P03 页面包含很多功能，但不代表 MVP 要把 P03 的全部功能做完。

P03 只实现普通报名相关能力。

暂时不做：

- 订阅与提醒。

- 加入个人日程。

- 评论、点赞、分享。

- 场次提问、投票、反馈。

- 复杂的关联资料操作。

这些都可以在后续版本补充。

P01、P02、P03 的页面设计及其功能编号见原文。

菜单与页面功能清单(3).md

## 2. 用户账号与个人中心

Must Have

| 页面       | 功能编号            | MVP 实现范围         |
| -------- | --------------- | ---------------- |
| P21 登录注册 | U01、U02         | 用户注册、登录          |
| M01 个人概览 | U03             | 简单个人信息、我的报名入口    |
| M02 我的报名 | A13、A32、A34、A38 | 查看报名记录、报名状态、入场凭证 |

暂时不做：

- M03 我的订阅。

- M04 我的日程。

- M05 我的席位与候补。

- M06 我的订单与票券。

- M07 我的周边。

- M08 我的预约。

- M09 我的投稿。

- M10 我的积分与奖品。

- M11 我的工单。

- M12 完整个人资料管理。

其中，M02 必须做，因为报名后用户需要知道自己是否报名成功，以及如何入场。

个人空间的原始页面和功能范围见文档。

菜单与页面功能清单(3).md

## 3. 运营后台：让工作人员能够运营大会

Must Have

| 页面          | 功能编号                        | MVP 实现范围                  |
| ----------- | --------------------------- | ------------------------- |
| B01 后台登录    | U07、U08                     | 工作人员登录、退出                 |
| B03 活动与议程管理 | A14–A16、A19–A20、A22–A24、A27 | 创建、编辑、发布、下线活动，维护活动时间和报名规则 |
| B04 报名管理    | A10–A11、A38                 | 查看报名名单、查询报名记录             |
| B27 运营账号与权限 | U10                         | 基础角色分配和权限控制               |

这里我建议先实现三个基本角色：

- 系统管理员：管理账号、角色和系统配置。

- 会务运营：管理大会活动、议程和报名。

- 现场核验员：只能核验授权场次。

暂时不需要把文档里所有工作人员角色、复杂数据范围、细粒度字段授权全部实现。

但是，不能因为是 MVP 就完全不做权限控制。至少要保证普通用户不能访问后台，核验员不能修改活动，运营人员不能越权操作。

原文已经定义了工作人员角色和授权边界，可以作为后续扩展依据。

菜单与页面功能清单(3).md

## 4. 现场执行：真正完成参会闭环

Must Have

| 页面       | 功能编号 | MVP 实现范围             |
| -------- | ---- | -------------------- |
| B05 现场核验 | A35  | 扫码或输入凭证、校验有效性、记录核验结果 |

必须支持：

- 正常凭证可以核验。

- 无效凭证不能核验。

- 已经核验过的凭证不能重复使用。

- 记录核验时间和操作人员。

这部分不能省略。

如果系统只完成报名，没有现场核验，就只能证明用户能够在网上提交表单，还没有形成完整的大会参会管理流程。

## 5. 基础数据统计

Must Have

| 页面          | 功能编号    | MVP 实现范围                 |
| ----------- | ------- | ------------------------ |
| B21 报名与参与报表 | R05–R08 | 报名总数、各场次报名数量、实际到场人数、基础统计 |

MVP 阶段不需要做复杂的数据分析。

先解决运营人员最关心的问题：

- 有多少人报名？

- 每个场次有多少人报名？

- 有多少人已经入场？

- 哪些场次的报名情况比较好？

渠道转化、复杂趋势分析、反馈分析和高级报表放到后续版本。

B21 原本包含报名、核验、线上观看、渠道明细和导出等能力，第一版只保留最核心的报名和核验指标。

菜单与页面功能清单(3).md

# 二、MVP 1.0 的完整业务流程

这才是本次 MVP 最重要的交付目标。

参会者视角

1. 浏览大会
   
   P01 首页，了解大会基本信息。

2. 查找活动
   
   P02 查看议程，进入 P03 查看场次详情。

3. 完成报名
   
   P21 登录后，在 P03 提交报名。

4. 获取凭证
   
   M01 / M02 查看报名记录和入场凭证。

5. 参加大会
   
   出示凭证，由工作人员通过 B05 核验。

运营人员视角

1. 登录 B01 后台。

2. 通过 B03 创建并发布活动。

3. 通过 B04 管理报名名单。

4. 通过 B05 完成现场核验。

5. 通过 B21 查看报名和参与统计。

端到端业务闭环

这个闭环有一个很重要的特点：前台、后台、数据库和现场业务全部连接起来，而不是只有前端页面可以展示。

# 三、哪些功能暂时不进入 MVP？

我不建议直接把剩下的功能全部放到一个笼统的 P1/P2 里面，而是根据业务性质安排后续版本。

## V1.1：大会内容与线上参会能力

第二阶段

这些功能能显著完善大会门户，但不影响普通报名闭环成立。

| 功能方向   | 页面及编号                       |
| ------ | --------------------------- |
| 直播与回放  | P04、B06：A07–A08、A17–A18、A36 |
| 大会资讯   | P11、B12：C01–C07、C24         |
| 展商展示   | P10、B13：C08–C12             |
| 大会成果   | P14、B14：C14–C15             |
| 资料下载   | P12、B15：C16–C21、C34         |
| 图片与视频  | P13、B16、B17：C22–C33         |
| 大会公开数据 | P09：R01–R02                 |
| 运营概览   | B02：R03–R04                 |

为什么这些放在第二阶段？

它们主要解决大会内容的丰富程度和线上服务能力，而不是普通报名的必要条件。

不过这里有两个例外：

- 如果大会明确要求线上直播，那么 P04 和 B06 应提前进入 MVP。

- 如果展商展示、大会成果属于甲方硬性验收项，也必须提前进入 MVP。

内容展示可以先做简单版本，不必一开始就把文件资产、媒体管理等完整管理体系全部建设出来。

## V2：限量预约与交易系统

第三阶段

这是我建议重点投入技术工作的一个阶段。

| 功能      | 对应页面        |
| ------- | ----------- |
| 限量活动浏览  | P07         |
| 免费工作坊抢席 | P08、M05     |
| 候补和递补   | M05、B08     |
| 批次配置    | B07         |
| 付费票券    | P08、M06、B09 |
| 限量周边    | P08、M07、B10 |
| 退款和异常处理 | B09、B11     |

建议分两步：

第一步：免费限量席位

先实现资格校验、库存控制、重复请求处理、席位查询、退席和候补。

第二步：交易和复杂履约

再实现支付、退款、付费票券、周边领取和异常申诉。

这里的高并发抢票值得作为一个独立技术里程碑，而不是为了追求 MVP 最小化就永远不做。

原文中的限量活动、个人席位、订单和后台处理实际上组成了另一条完整业务链路。

菜单与页面功能清单(3).md

菜单与页面功能清单(3).md

## V3：互动、社区和智能客服

后续增强

| 方向      | 对应功能            |
| ------- | --------------- |
| 大会助手    | P20、S01–S08、S13 |
| 人工客服    | B24、M11、S09–S10 |
| 知识管理    | B25、S11–S12     |
| 客服问题洞察  | B26、S14         |
| 现场互动    | P19、B20         |
| 签名墙     | P15、B19         |
| 投稿与审核   | P22、M09、B18     |
| 热点讨论    | P23             |
| 海报生成    | P17             |
| 签到积分与抽奖 | P18、M10、B28     |
| 参会交流预约  | P16、M08         |

这里的智能客服尤其适合独立成一个产品能力：

先实现大会信息问答、活动和资料检索，再实现登录后的个人事项查询和转人工。

不建议第一版就让 Agent 直接操作报名、支付、退款等业务。

另外，高级报表中的渠道转化 R09 和参会反馈 R10 也可以放到后续版本。

# 四、最终版本划分总表

把上述内容合并后，可以得到这份路线图：

| 功能领域        | MVP 1.0 | V1.1 | V2  | V3  |
| ----------- | ------- | ---- | --- | --- |
| 大会首页与基础信息   | 核心      |      |     |     |
| 议程与普通报名     | 核心      |      |     |     |
| 用户登录与报名记录   | 核心      |      |     |     |
| 活动管理与报名管理   | 核心      |      |     |     |
| 现场核验        | 核心      |      |     |     |
| 基础统计报表      | 核心      |      |     |     |
| 直播、回放、资料    |         | 扩展   |     |     |
| 资讯、展商、成果、媒体 |         | 扩展   |     |     |
| 限量活动与高并发抢票  |         |      | 重点  |     |
| 付费票、退款、周边   |         |      | 重点  |     |
| 智能客服        |         |      |     | 增强  |
| 社区、互动、积分    |         |      |     | 增强  |
| 高级数据分析      |         |      |     | 增强  |

这是一份建议路线图，不是对甲方验收范围的认定。

# 五、MVP 1.0 应该怎么安排开发顺序？

不要按页面编号从 P01 一直开发到 B28，而应该按依赖关系开发。

#chatgpt-mermaid-_r_316_{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;fill:rgb(45, 45, 43);}@keyframes edge-animation-frame{from{stroke-dashoffset:0;}}@keyframes dash{to{stroke-dashoffset:0;}}#chatgpt-mermaid-_r_316_ .edge-animation-slow{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 50s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_316_ .edge-animation-fast{stroke-dasharray:9,5!important;stroke-dashoffset:900;animation:dash 20s linear infinite;stroke-linecap:round;}#chatgpt-mermaid-_r_316_ .error-icon{fill:rgba(251, 250, 246, 0.96);}#chatgpt-mermaid-_r_316_ .error-text{fill:rgb(45, 45, 43);stroke:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .edge-thickness-normal{stroke-width:1px;}#chatgpt-mermaid-_r_316_ .edge-thickness-thick{stroke-width:3.5px;}#chatgpt-mermaid-_r_316_ .edge-pattern-solid{stroke-dasharray:0;}#chatgpt-mermaid-_r_316_ .edge-thickness-invisible{stroke-width:0;fill:none;}#chatgpt-mermaid-_r_316_ .edge-pattern-dashed{stroke-dasharray:3;}#chatgpt-mermaid-_r_316_ .edge-pattern-dotted{stroke-dasharray:2;}#chatgpt-mermaid-_r_316_ .marker{fill:rgba(45, 45, 43, 0.494);stroke:rgba(45, 45, 43, 0.494);}#chatgpt-mermaid-_r_316_ .marker.cross{stroke:rgba(45, 45, 43, 0.494);}#chatgpt-mermaid-_r_316_ svg{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:16px;}#chatgpt-mermaid-_r_316_ p{margin:0;}#chatgpt-mermaid-_r_316_ .label{font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";color:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .cluster-label text{fill:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .cluster-label span{color:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .cluster-label span p{background-color:transparent;}#chatgpt-mermaid-_r_316_ .label text,#chatgpt-mermaid-_r_316_ span{fill:rgb(45, 45, 43);color:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .node rect,#chatgpt-mermaid-_r_316_ .node circle,#chatgpt-mermaid-_r_316_ .node ellipse,#chatgpt-mermaid-_r_316_ .node polygon,#chatgpt-mermaid-_r_316_ .node path{fill:rgb(246, 232, 225);stroke:rgb(217, 119, 87);stroke-width:1px;}#chatgpt-mermaid-_r_316_ .rough-node .label text,#chatgpt-mermaid-_r_316_ .node .label text,#chatgpt-mermaid-_r_316_ .image-shape .label,#chatgpt-mermaid-_r_316_ .icon-shape .label{text-anchor:middle;}#chatgpt-mermaid-_r_316_ .node .katex path{fill:#000;stroke:#000;stroke-width:1px;}#chatgpt-mermaid-_r_316_ .rough-node .label,#chatgpt-mermaid-_r_316_ .node .label,#chatgpt-mermaid-_r_316_ .image-shape .label,#chatgpt-mermaid-_r_316_ .icon-shape .label{text-align:center;}#chatgpt-mermaid-_r_316_ .node.clickable{cursor:pointer;}#chatgpt-mermaid-_r_316_ .root .anchor path{fill:rgba(45, 45, 43, 0.494)!important;stroke-width:0;stroke:rgba(45, 45, 43, 0.494);}#chatgpt-mermaid-_r_316_ .arrowheadPath{fill:rgba(45, 45, 43, 0.494);}#chatgpt-mermaid-_r_316_ .edgePath .path{stroke:rgba(45, 45, 43, 0.494);stroke-width:1px;}#chatgpt-mermaid-_r_316_ .flowchart-link{stroke:rgba(45, 45, 43, 0.494);fill:none;}#chatgpt-mermaid-_r_316_ .edgeLabel{background-color:rgb(250, 249, 245);text-align:center;}#chatgpt-mermaid-_r_316_ .edgeLabel p{background-color:rgb(250, 249, 245);}#chatgpt-mermaid-_r_316_ .edgeLabel rect{opacity:0.5;background-color:rgb(250, 249, 245);fill:rgb(250, 249, 245);}#chatgpt-mermaid-_r_316_ .labelBkg{background-color:rgba(250, 249, 245, 0.5);}#chatgpt-mermaid-_r_316_ .cluster rect{fill:rgba(251, 250, 246, 0.96);stroke:rgba(45, 45, 43, 0.08);stroke-width:1px;}#chatgpt-mermaid-_r_316_ .cluster text{fill:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ .cluster span{color:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ div.mermaidTooltip{position:absolute;text-align:center;max-width:200px;padding:2px;font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";font-size:12px;background:rgba(251, 250, 246, 0.96);border:1px solid rgba(45, 45, 43, 0.08);border-radius:2px;pointer-events:none;z-index:100;}#chatgpt-mermaid-_r_316_ .flowchartTitleText{text-anchor:middle;font-size:18px;fill:rgb(45, 45, 43);}#chatgpt-mermaid-_r_316_ rect.text{fill:none;stroke-width:0;}#chatgpt-mermaid-_r_316_ .icon-shape,#chatgpt-mermaid-_r_316_ .image-shape{background-color:rgb(250, 249, 245);text-align:center;}#chatgpt-mermaid-_r_316_ .icon-shape p,#chatgpt-mermaid-_r_316_ .image-shape p{background-color:rgb(250, 249, 245);padding:2px;}#chatgpt-mermaid-_r_316_ .icon-shape .label rect,#chatgpt-mermaid-_r_316_ .image-shape .label rect{opacity:0.5;background-color:rgb(250, 249, 245);fill:rgb(250, 249, 245);}#chatgpt-mermaid-_r_316_ .label-icon{display:inline-block;height:1em;overflow:visible;vertical-align:-0.125em;}#chatgpt-mermaid-_r_316_ .node .label-icon path{fill:currentColor;stroke:revert;stroke-width:revert;}#chatgpt-mermaid-_r_316_ .node .neo-node{stroke:rgb(217, 119, 87);}#chatgpt-mermaid-_r_316_ [data-look="neo"].node rect,#chatgpt-mermaid-_r_316_ [data-look="neo"].cluster rect,#chatgpt-mermaid-_r_316_ [data-look="neo"].node polygon{stroke:url(#chatgpt-mermaid-_r_316_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_316_ [data-look="neo"].swimlane.cluster rect{filter:none;}#chatgpt-mermaid-_r_316_ [data-look="neo"].node path{stroke:url(#chatgpt-mermaid-_r_316_-gradient);stroke-width:1px;}#chatgpt-mermaid-_r_316_ [data-look="neo"].node .outer-path{filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_316_ [data-look="neo"].node .neo-line path{stroke:rgb(217, 119, 87);filter:none;}#chatgpt-mermaid-_r_316_ [data-look="neo"].node circle{stroke:url(#chatgpt-mermaid-_r_316_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_316_ [data-look="neo"].node circle .state-start{fill:#000000;}#chatgpt-mermaid-_r_316_ [data-look="neo"].icon-shape .icon{fill:url(#chatgpt-mermaid-_r_316_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_316_ [data-look="neo"].icon-shape .icon-neo path{stroke:url(#chatgpt-mermaid-_r_316_-gradient);filter:drop-shadow( 1px 2px 2px rgba(185,185,185,1));}#chatgpt-mermaid-_r_316_ .node text{font-size:14px;font-weight:600;letter-spacing:normal;fill:rgb(217, 119, 87);}#chatgpt-mermaid-_r_316_ .edgeLabels text{font-size:13px;font-weight:600;letter-spacing:-0.08px;fill:rgb(217, 119, 87);}#chatgpt-mermaid-_r_316_ .node tspan[font-weight="normal"],#chatgpt-mermaid-_r_316_ .edgeLabels tspan[font-weight="normal"]{font-weight:600;}#chatgpt-mermaid-_r_316_ .edgeLabel .label rect{opacity:1;rx:13px;ry:13px;fill:rgb(250, 249, 245);stroke:rgb(217, 119, 87);stroke-width:1px;}#chatgpt-mermaid-_r_316_ .node rect,#chatgpt-mermaid-_r_316_ .node circle,#chatgpt-mermaid-_r_316_ .node ellipse,#chatgpt-mermaid-_r_316_ .node polygon,#chatgpt-mermaid-_r_316_ .node path{fill:rgb(246, 232, 225);stroke:rgba(0, 0, 0, 0.1);stroke-width:1px;}#chatgpt-mermaid-_r_316_ .node rect{rx:16px;ry:16px;}#chatgpt-mermaid-_r_316_ .mermaid-decision .label-container{fill:rgb(250, 249, 245);stroke:rgb(217, 119, 87);stroke-dasharray:2,2;}#chatgpt-mermaid-_r_316_ .edgePaths .flowchart-link{stroke:rgba(45, 45, 43, 0.494);stroke-width:1px;stroke-linecap:round;stroke-linejoin:round;}#chatgpt-mermaid-_r_316_ .marker{fill:rgba(45, 45, 43, 0.494);stroke:rgba(45, 45, 43, 0.494);}#chatgpt-mermaid-_r_316_ :root{--mermaid-font-family:-apple-system-body,ui-sans-serif,-apple-system,system-ui,"Segoe UI",Helvetica,"Apple Color Emoji",Arial,sans-serif,"Segoe UI Emoji","Segoe UI Symbol";}阶段0：现有代码盘点与验收基础数据模型、登录、权限后台活动管理 B03前台首页、议程、详情P01/P02/P03普通报名与个人记录 P21/M02报名管理 B04现场核验 B05基础报表 B21全链路测试与部署

具体阶段：

| 阶段  | 主要任务         | 阶段交付物         |
| --- | ------------ | ------------- |
| 0   | 现有代码盘点       | 功能真实性清单、待修复问题 |
| 1   | 数据模型、账号、基础权限 | 可登录的前后台       |
| 2   | 活动管理         | 可以创建和发布活动     |
| 3   | 前台展示         | 用户可以浏览活动      |
| 4   | 普通报名         | 用户可以报名和查询凭证   |
| 5   | 现场核验         | 工作人员可以核验凭证    |
| 6   | 报表           | 可以查看报名及到场数据   |
| 7   | 联调与测试        | 完整业务流程可运行     |

特别注意阶段 0。

此前静态盘点曾发现登录、活动报名和管理、部分统计、资讯、评论、点赞、资料下载等模块已有一定代码基础，但没有完成实际运行验证。不能把这些直接算作已完成。

因此第一步应该是检查：

- 前端页面是否能正常运行。

- 接口是否真正可用。

- 数据库是否能正确保存和查询。

- 前后端是否已经完成联调。

- 权限和异常情况是否正常。

- 是否存在功能只有页面、没有真实业务逻辑的情况。

不要把已有代码全部推翻重写，也不要把代码存在等同于功能完成。

# 六、MVP 的验收标准

最后，我建议不要用「页面完成率」作为 MVP 的主要验收标准，而应该使用业务场景验收。

MVP 1.0 验收清单

验收进度

## 0/10

运营人员能够登录后台并创建、发布活动

游客能够浏览大会首页和活动列表

用户能够查看活动详情并完成注册、登录

用户能够提交普通报名，系统正确保存报名记录

用户能够查看自己的报名状态和入场凭证

现场核验员能够核验凭证，并阻止重复核验

运营人员能够查询报名名单和报名状态

系统能够统计报名人数和实际到场人数

不同工作人员只能执行授权范围内的操作

PC 和移动端均能完成核心报名流程

# 七、我认为还需要注意的一个问题

这份《菜单与页面功能清单》本身说明，它是基于 170 项功能建立的完整产品原型，页面和按钮是目标设计，当前完成状态及优先级仍需要回到原始功能清单核对。

菜单与页面功能清单(3).md

因此，目前这版 MVP 已经可以作为产品范围的第一版方案，但还不能直接作为最终开发任务清单。

还缺少两项核对：

1. 硬性验收需求： 哪些功能是甲方或比赛要求必须实现的？即使不是核心业务闭环，也不能漏掉。

2. 170 项原始功能明细： 需要将功能编号、具体功能描述、验收要求与本次 MVP 分类逐项对应，避免一个页面被选入 MVP，却遗漏了页面中真正重要的功能。

我的最终建议：先冻结上面这版 MVP 1.0 的业务目标，再逐项对照原始 170 项清单，生成 Must / Should / Could / Won't 四类明细，之后才正式估算工作量和分配开发人员。

这样得到的才是一份真正能指导团队开发的 MVP，而不是一张看起来很完整的功能优先级表。
