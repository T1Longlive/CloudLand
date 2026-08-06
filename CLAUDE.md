# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

本文件为 Claude Code (claude.ai/code) 在此代码库中工作时提供指导。

## 项目概述

Cloudland 是一个全栈土地/房产管理平台，后端使用 Spring Boot，前端使用 Vue.js。系统处理土地列表、产品销售、订单管理、用户认证和消息通知等功能。

## 架构

### 后端 (Spring Boot 2.7.14 + Java 8)
- **端口**: 9090
- **结构**: 标准 MVC 模式
  - `controller/` - REST API 接口
  - `service/` 和 `service/impl/` - 业务逻辑层
  - `mapper/` - MyBatis-Plus 数据访问层，XML 映射文件在 `resources/com/cloudland/mapper/`
  - `pojo/` - 领域实体（User, Land, Product, Order2, Msg, Trolley 等）
  - `config/` - Spring 配置类
  - `Interceptor/` - JWT 认证拦截器
  - `util/` - 工具类（JwtUtils, EmailUtils, FileUtil, OrderExporter 等）

### 前端 (Vue 2.6)
- **端口**: 8080（开发服务器）
- **位置**: `vue/` 目录
- **UI 框架**: Element UI + `@opentiny/vue`（两个组件库共存）
- **详细指导**: `vue/CLAUDE.md` 包含前端专用的架构和约定
- **结构**:
  - `src/views/backend/` - 管理员/员工视图
  - `src/views/frontend/` - 客户端视图
  - `src/components/` - 共享组件（Top, Top2）
  - `src/router/` - Vue Router 配置
  - `src/request/` - Axios 实例和拦截器
  - `src/config/app.js` — 统一配置（API 地址、资源 URL），通过 `APP_CONFIG` 导出
  - `src/utils/auth.js` — 认证状态管理（token 存储、登录、登出）
  - `src/constants/code.js` — 业务状态码常量（与后端 `Code.java` 一致）

### 路由结构
两个顶层布局：
- `/` → 前台商城：首页、土地列表/详情、产品列表/详情、用户中心、忘记密码
- `/backend` → 后台管理：用户管理、土地、产品、订单、消息推送

路由守卫在 `src/router/index.js`，访问后台和 `/user` 需要登录验证。

### 数据库
- **MySQL**，数据库结构在 `cloudland.sql`
- **主要表**: user, land, land_type, product, order2, trolley, msg, msg_send, cloudland_file
- **ORM**: MyBatis-Plus，已启用 SQL 日志

## 开发命令

### 后端
```bash
# 构建项目（跳过测试）
mvn clean package -DskipTests

# 运行应用
mvn spring-boot:run

# 主类: com.cloudland.CloudlandApplication
```

> 注意：后端无测试类，`mvn test` 不会执行任何测试。

### 前端
```bash
cd vue

# 安装依赖
npm install

# 启动开发服务器 (http://localhost:8080)
npm run serve

# 生产环境构建
npm run build

# 运行 Playwright E2E 测试（需要后端 + 前端 + MySQL + Redis 全部运行）
npx playwright test                    # 运行全部测试
npx playwright test tests/login.spec.js  # 运行单个测试文件
npx playwright test --project=webkit   # 仅在 webkit 运行
```

> 测试配置在 `vue/playwright.config.js`，测试文件在 `vue/tests/`。测试 helpers 使用 `ioredis` 直接写入验证码绕过邮件发送，使用 admin API 清理测试数据。测试项目：webkit 和 Microsoft Edge（Edge 启用 slowMo: 800ms）。baseURL: `http://localhost:8080`，workers: 1（串行执行）。

## 运行时依赖

本地开发需要以下服务运行：
- **MySQL 8.0** — 数据库，初始化脚本: `cloudland.sql`
- **Redis 7** — 缓存/会话，默认端口 6379

## Docker 部署

```bash
# 完整栈（后端 + 前端 + MySQL + Redis）
docker-compose up -d
```

- 前端通过 Nginx 在端口 80 提供服务，`/api/` 代理到 `backend:9090`
- 文件上传存储在 Docker 卷 `cloudland-files`（挂载于 `/cloudland-files`）
- Docker 镜像从 `docker.1panel.live` 镜像拉取（非 Docker Hub 直连）

## CI/CD

Gitee Go 部署流水线（`.gitee/workflows/deploy.yml`）：push 到 `main` 分支时自动触发，通过 SSH 连接服务器执行 `git pull origin main && docker-compose up -d --build`。需要配置 Secrets：`SERVER_HOST`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_PROJECT_PATH`。

### 部署脚本
`scripts/deploy.sh` 是生产环境部署脚本，执行以下流程：
- 拉取代码并硬重置到目标分支
- 构建后端和前端 Docker 镜像
- 停止宿主机 Nginx（避免端口 80 冲突）
- 启动所有容器并等待前端容器就绪
- 清理未使用的 Docker 镜像

## 配置

### 环境变量
应用支持基于环境的配置。主要变量：

- **数据库**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Redis**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `REDIS_TIMEOUT`
- **邮件**: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- **JWT**: `JWT_SIGN_KEY`, `JWT_EXPIRE`, `JWT_WEEK`
- **CORS**: `CORS_ALLOWED_ORIGINS`（逗号分隔）
- **文件存储**: `FILE_STORAGE_ROOT`（默认: `D:/CloudLandFile`）

### 前端环境变量
在 `vue/` 目录下创建 `.env.local` 覆盖默认值：
- `VUE_APP_API_BASE_URL` — 后端地址（默认 `http://localhost:9090`）
- `VUE_APP_FRONTEND_BASE_URL` — 前端地址（默认 `window.location.origin`）

