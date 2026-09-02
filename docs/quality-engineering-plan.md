# 质量效能路线图与进度跟踪

> 用途：跨会话接手文档。任何新对话读本文件即可了解方向决策、整体计划、当前进度与下一步任务，无需重新分析。
> 建立:2026-09-02 · 最近更新:2026-09-02(W3 全部完成,API 层 34/34 全绿,下一站 W6-7 CI)

## 1. 背景与方向决策

**个人定位**：功能测试转**测试开发（质量效能方向）**，融合 DevOps 能力。不转纯 Java 开发（经验清零、与 3 年开发同台），不做传统运维（岗位收缩、7×24 值班）。目标 3-5 年内月入 1.2-2 万（成都市场）。

**载体项目**：Cloudland（本仓库）。叙事是"我为这个全栈项目建设的完整质量工程体系"，不是孤立 demo。

**技术选型（已定，不再讨论）**：

| 决策    | 选择                                  | 理由                            |
| ----- | ----------------------------------- | ----------------------------- |
| 接口框架  | pytest + requests + allure-pytest   | 测开面试主流；为 AI+Python 铺路         |
| CI 平台 | GitHub Actions（仓库双推 Gitee）          | 面试官最熟；Gitee Go 部署流水线保留        |
| 仓库结构  | 单仓库加 `testing/` 目录                  | 与项目一体的质量体系叙事                  |
| 运维增量  | Prometheus + Grafana + Alertmanager | pom 已有 actuator，临门一脚；SRE 能力证明 |

## 2. 12 周总计划（2026-09-07 \~ 11-29，每周 8-10h）

| 阶段       | 周次     | 任务                                            | 验收标准                         | 状态             |
| -------- | ------ | --------------------------------------------- | ---------------------------- | -------------- |
| 基建整备     | W1-2   | playwright 拆 projects、种子数据、helper 参数化         | 删库重建后一条命令全绿                  | ✅ 完成（提前，09-02） |
| API 自动化层 | W3-5   | pytest + Allure 约 40 条用例                      | `pytest --alluredir` 全绿，分层可读 | ⬜              |
| CI 流水线   | W6-7   | GitHub Actions 双 job（api/e2e）+ Allure 报告 + 徽章 | push 即触发，双 job 全绿            | ⬜              |
| E2E 扩容   | W8-10  | 8-10 个 spec 约 25 用例 + 监控体系                    | CI 连续 5 次全绿（flaky 清零）        | ⬜              |
| 作品集包装    | W11-12 | 质量工程 README + 测试金字塔 + 面试故事                    | 外行看懂价值，内行看出深度                | ⬜              |

**运维增量（并入 W6-10，不单独排期）**：

- W6-7 CI 加第三个 job：**deploy+smoke**（把 deploy.sh 健康门禁思想搬进流水线）

- W8-10 并行：**监控体系**——micrometer-registry-prometheus + compose 加 Prometheus/Grafana/Alertmanager + 一块大盘 + 一条告警规则

- W12 后可选：docker-compose 迁单节点 k3s（加分项非入场券）

**API 层必写的亮点用例（面试故事素材）**：

| 类别  | 用例                                  | 叙事                            |
| --- | ----------------------------------- | ----------------------------- |
| 安全  | 注册请求带 `power=2` → 断言入库 power=0      | 已发现并修复的真实提权漏洞（commit 60b1487） |
| 安全  | `totalAmount=0.01` 篡改支付金额 → 断言服务端重算 | 金额防篡改验证                       |
| 安全  | power=0 删他人订单 → 断言拒绝                | 越权测试                          |
| 一致性 | 下单锁地 status=0 → 删单回滚 =1             | 数据一致性                         |
| 契约  | 读取 Code.java 与 code.js 源码自动比对码表     | 防前后端码表漂移                      |

## 3. W1 完成记录（2026-09-02）

**提交**：`60b1487`（安全修复）+ `50e05e8`（测试基建），已推送并经 Gitee Go 部署到线上（8.137.114.176）。

**测试基建**：

- [playwright.config.js](../vue/playwright.config.js)：`edge-ci`（无头 CI）/ `edge-debug`（本地调试）拆分；`html` + `junit` 双 reporter；viewport 1920×1080

