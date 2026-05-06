


# Cloudland (云地)

云地 (Cloudland) 是一个基于 Spring Boot + Vue 2 的云端土地租赁与管理平台，为用户提供土地浏览、租赁、ordering等完整功能。

## 项目简介

Cloudland 是一个全栈互联网应用，旨在为用户提供便捷的云端土地资源租赁服务。系统支持土地信息展示、产品浏览、在线下单、购物车、订单管理、留言反馈等功能。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.14
- **语言**: Java 8
- **数据库**: MySQL
- **缓存**: Redis
- **ORM**: MyBatis-Plus

### 前端
- **框架**: Vue 2.6
- **UI**: Bootstrap 5
- **路由**: Vue Router
- **状态管理**: Vuex

## 功能特性

### 用户功能
- 用户注册与登录
- 密码找回（邮箱验证码）
- 个人信息管理
- 购物车管理
- 订单查询与管理

### 土地管理
- 土地信息发布与展示
- 土地类型分类
- 土地图片/文件上传
- 分页查询

### 产品中心
- 产品浏览与搜索
- 产品详情查看
- 在线购买

### 订单系统
- 在线下单
- 订单状态管理
- 支付宝支付集成
- 报表下载

### 消息系统
- 留言反馈
- 邮件订阅推送

## 项目结构

```
cloudland/
├── src/main/java/com/cloudland/
│   ├── controller/      # 控制器层
│   ├── service/        # 业务逻辑层
│   ├── mapper/         # 数据访问层
│   ├── pojo/           # 实体类
│   ├── util/           # 工具类
│   └── config/         # 配置类
├── src/main/resources/
│   ├── mapper/          # MyBatis XML 映射
│   └── application.yml # 应用配置
└── vue/                # 前端项目
    ├── src/
    │   ├── views/     # 页面组件
    │   ├── components/# 公共组件
    │   ├── router/    # 路由配置
    │   └── request/   # HTTP 请求
    └── public/         # 静态资源
```

## 快速开始

### 环境要求

- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis
- Node.js 16+

### 后端配置

1. 创建数据库并导入初始化脚本：
```bash
mysql -u root