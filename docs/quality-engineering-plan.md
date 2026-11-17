# 质量效能路线图与进度跟踪

> 用途：跨会话接手文档。任何新对话读本文件即可了解方向决策、整体计划、当前进度与下一步任务，无需重新分析。
> 建立:2026-09-02 · 最近更新:2026-11-17
> ⚠️ 2026-11-16：全部测试代码已清空重建（快照 tag `v1-legacy-test-suite`）。本文档第 3-9 节为历史记录，新体系见 `testing/README.md`。
> ⚠️ 2026-11-17：**当前进度以第 10、11 节为准**（第 8 节的用例数/调度描述已过时）。
> 11 节 = 同日 Stage 3（E2E）交付记录；10.1/10.5 的 E2E 行已被 11 节覆盖。

## 1. 背景与方向决策

**个人定位**：功能测试转**测试开发（质量效能方向）**，融合 DevOps 能力。不转纯 Java 开发（经验清零、与 3 年开发同台），不做传统运维（岗位收缩、7×24 值班）。目标 3-5 年内月入 1.2-2 万（成都市场）。

**载体项目**：Cloudland（本仓库）。叙事是"我为这个全栈项目建设的完整质量工程体系"，不是孤立 demo。

**技术选型（已定，不再讨论）**：

| 决策    | 选择                                  | 理由                            |
| ----- | ----------------------------------- | ----------------------------- |
| 接口框架  | pytest + requests + allure-pytest   | 测开面试主流；为 AI+Python 铺路         |
| CI 平台 | GitHub Actions（主通道）                 | 面试官最熟；Gitee Go 额度耗尽后于 2026-10-09 退役 |
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

### W6-7（CI 流水线）—— W6 主体完成 ✅（2026-09-02）

**双 job 全绿 + 报告上线**：Run #2（commit 341925a）api / e2e / report 三 job 全部 success。

**访问入口**：

- CI 状态徽章：README.en.md（Actions / Allure / Playwright 三徽章）

- Allure 报告（含历史趋势）：<https://t1longlive.github.io/CloudLand/>

- Playwright HTML 报告：<https://t1longlive.github.io/CloudLand/e2e/>

- 仓库：<https://github.com/T1Longlive/CloudLand>（remote `origin`；Gitee 远端已于 2026-10-09 移除，不再双推）

**首跑失败与修复（面试故事素材：CI 环境与本地差异）**：

- 现象：E2E job 一次通过，API job 挂在 pytest 步骤

- 根因：CI 全新数据库只有 cloudland.sql 基础数据，无 199 段种子账号 → conftest 所有 fixture 登录失败；本地库预置过 seed 所以从未暴露

- 修复：api job 在 MySQL 就绪后显式导入 `vue/tests/fixtures/seed.sql`（INSERT IGNORE 幂等）并回查 199 段账号确认

- 教训：测试对环境的隐式依赖（本地预置数据）必须在 CI 的全新环境里验证一次才算真正闭环

### W6-7（CI 流水线）—— W7 完成 ✅（2026-09-02，W6-7 整体收口）

**W7 线上只读冒烟 job**（commit 17dfa40，Run #4 四 job 全绿验证）：
- **smoke job**：无基础设施依赖（无 docker/maven，~1 分钟）。四步断言——前端首页 200、`/api/actuator/health` 含 UP、土地分页 code=10004 且 records>0、产品分页同断言。纯只读（不部署/不写库/不造数），打生产 8.137.114.176。
- **触发策略**：随 push/PR 运行（作为质量门禁的一部分）+ `cron '0 */6 * * *'` 每 6 小时定时巡检（北京 8/14/20/2 点）。定时触发时 api/e2e/report 三 job 以 `if: github.event_name != 'schedule'` 跳过，只跑 smoke——重型验证与轻量巡检解耦。
- **首跑前本地预验证**：本地 curl 确认线上三端点行为（health=UP、land/page 返回数据、前端 200）后才写断言，避免拿猜的契约上 CI。

