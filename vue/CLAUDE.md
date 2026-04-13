# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Cloudland 前端，基于 Vue 2.6 + Element UI，对应后端服务运行在 `http://localhost:9090`。

## 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器 (http://localhost:8080)
npm run serve

# 生产构建
npm run build
```

## 配置

通过环境变量覆盖默认值（在 `.env.local` 中设置）：

- `VUE_APP_API_BASE_URL` — 后端地址（默认 `http://localhost:9090`）
- `VUE_APP_FRONTEND_BASE_URL` — 前端地址（默认 `window.location.origin`）

配置统一在 `src/config/app.js` 中读取，通过 `APP_CONFIG` 导出，包含 `apiBaseUrl`、`resourceUrls` 等。

## 架构

### 路由结构

两个顶层布局：

- `/` → `MainView`（前台商城）：首页、土地列表/详情、产品列表/详情、用户中心、忘记密码
- `/backend` → `MainView`（后台管理）：用户管理（customerA/customerB/employee）、土地、产品、订单、消息推送

路由守卫在 `src/router/index.js` 中，访问后台部分页面和 `/user` 需要登录验证（通过调用 `/user/login` 接口校验 token）。

### 认证状态管理

认证逻辑集中在 `src/utils/auth.js`：

- token 优先读 `sessionStorage`，其次 `localStorage`（"记住我"时持久化到 localStorage）
- `fetchCurrentUser()` — 用已存 token 静默登录，刷新 token
- `loginWithPassword()` — 账号密码登录
- `clearAuthState()` — 清除所有认证状态

### HTTP 请求

- `src/request/axiosInstance.js` — 创建 axios 实例，baseURL 来自 `APP_CONFIG.apiBaseUrl`，timeout 600s
- `src/request/interceptor.js` — 请求拦截自动附加 token 请求头；响应拦截处理 token 刷新（`updatedtoken` 响应头）及 HTTP 401/402/403 错误

### 业务状态码

`src/constants/code.js` 定义所有业务状态码常量（与后端 `Code.java` 保持一致），提供 `isSuccess()`、`isFailure()`、`getCodeMessage()` 工具函数。

主要状态码：`LOGIN_OK: 20005`、`TOKEN_ERR: 20003`、`ADD_OK: 10001` 等。

## 重要约定

- 资源文件 URL 统一通过 `APP_CONFIG.resourceUrls` 拼接，不要硬编码后端地址
- HTTP 状态码错误在拦截器处理，业务状态码错误在各业务组件中处理，两者不要混淆
- token 请求头名称为 `token`（不是 `Authorization`）