- [seed.sql](../vue/tests/fixtures/seed.sql)：3 个 199 段专用账号（power=0/1/2，密码 `Test@123456`，INSERT IGNORE 幂等；哈希由后端真实注册流程生成）

- [seed.mjs](../vue/tests/fixtures/seed.mjs)：跨平台种子脚本（mysql2 直连，`TEST_DB_*` 环境变量）

- [accounts.js](../vue/tests/fixtures/accounts.js)：账号集中定义 + `TEST_API_BASE_URL`

- helpers 参数化：[redis.js](../vue/tests/helpers/redis.js)（`TEST_REDIS_*`，**惰性单例**）、[cleanup.js](../vue/tests/helpers/cleanup.js)（`TEST_API_BASE_URL` + `cleanupTestUsersByPrefix`）、[auth.js](../vue/tests/helpers/auth.js)（`loginByPassword` 通用登录）

- `npm run test:ci` = seed → webkit + edge-ci，**54/54 全绿零 flaky（2 分钟）**

**顺手修复的 3 个真实缺陷**：

1. **注册提权漏洞（高危）**：[UserServiceImpl.save](../src/main/java/com/cloudland/service/impl/UserServiceImpl.java) 原样入库客户端 power/status/debt → 传 `power=2` 即成管理员。已强制 power=0/status=1/debt=0，攻击请求验证通过
2. **cascader 面板视口外（产品 bug）**：element-ui 2.4.5 popper 无边界钳制，注册表单地址级联面板（高 1170px）被定位到视口上方外。修复：`popper-class` + [element-overrides.css](../vue/src/styles/element-overrides.css) 限高 300px 滚动
3. **C01 用例失真**：`sendCode` 前置全表单校验（合理），用例只填 phone/email 就期待倒计时 → 补全表单

**flaky 治理经验**：Playwright 复用 worker 进程跑多文件，前一文件 afterAll 的 `closeRedis()` 关闭模块级连接 → 后一文件 import 死连接（"Connection is closed"）。单文件跑不触发。修法：惰性单例 + status=end 时重建。

## 4. 下一步任务

### W2(收尾,✅ 已完成 2026-09-02)

- [x] `.env.test` 模板 + `tests/env.js` 加载器:收敛 `TEST_*` 环境变量。优先级:已注入 env(如 CI)> `.env.test` > 代码默认值。坑:Playwright 加载 config 的管道不能 import `.mjs`(pirates hook 会错转 CJS),加载器须用 `.js` + CJS 语法以同时兼容 seed.mjs 的纯 Node ESM import

- [x] `cleanup.spec.js` 4 用例(132 段专用前缀,纯 API 不依赖前端):API 造号 → 按前缀清理返回删除数 → 幂等重跑返回 0 → seed 账号(199 段)不被误删

- [x] 线上冒烟双 PASS:① 注册带 `power=2/status=0/debt=999` → 登录实测入库 `power=0/status=1/debt=0`;② 线上 `app.82fb3576.css` 含 `.region-cascader-popper .el-cascader-panel{max-height:300px}`。`test:ci` 62/62 全绿(新增 cleanup 4 用例 × 2 project)

### W3-5（API 自动化层）—— W3 全部完成 ✅，下一站 W6-7 CI

**当前状态（2026-09-02）**：`testing/api/` 已落地 34 用例全绿（`python -m pytest testing/api/ --alluredir=testing/api/allure-results`）。

```
testing/api/
├── conftest.py          # seed 登录态 fixtures（user_id 内嵌）+ product/land 造数工厂 + redis fixture
├── common/
│   ├── client.py        # requests 封装：token 头 + updatedToken 续期 + 订单/支付 helper（pid/uid 小写契约）
│   └── assertions.py    # 双层断言：HTTP 状态 + Code 业务码
├── test_auth.py     (10) # 登录双通道/401·403 语义/token 滑动续期/公开接口白名单
├── test_user.py     (5)  # 提权回归（CRITICAL）/注册查重/默认值锁定/seed 保护
├── test_order.py    (7)  # 锁地/重复下单/删单放地/扣库存/超卖/库存回补/越权删单
├── test_pay.py      (5)  # 金额服务端重算防篡改/Redis 交易映射 TTL/已支付拒绝/越权支付/不存在订单
├── test_contract.py (7)  # 码表契约：键集合/同名取值/isSuccess 白名单/消息映射/无重值/锚点常量
└── requirements.txt     # pytest + requests + allure-pytest + redis
```