**W6-7 汇总**：CI 已具备 push 门禁（api 34 + e2e 62 用例）、报告发布（gh-pages：Allure 历史趋势 + Playwright HTML）、线上巡检（smoke 定时）。deploy+smoke 的"部署门禁"形态按当时的决策不做。（**2026-10-09 更新**：部署已迁 GitHub Actions，见 `.github/workflows/build-deploy.yml`；Gitee Go 通道退役。）

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
6. **"假 flaky" 排查（W8）**：webkit 两用例稳定失败而 Edge/curl 全过 → 不是随机不稳定，是宿主 HTTP_PROXY 污染浏览器网络栈。用 trace、手工预检、Tomcat access log、最小探针四步定位，修复落在测试配置层而非业务代码
7. **门禁死锁（W8）**：把 CI 结论串成部署门禁后，意识到线上巡检失败会永久挡住"发修复版本"，给 smoke 加 continue-on-error 解耦

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
| push 即部署     | push 到 main（命中 paths）触发 GitHub Actions → 构建推 ACR → deploy.sh → 生产 43.136.135.75 |

## 7. 进度日志

- **2026-09-02**：W1 全部完成（原计划 W1-2，提前收口）。方向决策定稿（测开×DevOps 融合）。2 commit 上线。54/54 全绿。

- **2026-09-02**：W6 开工：GitHub Actions CI 流水线落地（`.github/workflows/ci.yml`，推送至 <https://github.com/T1Longlive/CloudLand）。三> job 结构：① **api**（MySQL/Redis docker 编排 + 导入 cloudland.sql + mvn 构建 + 健康门禁 actuator + pytest 34 用例 + Allure 结果）；② **e2e**（同套基础设施 + 前端构建覆盖 `VUE_APP_API_BASE_URL` 指向本地 + `serve -s` SPA fallback + playwright webkit/msedge 62 用例）；③ **report**（simple-elf/allure-report-action 汇总历史 + Playwright HTML 报告，peaceiris 发布 gh-pages）。README.en.md 加 CI/Allure/Playwright 三徽章。**关键技术决策**：支付用例依赖 `alipayClient.pageExecute`（本地签名构造表单不联网）→ CI 生成一次性 RSA 密钥对经环境变量注入，本地已验证（9091 第二实例 + 5/5 支付用例通过）；MySQL 就绪判定用 `mysqladmin ping -h127.0.0.1` + user 表可查双条件（过滤 entrypoint temp-server 阶段）；生产部署仍由 Gitee Go 负责。deploy+smoke 第三个部署 job 按用户决策延后（不阻塞）。

- **2026-09-02**:W2 收尾完成,`test:ci` 62/62 全绿。`.env.test` 体系落地(含 Playwright .mjs 管道坑位记录);cleanup helper 补齐调用方用例;线上冒烟确认两修复生效。下一步:W3-5 API 自动化层(pytest,首条用例=提权回归)。

- **2026-09-02**:W3 开工:API 自动化层脚手架落地(testing/api/,pytest+requests+allure-pytest),15 用例全绿——test\_auth.py(登录双通道/HTTP 401·403 语义/token 滑动续期)+ test\_user.py(提权回归/注册查重/默认值锁定/seed 保护)。**顺手发现并修复真实缺陷**:不存在手机号+验证码头登录 → redisTemplate.delete(null) 抛 500,已改为返回 PHONE\_NO\_EXIST(UserServiceImpl)。

- **2026-09-02**:W3 主干完成:test\_order.py(锁地/重复下单/删单回滚放地/扣库存/超卖拒绝/库存回补/越权删单 POWER\_ERR)7 用例 + test\_pay.py(金额服务端重算防篡改/Redis 交易映射 TTL/已支付拒绝/越权支付拒绝/不存在订单)5 用例,API 层 27/27 全绿。**顺手修复后端缺陷**:AlipayController.pay 业务规则违规(订单不存在/已支付/归属失败)原为未捕获 RuntimeException→HTTP 500,现返回 ADD\_ERR 业务码。**造数踩坑记录**:①product.a\_id 为 NOT NULL 外键,造产品必须带 aId;②LandMapper.selectById 是 INNER JOIN user(代理人),land.employee\_id 缺省时查不到行→下单 NPE 500,造地必须带 employeeId;③@RequestBody 的 Lombok 属性(pId/uId)Jackson 绑定键为全小写 pid/uid,与前端一致;④redis-py 6.x 默认 RESP3 发 HELLO,本地 Redis 不支持需 protocol=2。待办:契约用例(Code.java vs code.js 码表比对)。