配置统一在 `src/config/app.js` 中读取，通过 `APP_CONFIG` 导出，包含 `apiBaseUrl`、`resourceUrls` 等。资源文件 URL 统一通过 `APP_CONFIG.resourceUrls` 拼接，不要硬编码后端地址。

### 本地配置覆盖
在项目根目录创建 `config/application-local.yml` 来覆盖配置，无需修改 `application.yml`。此文件已被 gitignore，通过 `spring.config.import` 加载。

### 其他参考文件
- `API.md` — 后端 API 接口文档
- `.env.example` — 环境变量模板，复制为 `.env` 后修改

## 认证与授权

### 基于 JWT 的认证
- **拦截器**: `MyInterceptor` 验证所有请求的 JWT 令牌，以下路径除外：
  - `/resource/**`（静态文件）
  - `/land/page`, `/land/{id}`（公开的土地列表）
  - `/product/page`, `/product/{id}`（公开的产品）
  - `/msg`, `/msg/mail`（公开的消息接口）
  - 包含以下关键词的 URL: `login`, `register`, `code`, `forgetPassword`

- **令牌请求头**: `token`（不是 `Authorization`）
- **令牌声明**: `id`（用户 ID）, `power`（用户角色/权限级别）
- **自定义请求头**: CORS 过滤器允许的自定义请求头: `token`, `remember`, `frond`, `code`, `contact`, `forgetPassword`
- **响应头**: `updatedToken` 用于令牌刷新

### 用户角色（power 字段）
- `0` - 客户
- `1` - 员工
- `2` - 管理员

### 用户状态
- `0` - 禁用（账号被封）
- `1` - 激活

## 文件存储

文件存储在项目目录外，由 `FILE_STORAGE_ROOT` 指定的位置：
- **用户头像**: `{FILE_STORAGE_ROOT}/UserIcon/`
- **土地文件**: `{FILE_STORAGE_ROOT}/LandFile/`
- **产品文件**: `{FILE_STORAGE_ROOT}/Product/`

这些路径映射到 URL：
- `/resource/userFile/**` → 用户头像
- `/resource/landFile/**` → 土地文件
- `/resource/productFile/**` → 产品文件

## 核心业务概念

### 土地管理
- **土地类型**: 农用地、建设用地、商业用地、公共管理与公共服务用地、水域及水利设施用地、其他土地
- **土地状态**: 0 = 可用, 1 = 已租用/不可用
- **定价**: 每平方米每天
- **关系**: 每块土地有一个所有者（`a_id`）和可选的员工代理（`employee_id`）

### 订单
- **实体**: `Order2`（不使用 `Order` 以避免 SQL 关键字冲突）
- **类型**: 土地订单和产品订单分别跟踪
- **导出**: `OrderExporter` 工具使用 Apache POI 生成 Excel 报表

### 消息
- **系统**: 双表设计，`msg`（消息模板）和 `msg_send`（已发送消息）
- **投递**: 通过 Spring Mail 发送邮件通知

## API 约定

### 统一响应格式
所有接口返回 `Result` 对象（定义在 `controller/result/Result.java`）：
```json
{
  "data": {},        // 响应数据
  "code": 10004,     // 业务状态码（10000+ 范围，避免与 HTTP 状态码冲突）
  "msg": "查询成功"   // 消息说明
}
```

业务状态码定义在 `controller/result/Code.java`，前端对应常量在 `vue/src/constants/code.js`。主要范围：
- **10000-10999** — CRUD 操作（10001=添加成功, 10002=删除成功, 10003=修改成功, 10004=查询成功, 对应失败码 +4）
- **20000-20999** — 认证授权（20005=登录成功, 20006=注册成功, 20003=令牌过期, 20002=密码错误）
- **30000-30999** — 邮件通知（30001=发送成功, 30003=验证码错误）

### 文件上传模式
土地和产品接口使用 `multipart/form-data`，文件和 JSON 数据混合传递：
- 字符串参数（如 `land`、`product`、`user`）是 **JSON 字符串**，不是对象
- 文件参数（如 `landFiles`、`imageFiles`、`productImg`、`userIcon`）是 `File` 或 `File[]`
- 单文件大小限制: 60MB

详细接口文档见 `API.md`。

## 重要注意事项

### 安全
- 密码通过 `BCryptPasswordEncoder` 使用 BCrypt 加密
- JWT 签名密钥在生产环境应更改（设置 `JWT_SIGN_KEY` 环境变量）
- `application.yml` 中的默认凭据仅用于开发环境

### 支付集成
- **支付宝**: `alipay-sdk-java 4.38.10`，沙箱网关，环境变量: `ALIPAY_APP_ID`, `ALIPAY_PRIVATE_KEY`, `ALIPAY_PUBLIC_KEY`, `ALIPAY_NOTIFY_URL`, `ALIPAY_RETURN_URL`
- **微信支付**: `wechatpay-java` + `wxpay-sdk`（配置未在 `application.yml` 中暴露，可能未完整集成）
- 支付图片存储在 `vue/payImg/`

### 实时消息
- 使用 `Java-WebSocket 1.3.8` 实现 WebSocket 推送

### 定时任务
- 应用使用 `@EnableScheduling`
- `OrderClear` 工具处理定时订单清理

### 数据库约束
- 外键强制引用完整性
- user 表对 `phone` 和 `mail` 有唯一约束
- `cloudland_file` 配置了级联删除，当土地被删除时一并删除

### 前后端通信
- 后端 API 基础 URL 在 `vue/src/request/axiosInstance.js` 中配置
- 请求/响应拦截器在 `vue/src/request/interceptor.js` 中处理令牌管理
- 前端默认期望后端在 `http://localhost:9090`
