# 测试体系：从 0 到全面

> 2026-11-16 重建。旧体系快照在 git tag `v1-legacy-test-suite`，随时可 `git show <tag>:<path>` 对照。

## 一句话说明这份代码的定位

这不是"给项目补一些测试"，而是把**测试当成一套工程系统**来建：
分层、有门禁、有数据治理、能自我纠错（缺陷会自己提醒你）。
面试时能讲的不是"我写了 82 条用例"，而是"每一层为什么这么分、边界在哪、怎么保证它三年后还能跑"。

## 为什么这样分层

测试代码最容易写成一坨：用例里直接拼 URL、断言散落、造数复制粘贴。三个月后没人看得懂，更别说维护。

专业框架的核心不是"用了什么库"，而是**每一层只做一件事，依赖方向单一**：

```
tests/       用例层：描述"测什么"        ← 只读业务语言，不关心 HTTP 细节
services/    业务层：封装"怎么调"        ← 只拼参数，不做断言
common/      驱动层：封装"怎么发/怎么判"  ← 唯一碰 requests 的地方
data/        数据层：用例输入与期望      ← 改数据不改代码
```

依赖只能向下：`tests → services → common`。反向引用一次都不要有，否则改驱动层会炸用例。

## 目录

```
testing/
├── README.md                        # 本文档
├── api/                             # 接口测试（Python / pytest）
│   ├── pytest.ini                   # 收集规则 + marker（只定义真正在用的）
│   ├── requirements.txt             # 依赖及用途，每条都写清为什么需要
│   ├── conftest.py                  # 夹具：三种角色登录态 + 临时账号工厂
│   ├── common/                      # 驱动层
│   │   ├── config.py                #   环境配置、seed 账号，唯一读环境变量的地方
│   │   ├── http_client.py           #   Session / token / 日志 / Allure 报文
│   │   ├── assertions.py            #   双层断言：HTTP 状态码 + 业务码
│   │   ├── data_loader.py           #   YAML 加载
│   │   └── data_factory.py          #   造不撞车的测试数据
│   ├── services/                    # 业务层（一个 Controller 一个类）
│   │   ├── auth_service.py          #   登录、注册、按角色登录
│   │   ├── authz_service.py         #   鉴权边界探测（不碰业务）
│   │   ├── user_service.py          #   用户管理
│   │   ├── land_service.py          #   土地
│   │   ├── product_service.py       #   产品
│   │   ├── order_service.py         #   订单（含下单 create，键名小写 pid/uid）
│   │   ├── pay_service.py           #   支付宝 pay（只测本地签名，不碰回调）
│   │   └── msg_service.py           #   留言/订阅（含"危险接口"隔离）
│   ├── data/
│   │   ├── auth/login.yaml          #   5 条登录场景数据
│   │   └── seed/seed_test_accounts.sql  # 幂等种子账号（CI 与本地共用）
│   └── tests/                       # 用例层
│       ├── test_auth.py             #   认证：登录/注册/token 续期通道
│       ├── test_authz.py            #   鉴权矩阵：401/402/403（最重要的一组）
│       ├── test_contract.py         #   码表契约：Code.java vs code.js（离线）
│       ├── test_user.py             #   用户管理 + 越权 + 改密 + 查重/改绑
│       ├── test_land.py             #   土地公开查询
│       ├── test_product.py          #   产品公开查询
│       ├── test_order.py            #   订单归属 + 下单/删单真实回滚
│       ├── test_trolley.py          #   购物车增查删 + 越权删
│       ├── test_pay.py              #   支付金额防篡改/归属/交易映射 TTL
│       └── test_msg.py              #   留言限流/搜索
└── e2e/                             # 端到端测试（Playwright，独立 node 工程）
    ├── playwright.config.js         #   清代理、webkit、失败留 trace
    ├── fixtures/
    │   ├── env.js                   #   环境变量唯一入口（baseURL/API/万能码）
    │   ├── accounts.js              #   seed 账号镜像（与 seed SQL 同一份事实）
    │   ├── backend.js               #   只用它造数/清数，不做断言
    │   └── index.js                 #   夹具：临时账号、可售产品、自动清理
    ├── pages/                       #   Page Object：一屏一个类
    │   ├── home.page.js             #   首页 + 登录入口
    │   ├── login-form.page.js       #   登录弹窗
    │   ├── register-form.page.js    #   注册表单（含三级地址联动）
    │   ├── product.page.js          #   产品详情 + 加购
    │   ├── trolley.page.js          #   购物车勾选 + 结算
    │   └── order.page.js            #   我的订单
    └── tests/
        ├── login.spec.js            #   登录成功/密码错误/进入个人中心/守卫拦截
        ├── register.spec.js         #   前端必填拦截（不发请求）+ 真实注册后能登录
        └── order.spec.js            #   加购 → 结算 → 订单列表可见

src/test/java/                       # 单元测试（JUnit 5）
├── com/cloudland/controller/result/
│   ├── CodeTest.java                #   码表白名单契约（含反射扫描）
│   └── ResultTest.java              #   统一响应结构契约
├── com/cloudland/service/impl/
│   └── Order2ServiceImplTest.java   #   金额/库存/归属/回滚不变量（Mockito）
├── com/cloudland/config/
│   └── StoragePropertiesTest.java   #   存储路径归一化与拼接
└── com/cloudland/util/
    ├── JwtUtilsTest.java            #   JWT 签发/解析/篡改/过期
    └── FileUtilTest.java            #   命名/打包/删除契约（@TempDir 隔离）

.github/workflows/
├── ci.yml                           # 单元 + 接口 + 端到端 + 汇总门禁
└── build-deploy.yml                 # workflow_run 监听 CI，CI 红则部署不触发
```