- **2026-09-02**:W3 收尾整理:计划文档重构(第 4 节改为接手指引,含目录-用例数对照、W3 剩余契约用例的实现思路、本地运行注意事项)。**未 push 的 3 个 commit**:f450ff9(W2)/7c6d3ab(W3 开工,含 UserServiceImpl 500 修复)/bf4122f(W3 主干,含 AlipayController 500 修复)——下次会话先确认是否 push 上线(push 即触发 Gitee Go 部署)。本地后端已用含两修复的 jar 运行中。

- **2026-09-02**:W3 全部完成:新增 `test_contract.py` 契约用例 7 条(码表键集合/逐键取值/isSuccess 双端白名单/白名单引用合法性/getCodeMessage 全覆盖/码表无重值/SUCCESS=0·FAILURE=-1 锚点),API 层 **34/34 全绿**。纯源码正则解析,不起服务即可跑通(0.02s),天然适配 CI 的快速反馈位。W4-5 可选增强不阻塞主线,下一站 **W6-7 CI 流水线**(GitHub Actions 双 job + Allure)。

- **2026-09-02**:W6 收官:CI Run #2 三 job 全绿(api 34 用例/e2e 62 用例/report 发布),GitHub Pages 上线(Allure 根路径 + Playwright /e2e/,用户手动开启 Pages 源=gh-pages)。首跑 API job 失败→根因=CI 全新库无 199 段 seed(本地预置过未暴露)→修复=api job 显式导入 seed.sql 并回查,教训已记入面试故事。
- **2026-09-02**:W7 完成:smoke 线上只读冒烟 job 上线(commit 17dfa40,Run #4 四 job 全绿验证)。四步断言(前端 200/health UP/土地分页/产品分页,均含业务码与数据量校验),随 push 门禁运行 + 每 6 小时 cron 巡检;定时触发时重型 job 条件跳过,只跑轻量 smoke。**W6-7 整体收口**。

## 8. W8 质量门禁闭环（2026-11-12）

**接手时发现的三个一致性问题**：① `testing/api` 三层重构（services/、common/http.py、data_loader.py、data/、pytest.ini）全是未跟踪文件，GitHub CI 跑的还是旧版 client.py —— 本地绿 ≠ CI 绿；② `ci.yml` 与 `build-deploy.yml` 完全独立，CI 红照样部署；③ `pytest.ini` 定义了 6 个 marker 只用了 1 个，无任何覆盖率度量。

**本轮实测基线（本地真实环境，非文档自述）**

| 项 | 结果 |
|---|---|
| Java 单测 `mvn test` | 17/17 |
| 契约测试（离线源码解析） | 7/7，0.01s |
| 接口自动化（真实 MySQL/Redis/后端） | 34/34，2.25s |
| PR 快速集 `-m "smoke or p0"` | 27 通过 / 7 deselected |
| 框架代码覆盖率 pytest-cov | 98%（192 行，3 行未覆盖） |
| E2E（webkit + edge-ci） | 修复前 53/62 → 修复后连续两轮 62/62，junit failures=0 |

**"假 flaky" 根因与修复（面试故事 6，可复现）**

- 现象：`npm run test:ci` 中 webkit 的「密码登录成功」「F01 注册成功」稳定失败（单独复跑仍是同样 2 条），Edge 全过，后端/数据库/接口用例均正常。
- 排查链：① Playwright trace 报 `Origin http://localhost:8080 is not allowed by Access-Control-Allow-Origin. Status code: 204`，正式 POST 无响应；② curl 手工发 OPTIONS 预检（带 `accept`/`origin` 等头）返回 200 + 完整 ACAO，后端 CORS 配置无问题；③ 重启后端开 Tomcat access log，失败时**无任何 OPTIONS 记录** → 请求根本没到后端；④ 查环境变量发现宿主注入 `HTTP_PROXY=http://127.0.0.1:51794` 且无 NO_PROXY；⑤ 最小 webkit 探针里 delete 代理变量后 POST 恢复 200，access log 出现 `OPTIONS /api/user/login 200`。
- 结论：代理对 localhost 的 CORS 预检返回 204 且不带 CORS 头，只有 WebKit 走这条路径；CI runner 不注入代理，所以线上 62/62 全绿而本地稳定 60/62。**不是业务缺陷、不是随机 flaky**。
- 修复：`vue/playwright.config.js` 在 config 加载时清除 6 个代理环境变量（测试只访问本机服务），不改任何业务代码。修复后连续两轮全量 E2E 62/62。
- 教训：测试环境的网络路径必须与 CI/生产一致；"只有某个浏览器挂"先查环境差异，别急着加 retries 把问题盖住。

**门禁闭环设计**

- `build-deploy.yml` 改 `workflow_run`（workflows: ["CI"], types: [completed], branches: [main]），build job 只在 `conclusion == success && event == 'push' && head_branch == 'main'` 时执行；PR、定时巡检、CI 失败的运行都不部署。
- 制品与代码统一用 `DEPLOY_SHA = workflow_run.head_sha || github.sha`（checkout ref 显式指定），避免 workflow_run 下 `github.sha` 指向默认分支最新提交，导致「构建出未被测试过的版本」。
- 原 push+paths 守卫在 workflow_run 事件下不受 GitHub 支持，但 `scripts/deploy.sh` 的 NON_APP_PATHS 路径守卫仍在，纯文档变更不会重建容器。
- **防死锁**：`ci.yml` 的 smoke job 加 `continue-on-error: true`。线上自身故障（当前 8.137.114.176 返回 502）若让 CI 结论变红，会因门禁串联导致「线上挂了 → 修复版本也发不出去」。巡检只负责告警，放行/拦截由 api + e2e 两个测试 job 决定。
- 应急通道保留 `workflow_dispatch`（绕过门禁，仅用于故障恢复，记录触发人）。

**分层调度与覆盖率**

- 各 `test_*.py` 用模块级 `pytestmark` 打标：auth/contract=smoke，user/pay=p0，order=regression。PR 跑 `-m "smoke or p0"`（27 条），push main 与 UTC 18 点（北京 02:00）夜间 cron 跑全量 34 条 + E2E 全量；每 6 小时的 schedule 仍只跑线上只读巡检。
- `requirements.txt` 加 `pytest-cov`，CI 上传 `coverage.xml`。**口径要说清**：这里量的是 Python 测试框架自身代码（common/、services/）覆盖率 98%，不是后端 Java 代码覆盖率；后端需另接 JaCoCo agent，简历里别混为一谈。

**仍未完成（别写进简历）**

- 线上 8.137.114.176 当前 502，服务器侧未恢复，定时巡检仍在告警；需登录服务器排查，本地无法处理。
- JaCoCo 后端代码覆盖率未接入。
- 前端单测仍为 0；接口覆盖仍为 12/36 端点；性能/压测与 Prometheus 监控未做。
- 62/62 连续全绿仅本地验证两轮，5 次连续稳定性待 CI 连续运行与 Allure 历史趋势确认。
- 本轮改动只做本地 commit，**未 push**；push 会触发真实 CI 与生产部署，需本人确认。

## 9. 进度日志（续）

- **2026-11-12**：W8 质量门禁闭环收口。提交 testing/api 三层重构、串联 CI→部署门禁、marker 分层调度 + pytest-cov（98%）、修复 webkit 代理导致的「CORS 假 flaky」（62/62 全绿）、smoke 防死锁；同步修正文档漂移与 .gitignore。

## 10. 2026-11-17 进度与接手指南（当前最新）

### 10.1 实测基线（本地真实环境）

| 层 | 数量 | 结果 |
|---|---|---|
| 单元（JUnit 5） | 83 | 全绿 ~1s；JaCoCo 行覆盖基线 25.3%（**只度量、无 check 阈值**） |
| 接口（pytest） | 110（105 passed + 5 xfailed） | 连续 3 轮全绿 ~5.5s；零残留（134 账号 0、alipay:trade 键 0、trolley/order 计数三轮不变） |
|  marker 子集 | smoke 47 / smoke or p0 77 | PR 跑后者 |
| E2E | 0 | 旧套件已删（tag `v1-legacy-test-suite`），Stage 3 未开始 |

### 10.2 今日完成

- **单测 +40**：`Order2ServiceImplTest`(24，Mockito 守金额重算/锁地扣库存/删单回滚/归属/支付映射幂等)、`StoragePropertiesTest`(7)、`FileUtilTest`(9，@TempDir 隔离)。
- **接口 +23**：`test_contract.py`(6，离线码表契约，恢复重建时丢失的资产)、token 续期通道 3、用户查重/改绑 4、`test_trolley.py` 3、订单写路径真实回滚 3、`test_pay.py` 4（金额防篡改/Redis TTL/归属/已支付/不存在订单）。
- **修掉的测试自身缺陷**：JWT 篡改用例改签名尾字符（base64url 尾字符低位不参与解码→概率性假绿/假红）改中段；`deleteById` 传实体重载的 verify 参数；**Jackson 对 Lombok pId/uId 的请求与响应键均为小写 pid/uid**；`login_by_token` 补 `frond` 头（客户 power=0 不带被拦 20008）。
- **JaCoCo 0.8.11** 接入 pom（prepare-agent + report）；ci.yml unit job 上传报告。
- **CI 分层调度落地**（见 10.3）；**build-deploy 门禁加 `workflow_run.event == 'push'`**——否则夜间/巡检 cron 的 success 结论会触发定时部署（加 cron 引入的连锁风险，已堵）。
- ci.yml api job 用 openssl 生成**一次性 RSA 密钥**注入 ALIPAY_*（pay 用例只本地签名，真实沙箱密钥不进 CI）。
- `testing/README.md` 基线同步；`.idea` 旧 iml 与 4 个过期库备份至 `.idea-stale-backup-20261117/`（未跟踪，确认 IDEA 正常后可删）。

### 10.3 CI 触发矩阵（ci.yml 现状）

| 触发 | unit | api | gate | smoke 巡检 | 部署 |
|---|---|---|---|---|---|
| PR | ✅ | 子集 77 | ✅ | ✗ | ✗ |
| push main/develop | ✅ | 全量 110 | ✅ | ✅ | 仅 main 且 CI 绿 |
| 夜间 cron UTC18（北京 02:00） | ✅ | 全量 110 | ✅ | ✅ | ✗ |
| 每 6 小时 cron | ✗ | ✗ | ✗ | ✅ | ✗ |
| workflow_dispatch | ✅ | 全量 | ✅ | ✅ | 人工兜底 |

smoke job：四步只读断言（首页 200 / health UP / 土地分页 / 产品分页），`continue-on-error: true` 防死锁，目标 `secrets.SERVER_HOST`（服务器已迁腾讯云，不硬编码旧 IP 8.137.114.176）。

### 10.4 环境事实（接手必读，详见记忆 cloudland-local-env-pitfalls）

- **IDEA 从未成功导入 Maven**：自动构建把整仓非 Java 文件复制进 `target/classes` 形成递归嵌套 → jar 打包/JaCoCo 卡死或报重复类。**根治需用户在 IDEA 里 Reload All Maven Projects/重启**；临时解法 `rm -rf target/classes` 或 `mvn spring-boot:run`（不打 jar）。
- 当前 9090 后端由 agent 以 `spring-boot:run` 启动（**非 jar、非用户长期运行方式**）；MySQL/Redis 为 Windows 服务常驻。
- mysql CLI 在 `/d/Software/MySQL 9.0/bin/mysql`，默认 gbk：导 seed 必须 `--default-character-set=utf8mb4`。
- mvn 用 wrapper dists 的 mvn.cmd（`-o` 离线可用）；`env -u VAR mvn.cmd` 静默不执行，须同条命令内 `unset`；宿主注入 SERVER__PORT/HTTP_PROXY 需排除。

### 10.5 未完成（按优先级）

1. **67 个文件未 commit；本地领先 origin/main 1 个 commit（11-16 那次也未推）** → 远端 CI 跑的仍是旧套件，110+83/调度/JaCoCo 均未在流水线证明。push 触发真实 CI+生产部署，**必须用户确认**。
2. IDEA Reload Maven（用户操作，否则本地 jar/JaCoCo 继续被污染卡死）。
3. CI 首跑与连续绿未证明；巡检首跑未验证（生产迁腾讯云后状态未知）。
4. ~~E2E=0；`vue/package.json` 4 个测试依赖待迁~~ → **已完成，见第 11 节**（7 条用例，依赖已迁，lock 已重生成）。
5. 前端单测 0；约 9 个端点无业务级用例（土地/产品 multipart 写盘、验证码发送、找回密码两阶段、后台改订单状态、Excel 导出、订阅/群发）。~~alipay notify~~ 已于 2026-11-17 覆盖，见 11.6；`/alipay/return` 仍缺口（依赖真实沙箱查询，要联网打支付宝）。
6. JaCoCo check 阈值延后（基线收敛后再设）；Allure gh-pages 历史趋势发布未恢复，README.en 三徽章失效。
7. 性能/压测、Prometheus+Grafana+Alertmanager 未做。
8. 文档漂移：docs/wiki 01/08 仍写"无后端单测/E2E 存在/Gitee/-DskipTests"；本文件第 8 节数字过时（以第 10 节为准）。
9. 5 个 xfail 记录的**后端缺陷本体未修**（业务代码未动）：/user/page 泄露密码哈希、land/product 不存在 id 500、msg phone 过滤失效、注册缺 detailedAddress 500。

### 10.6 下一步与跑法

建议顺序：commit（用户确认）→ push（用户确认）→ IDEA reload → 观察 CI 首跑（现在多一个 e2e job）/巡检 → JaCoCo 阈值 → 前端单测。

```bash
# 接口（需 9090 + MySQL + Redis + seed 在线）
cd testing/api && ./.venv/Scripts/python.exe -m pytest          # 110
./.venv/Scripts/python.exe -m pytest -m "smoke or p0"           # 77
# 单元
mvn test                                                        # 83
# seed 重置（幂等）
"/d/Software/MySQL 9.0/bin/mysql" --default-character-set=utf8mb4 -h127.0.0.1 -uroot -p123456 cloudland < testing/api/data/seed/seed_test_accounts.sql
```

## 11. 2026-11-17 Stage 3：E2E 交付（当前最新）

### 11.1 结果

`testing/e2e/` 独立 node 工程落地，**7 条用例 / 3 条链路 / 连续 3 轮全绿（~15s）**，三轮跑完库内零残留（131/132 段账号 0、`E2E产品*` 0）。

2026-11-17 已在 GitHub Actions 真实 runner 上跑通（CI Run #281 五项全绿），排障过程见 11.7。

| spec | 条数 | 覆盖 |
|---|---|---|
| `login.spec.js` | 4 | 客户登录成功 / 密码错误提示 / 登录后进个人中心（`frond` 头回归点）/ 未登录被路由守卫弹回 |
| `register.spec.js` | 2 | 必填为空被前端拦截且**请求未发出** / 完整填写（含三级地址联动）注册成功且新账号能登录 |
| `order.spec.js` | 1 | 加购物车 → 确认弹窗 → 购物车勾选 → 结算 → 订单列表出现且为「未支付」 |

### 11.2 结构

```
testing/e2e/
├── playwright.config.js   # 清代理 / webkit / retries=0 / 失败留 trace
├── fixtures/              # env(环境唯一入口) accounts(seed 镜像) backend(造数清数) index(夹具)
├── pages/                 # home / login-form / register-form / product / trolley / order
└── tests/                 # login / register / order
```

四层同接口层一个思路：`tests → pages → fixtures → 被测系统`，用例层只写业务语言。

### 11.3 四个关键决策（面试可讲）

1. **选择器分层，禁止结构选择器**：文字/placeholder/role 优先，`data-testid`/id 次之，class 链与 nth-child 禁用。本次只给 UI 加了 **2 个** `data-testid`（登录/注册容器）——因为一个组件里塞了两套表单，`getByPlaceholder('请输入你的验证码')` 会同时命中两个，必须显式圈定范围。
2. **数据自造自清**：下单用例用「用例专属临时账号（131 段）+ 用例专属上架产品」，跑完删账号（order2/trolley 对 user 是 ON DELETE CASCADE，删账号即清业务数据）再删产品。不拿 seed 客户下单——它一有历史订单，"列表里有这一行"就不再是有效断言。
3. **等条件不等时间**：`MyTrolley.addOrder()` 有两段写死的 `setTimeout(2000)`，点「去结算」到订单页可用硬成本 4 秒起。用例用 `waitForURL` + 行可见等状态，没有一处 `sleep`。
4. **`retries = 0`**：flake 必须暴露。重试只会把问题变成"偶尔红一下"的噪音。

### 11.4 写 E2E 撞出的三个坑（都写进了 README）

- **el-popper 会搬家**：级联面板展开时节点被移到 `document.body`，`[data-testid=register-form] .el-cascader__dropdown` 打开后变 0 个元素。改用应用自己声明的 `popper-class`（`region-cascader-popper`）全页定位。
- **地址正则不容空格**：前端 `/^\S{5,}$/`，详细地址带空格会被判成"最低填五个字"。
- **提示浮层只活 1 秒**（`duration: 1000`）：靠它断言注册结果既脆又说不清原因。改成 `waitForResponse` 抓 `/user/register` 的 `code`。

### 11.5 顺带发现的第 6 个真实缺陷

E2E 造数时想按手机号定位刚注册的账号，结果每次捞回全表 → **`POST /user/page` 的 `phone` 条件被静默忽略**（`UserMapper.selectByCondition` 只拼了 status/id/power/username 四个 `<if>`，与 `msg/page` 同类）。已固化为 `test_user.py::test_page_users_filter_by_phone_must_narrow_results`（`xfail(strict=True)`），接口层基线由 110 → **111（105 passed + 6 xfailed）**。

### 11.6 依赖与流水线

- `vue/package.json` 移除 4 个测试专用依赖（`@playwright/test`、`@types/node`、`ioredis`、`mysql2`），`npm install --package-lock-only` 重生成 lock（**-433 行**），`npm ci --dry-run` 验证一致——这次没留"只删 json 弄坏 npm ci"的坑。
- `ci.yml` 新增 **e2e job**（MySQL/Redis service + jar + `npm ci` ×2 + `playwright install --with-deps webkit`，前端由 Playwright `webServer` 自己拉起），`gate` 纳入 e2e 结论。触发矩阵同 10.3：PR / push / 夜间跑，6 小时巡检跳过。
- ~~未验证项：e2e job 尚未在真实 runner 跑过~~ → **2026-11-17 已在真实 runner 跑通**，见 11.7。
- **顺手修掉的生产配置缺陷**：`vue/.env.production` 原为 `http://8.137.114.176/api`（旧服务器绝对地址），构建时焊进 bundle。服务器迁腾讯云后线上前端每个接口都打旧 IP，而 smoke 巡检直连 `SERVER_HOST`，所以**巡检绿、用户侧全挂**。改相对路径 `/api`，由 nginx 同源反代命中 backend，CORS 预检一并消失。验证：生产模式构建后 grep 产物，旧 IP 出现 0 次、基址为 `"/api"`。
- **服务器侧核对（2026-11-17 19:10，登机实测）**：旧 IP `8.137.114.176` 已完全不通（curl 000），而 `.env` 的 `ALIPAY_NOTIFY_URL` / `ALIPAY_RETURN_URL` / `CORS_ALLOWED_ORIGINS` 仍指向它——**支付回调与支付后跳转全打在死地址上**。已备份 `.env.bak.202611171911` 后改到 `43.136.135.75` 并重建后端（health 6s 就绪）。仓库侧同步 `nginx.conf` 的 `server_name`、`application.yml` 的支付宝默认值、wiki/08 与本文档 §6（`72a31fd`，已部署，服务器容器与 nginx 配置均核验为新值）。
  另确认：容器内 `CODE_MASTER_ENABLED=false`（生产没开万能验证码），`ALIPAY_APP_ID=9021000123611619` 为沙箱应用。
- **支付全链路已实测通过（2026-11-17 19:36，用户手动沙箱支付）**：`POST /api/alipay/pay` 金额服务端校验 `server=45.0 / client=45.00` → 19:36:32 回调落到 `http://43.136.135.75/api/alipay/notify`，`trade_status=TRADE_SUCCESS`、**验签结果 true** → 订单 `id=74` 由 `status=0` 变 `1`、`pay_time=19:36:32`。这条同时验证了三件事：回调地址修复生效、验签逻辑正确、金额重算在真实链路成立。
  ~~仍未覆盖的是自动化~~ → **2026-11-17 已补上**：`tests/test_alipay_notify.py` 3 条（验签通过→改单、重复投递幂等、签名后篡改被拒、非 TRADE_SUCCESS 忽略），接口层 111 → **114（108 passed + 6 xfailed）**。
  实现要点：测试侧用 `common/alipay_signer.py` 自签 RSA2 报文——待签串必须与 Java `AlipaySignature.getSignContent` 逐条对齐（key 升序、剔除 `sign`/`sign_type` 与空值），差一条就是永久 failure 且日志只说"验签失败"。
  密钥配对是前提：CI 里私钥/公钥同源；本地 `.env` 是「应用私钥 + 支付宝平台公钥」，不是一对，所以用例自带 `signing_pair_usable()` 自检——不配对本地 skip，**CI 缺密钥则直接 fail**（静默跳过等于这条链路没覆盖）。
  验这个过程时踩到两个坑：① `spring-boot:run` 下用 `export` 传 ALIPAY_* 没生效，改成命令行参数 `--alipay.publicKey=` 才验过（这也解释了为什么要在独立端口起第二实例验证）；② 篡改用例一开始把 `total_amount` 改成"0.01"，而默认值本来就是 0.01，等于没篡改、用例假通过——已加防呆断言。

### 11.7 CI 首跑排障：4 轮才绿（面试可讲的完整链路）

第一次真正把本地体系推上 runner，暴露的全是"本地环境替你兜住的假设"。

| 轮次 | 现象 | 根因 | 修法 |
|---|---|---|---|
| #274 | api / e2e 都死在 `Initialize containers`，pytest 一行没跑 | `services.redis.options` 里写了 `--requirepass`，它被拼进 `docker create`，而这是 redis-server 的参数不是 docker 的参数 → exit 125 | 改成独立 step `docker run ... redis-server --requirepass`，自己轮询 ping 等健康 |
| #275 | api 绿了，e2e 挂在 `playwright install --with-deps`，只有 "exit code 1" | `ubuntu-latest` 已开始迁往 Ubuntu 26，`--with-deps` 的发行版→包名映射对不上 | e2e 固定 `ubuntu-22.04`（可复现优先于最新） |
| #277 | e2e 挂在 `运行 E2E`，仍无日志 | 前端由 Playwright 的 webServer 拉起，超时只报一句 "Timed out waiting for http://…"，看不到 webpack 卡在哪 | 前端改为独立 step 启动、等 300s、日志落盘；`E2E_SKIP_WEB_SERVER=1` 跳过自启 |
| #279 | 用例真跑了：`#user-msg` 文本是 `" 欢迎您， "`，等 `.el-notification` 超时 | **浏览器侧接口全 404**：仓库里没有 `vue/.env.local`（未跟踪的本地文件），CI 上 `VUE_APP_API_BASE_URL` 为空 → 走 app.js 兜底值 `http://localhost:9090`，少了 `/api` | CI 显式注入 `.../api`；app.js 兜底值补 `/api`（默认值本身就是 bug） |
| #281 | **全绿**（unit / api / e2e / smoke / gate） | — | — |

三个教训，比"CI 绿了"更值钱：

1. **"只给 exit code" 不是没办法，是没设计观测点**。第 3 轮开始把失败行打成 `::error::` 注解——注解能被 API 读到，step summary 不能。排障通道要提前设计，不能等挂了再想办法。
2. **未跟踪文件是"只能在我机器上跑"的头号来源**。`.env.local` 覆盖了一个错误默认值，本地永远绿，CI 永远红。
3. **本地复现优先于猜**。第 4 轮之前先在本地做了两个反证：干净重装依赖后 dev server 能编译、用 `127.0.0.1:8080` 跑 7/7 全绿——把"依赖被删少了""地址/CORS 问题"两个假设直接排除，才把范围收窄到 API 基址。

### 11.8 下一步

1. commit（用户确认）→ push（用户确认）→ 观察 CI 首跑（unit/api/e2e/gate 四绿）→ IDEA Reload Maven。
2. JaCoCo check 阈值（基线 25.3% 收敛后按包设）。
3. 前端单测（当前 0）：`utils/auth.js` 登录态缓存、`config/app.js` URL 归一化都是纯函数。
4. E2E 扩容：后台（登录 → 土地/产品/订单管理）、删单/支付跳转分支。
5. 后端 6 个 xfail 缺陷本体，业务代码仍未动。
