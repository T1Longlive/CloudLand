# Cloudland Code Wiki

> Cloudland（云用地）是一个全栈土地/房产与农产品管理平台。后端基于 **Spring Boot 2.7.14 + Java 8**，前端基于 **Vue 2.6 + Element UI**，数据层使用 **MySQL 8 + MyBatis-Plus**，缓存/会话使用 **Redis 7**，并集成支付宝电脑网站支付（沙箱）。

本文档已拆分为结构化的多文件 Wiki，请移步 [docs/wiki](./docs/wiki/README.md) 阅读：

| 文档 | 内容 |
| --- | --- |
| [01-项目概述](./docs/wiki/01-项目概述.md) | 项目简介、技术栈、目录结构、已知陷阱 |
| [02-整体架构](./docs/wiki/02-整体架构.md) | 系统架构图、分层设计、请求处理流程、模块依赖关系 |
| [03-后端模块详解](./docs/wiki/03-后端模块详解.md) | Controller / Service / Mapper / POJO / Util / Config 逐层说明 |
| [04-前端模块详解](./docs/wiki/04-前端模块详解.md) | 路由体系、页面职责、请求层、认证工具、共享组件 |
| [05-数据库设计](./docs/wiki/05-数据库设计.md) | 全部数据表结构、外键关系、业务字段语义 |
| [06-认证与安全](./docs/wiki/06-认证与安全.md) | JWT 认证流程、拦截器、CORS、业务状态码体系 |
| [07-核心业务流程](./docs/wiki/07-核心业务流程.md) | 登录/注册、订单生命周期、支付宝支付、文件存储、定时任务 |
| [08-部署与运行](./docs/wiki/08-部署与运行.md) | 本地开发、环境变量、Docker 部署、CI/CD、E2E 测试 |

阅读建议与快速了解见 [docs/wiki/README.md](./docs/wiki/README.md)。