## 实测基线（2026-11-17 本地）

| 层 | 用例数 | 结果 | 耗时 | 依赖 |
|---|---|---|---|---|
| 单元（JUnit 5） | 83 | 全绿 | ~1s | 无外部依赖；JaCoCo 行覆盖基线 25.3% |
| 接口（pytest） | 111（105 passed + 6 xfailed） | 全绿 | ~6s | MySQL + Redis + 后端 |
| 端到端（Playwright / WebKit） | 7 | 连续 3 轮全绿 | ~15s | 后端 + MySQL + Redis + 前端 |
| 稳定性 | — | 三轮结果一致、临时数据零残留 | — | — |
| 门禁有效性 | — | 故意让一条单测失败 → `BUILD FAILURE` | — | 已实测 |

`xfailed` 是**有意的**：那是 6 条已确认缺陷的"缺陷记录用例"，见下一节。

## 这套框架抓到的真实缺陷（6 个，全部已固化为用例）

| # | 缺陷 | 影响 | 用例 |
|---|---|---|---|
| 1 | `POST /user/page` 返回 BCrypt 密码哈希 | 哈希外泄可离线爆破；而 `/user/id` 会置空，同一份数据两套标准 | `test_user.py::test_page_users_must_not_leak_password_hash` |
| 2 | `GET /land/{id}` 不存在的 id 返回 500 | 每个坏请求都触发异常告警，真故障被噪音淹没 | `test_land.py::test_land_detail_not_found_should_not_500` |
| 3 | `GET /product/{id}` 同上 | 同源：空列表取 `get(0)` 抛 `IndexOutOfBoundsException` | `test_product.py::test_product_detail_not_found_should_not_500` |
| 4 | `POST /msg/page` 的 `phone` 条件被静默忽略 | 后台按手机号搜索返回全部数据，运营据此判断"这人没留过言" | `test_msg.py::test_msg_page_filter_by_phone_returns_nothing` |
| 5 | 注册缺 `detailedAddress` 返回 500 | 服务端无参数校验，5xx + 堆栈外泄 | `test_auth.py::test_register_without_detailed_address` |
| 6 | `POST /user/page` 的 `phone` 条件被静默忽略 | 与 #4 同源：后台按手机号核对用户，拿到的是全表，会对错的人做改绑/禁用 | `test_user.py::test_page_users_filter_by_phone_must_narrow_results` |

**#6 是怎么被发现的**（值得一提的路径）：写 E2E 时要按手机号定位"刚注册的临时账号"以便清理，结果每次捞回来的都是全表。造数工具反过来把产品缺陷暴露了出来——这也是为什么测试框架要自己造数、自己清数，而不是依赖现成数据。

**为什么用 `xfail(strict=True)` 记缺陷而不是写进文档**：
后端一修复，该用例会变成 XPASS，strict 模式立即让测试转红，强制你回来把它改成正式断言。
缺陷记录因此不会"烂在文档里"，而是变成一条会自己催你的待办。

## 怎么跑

前置：后端 `http://localhost:9090/api` + MySQL + Redis 在线，199 段种子账号已入库。

```bash
# 首次：建虚拟环境（.venv 已进 .gitignore）
cd testing/api
python -m venv .venv
./.venv/Scripts/python.exe -m pip install -r requirements.txt   # Windows
# source .venv/bin/activate && pip install -r requirements.txt  # macOS/Linux

# 种子账号（幂等，可重复执行；把被测试改坏的账号重置回基线）
mysql -h127.0.0.1 -uroot -p123456 cloudland < data/seed/seed_test_accounts.sql

# 接口测试
./.venv/Scripts/python.exe -m pytest                            # 全量 110 条
./.venv/Scripts/python.exe -m pytest -m smoke                   # 核心链路 47 条
./.venv/Scripts/python.exe -m pytest -m "smoke or p0"            # CI 里 PR 跑的集合，77 条
./.venv/Scripts/python.exe -m pytest --alluredir=allure-results  # 带报告
./.venv/Scripts/python.exe -m pytest --cov=common --cov=services --cov-report=term-missing
```

