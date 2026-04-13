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
- **UI 框架**: Element UI
- **结构**:
  - `src/views/backend/` - 管理员/员工视图
  - `src/views/frontend/` - 客户端视图
  - `src/components/` - 共享组件（Top, Top2）
  - `src/router/` - Vue Router 配置
  - `src/request/` - Axios 实例和拦截器

### 数据库
- **MySQL**，数据库结构在 `cloudland.sql`
- **主要表**: user, land, land_type, product, order2, trolley, msg, msg_send, cloudland_file
- **ORM**: MyBatis-Plus，已启用 SQL 日志

## 开发命令

### 后端
```bash
# 构建项目
mvn clean package

# 运行应用
mvn spring-boot:run

# 运行测试
mvn test

# 主类: com.cloudland.CloudlandApplication
```

### 前端
```bash
cd vue

# 安装依赖
npm install

# 启动开发服务器 (http://localhost:8080)
npm run serve

# 生产环境构建
npm run build
```

## 配置

### 环境变量
应用支持基于环境的配置。主要变量：

- **数据库**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- **Redis**: `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, `REDIS_TIMEOUT`
- **邮件**: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`
- **JWT**: `JWT_SIGN_KEY`, `JWT_EXPIRE`, `JWT_WEEK`
- **CORS**: `CORS_ALLOWED_ORIGINS`（逗号分隔）
- **文件存储**: `FILE_STORAGE_ROOT`（默认: `D:/CloudLandFile`）

### 本地配置覆盖
在项目根目录创建 `config/application-local.yml` 来覆盖配置，无需修改 `application.yml`。此文件已被 gitignore，通过 `spring.config.import` 加载。

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

## 重要注意事项

### 安全
- 密码通过 `BCryptPasswordEncoder` 使用 BCrypt 加密
- JWT 签名密钥在生产环境应更改（设置 `JWT_SIGN_KEY` 环境变量）
- `application.yml` 中的默认凭据仅用于开发环境

### 支付集成
- 已集成微信支付 SDK（`wechatpay-java`, `wxpay-sdk`）
- 支付图片存储在 `vue/payImg/`

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
