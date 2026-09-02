# 质量效能路线图与进度跟踪

> 用途：跨会话接手文档。任何新对话读本文件即可了解方向决策、整体计划、当前进度与下一步任务，无需重新分析。
> 建立:2026-09-02 · 最近更新:2026-09-02(W2 收官)

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

### W3-5（API 自动化层）

```
testing/api/
├── conftest.py          # base_url 从 env 读；登录 fixture（token 管理）
├── common/
│   ├── client.py        # requests 封装：token 头 + updatedToken 滑动续期
│   └── assertions.py    # 双层断言：HTTP 状态 + Code 业务码
├── test_auth.py         # 登录双模式/token 刷新/401·402·403 语义/万能码开关
├── test_user.py         # 注册查重/改密防越权/power=0 提权锁定（含提权漏洞回归用例）
├── test_order.py        # 下单锁地/扣库存/删单回滚/归属校验
├── test_pay.py          # 金额服务端重算/交易映射幂等
└── requirements.txt
```

- 第一条用例就是提权漏洞回归：注册带 `power=2` → 断言入库 power=0

- 环境依赖：后端 9090 + MySQL + Redis（无需前端）

- 测试账号复用 seed.sql 的 199 段体系

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
| 测试           | `cd vue && npm run test:ci`（54 用例）/ `npm run test:debug`                           |
| 本地 MySQL     | root / 123456，库名 cloudland                                                         |
| 本地 Redis     | 6379，密码 123456                                                                     |
| 种子测试账号       | 19900000001（客户）/ 19900000002（员工）/ 19900000003（管理员），密码均 `Test@123456`               |
| ⚠️ 演示账号勿用于测试 | 18140213287 在本地库 power 已非 2，admin 接口会 403                                          |
| push 即部署     | push 到 main 触发 Gitee Go → deploy.sh → 生产 8.137.114.176                             |

## 7. 进度日志

- **2026-09-02**：W1 全部完成（原计划 W1-2，提前收口）。方向决策定稿（测开×DevOps 融合）。2 commit 上线。54/54 全绿。
- **2026-09-02**:W2 收尾完成,`test:ci` 62/62 全绿。`.env.test` 体系落地(含 Playwright .mjs 管道坑位记录);cleanup helper 补齐调用方用例;线上冒烟确认两修复生效。下一步:W3-5 API 自动化层(pytest,首条用例=提权回归)。
- **2026-09-02**:W3 开工:API 自动化层脚手架落地(testing/api/,pytest+requests+allure-pytest),15 用例全绿——test_auth.py(登录双通道/HTTP 401·403 语义/token 滑动续期)+ test_user.py(提权回归/注册查重/默认值锁定/seed 保护)。**顺手发现并修复真实缺陷**:不存在手机号+验证码头登录 → redisTemplate.delete(null) 抛 500,已改为返回 PHONE_NO_EXIST(UserServiceImpl)。
- **2026-09-02**:W3 主干完成:test_order.py(锁地/重复下单/删单回滚放地/扣库存/超卖拒绝/库存回补/越权删单 POWER_ERR)7 用例 + test_pay.py(金额服务端重算防篡改/Redis 交易映射 TTL/已支付拒绝/越权支付拒绝/不存在订单)5 用例,API 层 27/27 全绿。**顺手修复后端缺陷**:AlipayController.pay 业务规则违规(订单不存在/已支付/归属失败)原为未捕获 RuntimeException→HTTP 500,现返回 ADD_ERR 业务码。**造数踩坑记录**:①product.a_id 为 NOT NULL 外键,造产品必须带 aId;②LandMapper.selectById 是 INNER JOIN user(代理人),land.employee_id 缺省时查不到行→下单 NPE 500,造地必须带 employeeId;③@RequestBody 的 Lombok 属性(pId/uId)Jackson 绑定键为全小写 pid/uid,与前端一致;④redis-py 6.x 默认 RESP3 发 HELLO,本地 Redis 不支持需 protocol=2。待办:契约用例(Code.java vs code.js 码表比对)。