```bash
# 单元测试（项目根目录）
mvn test                                      # 83 条
mvn test -Dtest=JwtUtilsTest                  # 只跑一个类
mvn test -Dtest=CodeTest#whitelistCoversAllConstants   # 只跑一个方法
```

```bash
# 端到端（testing/e2e，独立 node 工程）
cd testing/e2e
npm ci                                        # 首次
npx playwright install webkit                 # 首次（CI 用 --with-deps）

npx playwright test                           # 全量 7 条（自动拉起前端 dev server）
npx playwright test tests/login.spec.js       # 只跑登录
npx playwright test --headed                  # 看着浏览器跑，调选择器时用
npx playwright test --ui                      # 时间轴调试器
npx playwright show-report                    # 上次的 HTML 报告
```

> E2E 也不需要手工起前端：`playwright.config.js` 里配了 `webServer`
> （`npm --prefix ../../vue run serve`），本地已在跑就复用，CI 里自己起一份。
> 后端仍要自己起（9090），因为它是被测系统的一部分，不该被测试框架"顺手启动"——
> 那样会掩盖"部署形态不对"的问题。

> 后端启动注意：宿主机注入了 `SERVER__PORT`，会让 Spring 顶掉 `application.yml` 里的 9090。
> 必须显式排除：`env -u SERVER__PORT java -jar target/Cloudland-0.0.1-SNAPSHOT.jar --server.port=9090`
>
> 代理注意：宿主机注入了 `HTTP_PROXY` 且没有 `NO_PROXY`，本地 curl 会被劫持报
> "os error 10061"。加 `--noproxy '*'`，或在启动后端/前端前 `unset` 掉代理变量。
>
> MySQL 客户端注意：Windows 下 mysql CLI 默认字符集是 gbk，导入 UTF-8 的 seed 会报
> "Data too long"。必须加 `--default-character-set=utf8mb4`。
>
> IDEA 注意：若 IDEA 未成功导入 Maven（旧模块把整仓当 source root），它的自动构建会把
> 全仓文件复制进 `target/classes` 形成递归嵌套，导致 jar 打包/JaCoCo 卡死或报重复类。
> 在 IDEA 里 Reload All Maven Projects（或重启）根治；CLI 侧可先 `rm -rf target/classes` 再构建。

## 学习顺序（建议按这个顺序读，每步都有明确的"看懂标志"）

1. **`common/http_client.py` + `common/assertions.py`**
   看懂标志：能说清"为什么发请求和判结果要分两个文件"。
   关键点：本项目业务失败也返回 HTTP 200，只判状态码会把"密码错误"当成功。

2. **`services/auth_service.py`**
   看懂标志：能说清"为什么这个文件里一个 assert 都没有"。
   关键点：同一个 `login()` 要同时服务"登录成功"和"密码错误"两条用例。

3. **`data/auth/login.yaml` + `tests/test_auth.py`**
   看懂标志：能说清"什么场景该数据驱动、什么场景该显式写"。
   关键点：正常流断言多且各自独特 → 显式写；异常流共同点强 → YAML 化。

4. **`tests/test_authz.py`（最重要的一组）**
   看懂标志：能解释"为什么探测权限可以不带参数"。
   关键点：拦截器在参数绑定**之前**执行，所以 401/403 与参数无关 —— 这让权限矩阵可以零数据写入、毫秒级跑完。

5. **`conftest.py`**
   看懂标志：能说清 session 级与 function 级夹具各自该放什么。
   关键点：会被用例改坏的数据（临时账号）必须 function 级，否则前一条把账号禁了、后面全连坐。

6. **`src/test/java/.../CodeTest.java`**
   看懂标志：能解释"反射扫描"那条用例在防什么。
   关键点：`Code.isSuccess` 是手写白名单，加新码忘了同步 → 新接口被判失败，编译期看不出来。

7. **`.github/workflows/ci.yml` + `build-deploy.yml`**
   看懂标志：能画清"测试红 → 镜像不推 → 服务器不动"的链路。
   关键点：门禁靠 `workflow_run` 的事件依赖实现，不是靠人记得先看 CI。

### 自己动手（检验是否真看懂）

- 给 `/user/employee` 加一条用例：客户调用返回 403，员工调用返回 200。
  只改 `tests/`，不要动 `common/` 或 `services/`。