**契约用例已落地（2026-09-02）**：`test_contract.py` 纯源码正则解析（无需起服务），固化四层契约——① Java/JS 码表键集合一致（无缺失无多余）；② 同名状态码逐键取值一致；③ 两端 isSuccess() 成功白名单一致且引用合法；④ code.js getCodeMessage 消息映射全覆盖；另含码表无重值与 SUCCESS=0/FAILURE=-1 锚点锁定。解析要点：Java 匹配 `public static final Integer NAME = 数字;`；JS 只匹配两空格缩进的 `NAME: 数字,` 属性行（排除 `[Code.X]: '文案'` 映射行）；文件用 utf-8-sig 读取兼容 BOM。

**W4-5 可选增强**（时间富余再做，不阻塞 W6-7）：改密防越权（/user/password + UpdatePasswordDTO）、订单导出下载、msg 模块公开接口。

**本地运行注意**：

- 需后端 9090 + MySQL + Redis 全部在线（无需前端）

- 临时账号 134 段 / 造数产品名前缀 `APIT产品` / 造数土地名前缀 `APIT土地`（conftest 工厂自动清理，残留可按前缀手动清）

- 支付用例依赖支付宝沙箱配置（本地 application.yml 已配好，只测 /alipay/pay 不碰回调）

### W6-7（CI）预埋的坑（提前知道）

- `FILE_STORAGE_ROOT` 在 CI 是 Linux 路径，需 env 覆盖 `D:/CloudLandFile`

- 支付宝 notify 公网不可达：支付用例只测 `/alipay/pay` 金额重算与交易映射，不碰回调

- MySQL 首启导 sql 需 healthcheck 等待

- Maven 在本机无 CLI：用 `C:\Users\67610\.m2\wrapper\dists\apache-maven-3.8.7-bin\1ktonn2lleg549uah6ngl1r74r\apache-maven-3.8.7\bin\mvn.cmd`

- webkit 浏览器已装（`npx playwright install webkit` 已执行过）

## 5. 面试故事清单（持续积累）

1. **安全用例设计**：搭测试基建时发现注册提权漏洞（power=2 直通管理员），修复 + 回归用例固化
2. **flaky 治理**：从"多文件串行偶发红"到定位 worker 复用导致连接传染，惰性单例根治
3. **CI 从 0 到 1**：环境编排踩坑（W6-7 做完后补全）
4. **从 0 搭可观测体系**：为什么选 micrometer、大盘放哪些指标、告警阈值怎么定（W8-10 做完后补全）
5. **UI 缺陷的测试价值**：headless E2E 逼出真实用户会踩的级联面板视口外缺陷

## 6. 本地环境速查

| 项            | 值                                                                                  |
| ------------ | ---------------------------------------------------------------------------------- |
| 后端启动         | `java -jar target\Cloudland-0.0.1-SNAPSHOT.jar`（先 `mvn clean package -DskipTests`） |
| 前端启动         | `cd vue && npm run serve`（8080）                                                    |
| E2E 测试       | `cd vue && npm run test:ci`（62 用例）/ `npm run test:debug`                           |
| API 测试       | `python -m pytest testing/api/ -v --alluredir=testing/api/allure-results`（34 用例）   |
| 本地 MySQL     | root / 123456，库名 cloudland                                                         |
| 本地 Redis     | 6379，密码 123456                                                                     |
| 种子测试账号       | 19900000001（客户）/ 19900000002（员工）/ 19900000003（管理员），密码均 `Test@123456`               |
| ⚠️ 演示账号勿用于测试 | 18140213287 在本地库 power 已非 2，admin 接口会 403                                          |
| push 即部署     | push 到 main 触发 Gitee Go → deploy.sh → 生产 8.137.114.176                             |

## 7. 进度日志