- 把 `test_login_failure` 里的 `wrong_password` 从 YAML 删掉，观察 `-k` 过滤后的数量变化。
- 故意把 `api` 断言里的 `20005` 改成 `20006`，看失败信息是否够定位问题。

## 五个阶段的进度

| 阶段 | 内容 | 状态 |
|---|---|---|
| 1 | 接口框架：三层分离 + 双层断言 + 数据驱动 + Allure | ✅ 111 条（含契约/支付/购物车/订单写路径） |
| 2 | 单元测试：JUnit 5 打纯函数、JWT、码表、订单不变量 | ✅ 83 条 + JaCoCo 度量，门禁已实测有效 |
| 3 | E2E：Playwright 覆盖登录、注册、下单 | ✅ 7 条 / 连续 3 轮全绿（含三级地址联动、自动造数清数） |
| 4 | 数据治理：幂等 seed、造数工厂、自动清理 | ✅ 工厂 + 幂等 seed + API 清理 + Redis 键清理 + E2E 临时账号/产品自动删，三轮零残留 |
| 5 | 质量门禁：CI 分层调度 + 部署门禁 | ✅ ci.yml 五层（unit/api/e2e/gate/smoke）+ 调度 + workflow_run 闭环（仅 push 触发部署） |

## Stage 3（E2E）为什么这么设计

E2E 最容易写成负资产：一堆 CSS 选择器、一堆 `sleep`、红了没人信。
所以这一层刻意做了四件事。

**1. 选择器分层，禁止结构选择器**

```
文字 / placeholder / role      ← 优先。用户看得见的东西不会因为改样式失效
data-testid / id               ← 其次。UI 与测试的显式契约（本次给登录/注册容器加了 2 个）
class 链 / nth-child / 兄弟结构  ← 禁止。今天能跑，改一次布局全红
```

**2. 数据自己造、自己删，不碰 seed 数据**

下单用例用「一条用例专属的临时账号 + 一个用例专属的上架产品」，跑完删掉。
拿 seed 客户下单是错的：它一有历史订单，"订单列表里有这一行"就不再是有效断言。
清理失败只告警不失败——那是环境问题，不该把一条真实通过的用例染红。

**3. 等条件，不等时间**

`MyTrolley.addOrder()` 里有两段写死的 `setTimeout(2000)`，从点「去结算」到订单页可用硬成本 4 秒起。
用例用 `waitForURL` + 行可见来等**状态**，不是 `sleep 5000`。
将来前端把 setTimeout 换成 Promise，用例一行都不用改。

**4. 失败必须留证据**

`trace: retain-on-failure` + 截图 + 录像，且 `retries = 0`。
重试会把 flaky 变成"偶尔红一下"的噪音，三个月后没人敢信这套用例；真不稳就修根因。

### 写 E2E 时踩到的三个真实坑（都会反复出现）

| 坑 | 现象 | 根因 |
|---|---|---|
| el-popper 会搬家 | `[data-testid=register-form] .el-cascader__dropdown` 打开后变成 0 个元素 | 弹层展开时节点被移到 `document.body`，作用域失效。改成用应用声明的 `popper-class` 全页定位 |
| 地址校验正则不许空格 | 详细地址填「…天府大道 1 号 E2E」被判成"最低填五个字" | 前端 `/^\S{5,}$/`，`\S` 不含空格 |
| 提示浮层活 1 秒 | 断言"注册成功"时浮层已经消失，看不出失败原因 | 断言改为 `waitForResponse` 抓 `/user/register` 的 `code`，浮层只做辅助佐证 |

### 与接口层的分工（别让 E2E 越界）

- 业务规则（金额重算、库存、越权、码表契约）→ 接口层。快、稳、能穷举。
- 用户能不能点通、登录态在页面间是否延续、前端校验有没有真拦住 → E2E。
- 同一个规则不要两边都写：一旦两边都写，改动时你要改两处，且不一致时说不清谁对。

## 下一步

1. **前端单测**（当前 0）：`vue` 里的 `utils/auth.js` 登录态缓存、`config/app.js` URL 归一化都是纯函数，适合 Vitest + @vue/test-utils。
2. **JaCoCo 阈值**：现在只有 25.3% 的度量，没有 check。基线收敛后按包设阈值，让它成为真门禁。
3. **E2E 扩容**：后台（登录 → 土地/产品/订单管理）与"删除订单"“支付跳转"两条分支还没覆盖。
4. **性能与可观测**：k6/vegeta 压 `/land/page`、`/product/page`；Prometheus + Grafana + Alertmanager 尚未落地。
5. **后端 6 个 xfail 缺陷本体**：业务代码一行未动，修完把 xfail 改成正式断言。