- **2026-09-02**：W1 全部完成（原计划 W1-2，提前收口）。方向决策定稿（测开×DevOps 融合）。2 commit 上线。54/54 全绿。
- **2026-09-02**：W6 开工：GitHub Actions CI 流水线落地（`.github/workflows/ci.yml`，推送至 https://github.com/T1Longlive/CloudLand）。三 job 结构：① **api**（MySQL/Redis docker 编排 + 导入 cloudland.sql + mvn 构建 + 健康门禁 actuator + pytest 34 用例 + Allure 结果）；② **e2e**（同套基础设施 + 前端构建覆盖 `VUE_APP_API_BASE_URL` 指向本地 + `serve -s` SPA fallback + playwright webkit/msedge 62 用例）；③ **report**（simple-elf/allure-report-action 汇总历史 + Playwright HTML 报告，peaceiris 发布 gh-pages）。README.en.md 加 CI/Allure/Playwright 三徽章。**关键技术决策**：支付用例依赖 `alipayClient.pageExecute`（本地签名构造表单不联网）→ CI 生成一次性 RSA 密钥对经环境变量注入，本地已验证（9091 第二实例 + 5/5 支付用例通过）；MySQL 就绪判定用 `mysqladmin ping -h127.0.0.1` + user 表可查双条件（过滤 entrypoint temp-server 阶段）；生产部署仍由 Gitee Go 负责。deploy+smoke 第三个部署 job 按用户决策延后（不阻塞）。


- **2026-09-02**:W2 收尾完成,`test:ci` 62/62 全绿。`.env.test` 体系落地(含 Playwright .mjs 管道坑位记录);cleanup helper 补齐调用方用例;线上冒烟确认两修复生效。下一步:W3-5 API 自动化层(pytest,首条用例=提权回归)。

- **2026-09-02**:W3 开工:API 自动化层脚手架落地(testing/api/,pytest+requests+allure-pytest),15 用例全绿——test\_auth.py(登录双通道/HTTP 401·403 语义/token 滑动续期)+ test\_user.py(提权回归/注册查重/默认值锁定/seed 保护)。**顺手发现并修复真实缺陷**:不存在手机号+验证码头登录 → redisTemplate.delete(null) 抛 500,已改为返回 PHONE\_NO\_EXIST(UserServiceImpl)。

- **2026-09-02**:W3 主干完成:test\_order.py(锁地/重复下单/删单回滚放地/扣库存/超卖拒绝/库存回补/越权删单 POWER\_ERR)7 用例 + test\_pay.py(金额服务端重算防篡改/Redis 交易映射 TTL/已支付拒绝/越权支付拒绝/不存在订单)5 用例,API 层 27/27 全绿。**顺手修复后端缺陷**:AlipayController.pay 业务规则违规(订单不存在/已支付/归属失败)原为未捕获 RuntimeException→HTTP 500,现返回 ADD\_ERR 业务码。**造数踩坑记录**:①product.a\_id 为 NOT NULL 外键,造产品必须带 aId;②LandMapper.selectById 是 INNER JOIN user(代理人),land.employee\_id 缺省时查不到行→下单 NPE 500,造地必须带 employeeId;③@RequestBody 的 Lombok 属性(pId/uId)Jackson 绑定键为全小写 pid/uid,与前端一致;④redis-py 6.x 默认 RESP3 发 HELLO,本地 Redis 不支持需 protocol=2。待办:契约用例(Code.java vs code.js 码表比对)。

- **2026-09-02**:W3 收尾整理:计划文档重构(第 4 节改为接手指引,含目录-用例数对照、W3 剩余契约用例的实现思路、本地运行注意事项)。**未 push 的 3 个 commit**:f450ff9(W2)/7c6d3ab(W3 开工,含 UserServiceImpl 500 修复)/bf4122f(W3 主干,含 AlipayController 500 修复)——下次会话先确认是否 push 上线(push 即触发 Gitee Go 部署)。本地后端已用含两修复的 jar 运行中。

- **2026-09-02**:W3 全部完成:新增 `test_contract.py` 契约用例 7 条(码表键集合/逐键取值/isSuccess 双端白名单/白名单引用合法性/getCodeMessage 全覆盖/码表无重值/SUCCESS=0·FAILURE=-1 锚点),API 层 **34/34 全绿**。纯源码正则解析,不起服务即可跑通(0.02s),天然适配 CI 的快速反馈位。W4-5 可选增强不阻塞主线,下一站 **W6-7 CI 流水线**(GitHub Actions 双 job + Allure)。

