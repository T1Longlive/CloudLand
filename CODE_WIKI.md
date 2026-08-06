# Cloudland Code Wiki

> Cloudland（云用地）是一个全栈土地/房产与农产品管理平台。后端基于 **Spring Boot 2.7.14 + Java 8**，前端基于 **Vue 2.6 + Element UI**，数据层使用 **MySQL 8 + MyBatis-Plus**，缓存/会话使用 **Redis 7**，并集成支付宝电脑网站支付。
>
> 本文档为项目的结构化代码百科，涵盖整体架构、模块职责、关键类与函数、依赖关系、数据库设计、核心业务流程与运行方式。

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈与依赖](#2-技术栈与依赖)
3. [项目目录结构](#3-项目目录结构)
4. [整体架构](#4-整体架构)
5. [后端架构详解](#5-后端架构详解)
6. [前端架构详解](#6-前端架构详解)
7. [数据库设计](#7-数据库设计)
8. [认证与授权机制](#8-认证与授权机制)
9. [核心业务流程](#9-核心业务流程)
10. [文件存储机制](#10-文件存储机制)
11. [状态码体系](#11-状态码体系)
12. [部署与运行](#12-部署与运行)
13. [测试体系](#13-测试体系)
14. [已知陷阱与编码约定](#14-已知陷阱与编码约定)

---

## 1. 项目概述

### 1.1 项目定位

Cloudland 是一个面向土地出租与农产品直销的综合管理平台，提供两类核心业务：

- **土地/用地业务**：土地资源的发布、分页检索、详情查看、用地资料下载、预约与租用下单。
- **农产品业务**：农产品的发布、销售、购物车、下单与支付。

### 1.2 用户角色

系统通过 `user.power` 字段区分三类角色：

| power 值 | 角色 | 说明 |
|---|---|---|
| `0` | 客户（普通用户） | 前台注册用户，可浏览/下单/管理个人资料 |
| `1` | 员工（加盟用户） | 可发布并管理自己名下的土地与产品 |
| `2` | 管理员（平台员工） | 后台全量管理用户、土地、产品、订单、消息推送 |

### 1.3 双前台结构

- **前台商城**（`/`）：面向客户，包含首页、土地列表/详情、产品列表/详情、用户中心、忘记密码。
- **后台管理**（`/backend`）：面向员工/管理员，包含用户管理、土地管理、产品管理、订单管理、消息推送。

---

## 2. 技术栈与依赖

### 2.1 后端技术栈

| 类别 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 框架 | Spring Boot | 2.7.14 | 主框架，端口 9090，context-path `/api` |
| 语言 | Java | 1.8 | |
| ORM | MyBatis-Plus | 3.5.3.1 | 数据访问，XML 映射在 `resources/com/cloudland/mapper/` |
| 数据库 | MySQL | 8.0 | 驱动 `mysql-connector-j 9.3.0` |
| 连接池 | Druid | 1.2.16 | |
| 缓存 | Spring Data Redis + Lettuce | - | 验证码、支付交易映射 |
| 认证 | JJWT | 0.9.1 | JWT 签发/校验，HS256 |
| 密码 | spring-security-crypto | - | BCryptPasswordEncoder |
| 邮件 | spring-boot-starter-mail | - | QQ 邮箱 SMTP，HTML 邮件 |
| Excel | Apache POI | 5.2.2 | 订单报表导出 |
| JSON | FastJSON + Gson | 2.0.32 / 2.8.5 | |
| 支付 | alipay-sdk-java | 4.38.10.ALL | 支付宝沙箱电脑网站支付 |
| 支付 | wechatpay-java / wxpay-sdk | 0.2.12 / 0.0.3 | 引入但未完整集成 |
| WebSocket | Java-WebSocket | 1.3.8 | 实时消息推送 |
| HTTP | OkHttp | 4.10.0 | |
| 工具 | Lombok | 1.18.26 | |

后端完整依赖见 [pom.xml](file:///d:/IDEA_Project/Cloudland/pom.xml)。

> **注意**：pom.xml 的 groupId 为 `com.couldland`（拼写错误），但实际 Java 包名为 `com.cloudland`。

### 2.2 前端技术栈

| 类别 | 技术 | 版本 | 说明 |
|---|---|---|---|
| 框架 | Vue | 2.6.14 | |
| 路由 | Vue Router | 3.5.1 | history 模式 |
| 状态管理 | Vuex | 3.6.2 | 已安装但**实际未使用**，状态靠 sessionStorage/localStorage + 路由 props |
| UI 框架 | Element UI | 2.4.5 | 实际唯一使用的 UI 库 |
| UI 框架 | @opentiny/vue | 2.10.0 | 已安装但 main.js 未实际引入使用 |
| HTTP | axios | 1.2.0 | |
| 地址数据 | element-china-area-data | 5.0.2 | 省市区三级联动 |
| JWT 解析 | jwt-decode | 3.1.2 | |
| 滑块验证 | vue-monoplasty-slide-verify | 1.3.1 | |
| 轮播 | swiper | 5.4.5 | |
| 测试 | @playwright/test | 1.59.1 | E2E 测试 |
| Redis 客户端 | ioredis | 5.10.1 | 测试 helper 直接写验证码 |

前端完整依赖见 [vue/package.json](file:///d:/IDEA_Project/Cloudland/vue/package.json)。

---

## 3. 项目目录结构

```
Cloudland/
├── src/main/java/com/cloudland/        # 后端源码
│   ├── CloudlandApplication.java       # 启动类
│   ├── controller/                     # REST 控制器
│   │   └── result/                     # Result/Code/Msg 响应封装
│   ├── service/ + service/impl/        # 业务接口与实现
│   ├── mapper/                         # MyBatis-Plus Mapper 接口
│   ├── pojo/ + pojo/vo/                # 实体与视图对象
│   ├── config/                         # Spring 配置类
│   ├── Interceptor/                    # JWT 拦截器（大写 I）
│   └── util/                           # 工具类
├── src/main/resources/
│   ├── application.yml                 # 主配置
│   └── com/cloudland/mapper/*.xml      # MyBatis XML 映射
├── vue/                                # 前端源码
│   ├── src/
│   │   ├── main.js                     # 入口
│   │   ├── App.vue                     # 根组件
│   │   ├── config/app.js               # APP_CONFIG 统一配置
│   │   ├── constants/code.js           # 业务状态码常量
│   │   ├── request/                    # axios 实例与拦截器
│   │   ├── router/index.js             # 路由配置与守卫
│   │   ├── utils/auth.js               # 认证状态管理
│   │   ├── components/                 # Top/Top2 共享导航
│   │   └── views/                      # frontend/ + backend/ 视图
│   ├── tests/                          # Playwright E2E 测试
│   ├── Dockerfile
│   └── package.json
├── cloudland.sql                       # 数据库初始化脚本
├── docker-compose.yml                  # 容器编排
├── nginx.conf                          # 前端 Nginx 配置
├── Dockerfile                          # 后端镜像
├── scripts/deploy.sh                   # 生产部署脚本
├── pom.xml                             # Maven 配置
├── AGENTS.md / CLAUDE.md / API.md      # 项目文档
└── CODE_WIKI.md                        # 本文档
```

---

## 4. 整体架构

### 4.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│  浏览器 (Vue 2.6 SPA, port 8080)                         │
│  ┌──────────┐  ┌──────────┐  ┌────────────────────┐    │
│  │ ElementUI│  │ Router   │  │ axios + 拦截器      │    │
│  └──────────┘  └──────────┘  └─────────┬──────────┘    │
└────────────────────────────────────────┼────────────────┘
                                         │ HTTP (token 头, /api 前缀)
┌────────────────────────────────────────▼────────────────┐
│  Spring Boot (port 9090, context-path /api)              │
│  ┌──────────────────────────────────────────────────┐   │
│  │ CorsFilter + MyInterceptor (JWT 校验)            │   │
│  └────────────────────────┬─────────────────────────┘   │
│  ┌────────────────────────▼─────────────────────────┐   │
│  │ Controller (REST, 统一 Result 响应)              │   │
│  └────────────────────────┬─────────────────────────┘   │
│  ┌────────────────────────▼─────────────────────────┐   │
│  │ Service / ServiceImpl (业务逻辑)                 │   │
│  └───┬──────────┬──────────┬──────────┬─────────────┘   │
│      │          │          │          │                  │
│  ┌───▼───┐ ┌────▼────┐ ┌───▼───┐ ┌────▼─────┐           │
│  │Mapper │ │FileUtil │ │JwtUtils│ │EmailUtils│           │
│  │(MP+XML)│ │(本地磁盘)│ │(JWT)  │ │(JavaMail)│           │
│  └───┬───┘ └─────────┘ └───────┘ └──────────┘           │
│      │              │                                     │
│      │         ┌────▼─────┐                              │
│      │         │ Redis    │ (验证码/交易映射)             │
│      │         └──────────┘                              │
└──────┼──────────────────────────────────────────────────┘
       │
  ┌────▼────┐
  │ MySQL 8 │ (10 张表)
  └─────────┘
```

### 4.2 请求处理链

`HTTP 请求 → CorsFilter → MyInterceptor（JWT 校验，公开路径放行）→ Controller → Service(Impl) → Mapper(BaseMapper + XML) → MySQL`

- 文件操作经 `FileUtil` + `StorageProperties` 落本地磁盘
- 验证码/支付交易映射经 `StringRedisTemplate` 落 Redis
- 邮件经 `EmailUtils` + `JavaMailSender` 发送
- 定时任务 `OrderClear` 由 `@EnableScheduling` 驱动，无 Controller 入口

### 4.3 前后端通信约定

| 约定 | 说明 |
|---|---|
| Token 请求头 | `token`（**不是** `Authorization`），由 [interceptor.js](file:///d:/IDEA_Project/Cloudland/vue/src/request/interceptor.js) 自动附加 |
| 响应头 | `updatedToken`（小写），用于 token 刷新 |
| Context-path | 后端 `/api`，所有接口前缀 `http://localhost:9090/api/...` |
| 自定义头 | `token`、`remember`、`frond`、`code`、`contact`、`forgetPassword`（CORS 白名单内） |
| 响应格式 | 统一 `Result { data, code, msg }` |
| 文件上传 | `multipart/form-data`，业务参数为 **JSON 字符串**（非对象） |

---

## 5. 后端架构详解

### 5.1 启动类与配置

#### CloudlandApplication

[CloudlandApplication.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/CloudlandApplication.java)

```java
@SpringBootApplication
@EnableScheduling
@MapperScan("com.cloudland.mapper")
public class CloudlandApplication { ... }
```

- `@EnableScheduling`：启用定时任务（`OrderClear` 每日 0 点清理挂单）
- `@MapperScan("com.cloudland.mapper")`：扫描 Mapper 接口

#### 配置文件

[application.yml](file:///d:/IDEA_Project/Cloudland/src/main/resources/application.yml) 关键配置：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `server.port` | 9090 | 后端端口 |
| `server.servlet.context-path` | `/api` | 接口前缀 |
| `access-file.location` | `${FILE_STORAGE_ROOT:D:/CloudLandFile}` | 文件存储根目录 |
| `spring.datasource` | MySQL `cloudland` 库 | Druid 连接池 |
| `spring.redis` | localhost:6379 | Lettuce 连接池 |
| `spring.mail` | smtp.qq.com:465 | QQ 邮箱 |
| `spring.servlet.multipart` | 60MB | 单文件/单请求上限 |
| `jwt.signKey` | `change-me-in-production` | JWT 签名密钥 |
| `jwt.expire` / `jwt.week` | 604800000ms | 普通/记住我有效期（7 天） |
| `cors.allowed-origins` | `http://localhost:8080,...` | CORS 白名单 |
| `alipay.*` | 沙箱配置 | 支付宝 |
| `mybatis-plus.configuration.log-impl` | StdOutImpl | 控制台打印 SQL |

**本地覆盖**：创建 `config/application-local.yml`（gitignored），通过 `spring.config.import` 加载，避免修改主配置。

#### Config 包

| 类 | 文件 | 职责 |
|---|---|---|
| `WebMvcConfig` | [WebMvcConfig.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/WebMvcConfig.java) | 注册 `MyInterceptor`、配置静态资源映射、`CorsFilter` Bean、`BCryptPasswordEncoder` Bean |
| `StorageProperties` | [StorageProperties.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/StorageProperties.java) | `@ConfigurationProperties(prefix="access-file")`，统一计算各类文件路径 |
| `RedisConfig` | [RedisConfig.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/RedisConfig.java) | Redis 序列化配置 |
| `AlipayConfig` | [AlipayConfig.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/AlipayConfig.java) | 支付宝客户端 Bean 配置 |
| `MybatisPlusConfig` | [MybatisPlusConfig.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/MybatisPlusConfig.java) | MyBatis-Plus 分页插件等配置 |

**WebMvcConfig 关键逻辑**：

- 资源映射：`/resource/userFile/**` → `UserIcon/`、`/resource/landFile/**` → `LandFile/`、`/resource/productFile/**` → `Product/`
- 拦截器排除路径：`/resource/**`、`/land/page`、`/land/{id}`、`/product/page`、`/product/{id}`、`/msg`、`/msg/mail`、`/alipay/notify`、`/alipay/return`、`/error`
- CORS 允许方法：GET/POST/PUT/DELETE/OPTIONS
- CORS 允许头：`token, Content-Type, remember, frond, code, contact, forgetPassword`
- CORS 暴露头：`updatedToken`

**StorageProperties 路径计算**（被 FileUtil / OrderExporter / DownloadUtil 依赖）：

- `getUserIconDir/Path(fileName)` → `{root}/UserIcon/`
- `getProductDir/Path(fileName)` → `{root}/Product/`
- `getLandDir(landId)` → `{root}/LandFile/Land_{id}/`
- `getLandCloudFileDir(landId)` → `{root}/LandFile/Land_{id}/CloudLandFile/`
- `getLandImagesDir(landId)` → `{root}/LandFile/Land_{id}/Images/`
- `getLandZipPath(landId, zipFileName)` → `{root}/LandFile/Land_{id}/{zip}`
- `getExportPath(type)` → `{root}/云用地_用地订单.xlsx`（type=0）或 `云用地_产品订单.xlsx`（type=1）
- 内部 `normalize` 将反斜杠转正斜杠并去尾部斜杠

---

### 5.2 Controller 层

Controller 包：`src/main/java/com/cloudland/controller/`

所有接口返回统一 `Result` 对象（[Result.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Result.java)），状态码定义在 [Code.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Code.java)，文案在 [Msg.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Msg.java)。

#### UserController

文件：[UserController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/UserController.java)
注解：`@RestController @RequestMapping("/user") @Slf4j`
注入：`UserServiceImpl`、`ITrolleyService`

| HTTP | 路径 | 方法 | 功能 | 认证 |
|---|---|---|---|---|
| POST | `/user/login` | `login` | 登录（密码登录 / token 验证刷新双模式） | 否 |
| POST | `/user/register` | `register` | 注册（multipart: `userIcon` + `user` JSON 字符串） | 否 |
| DELETE | `/user` | `delete` | 批量删除（`@RequestBody Integer[] ids`） | 是 |
| PUT | `/user` | `update` | 更新（multipart: `userIcon` + `user` JSON，依 `frond`/`contact` 头分支） | 是 |
| POST | `/user/page` | `selectPage` | 分页查询 | 是 |
| POST | `/user/employee` | `selectAllId` | 查询 power=2 员工 id+username | 是 |
| POST | `/user/id` | `selectById` | 按 id 查询（返回前清空 password） | 是 |
| POST | `/user/code` | `sendCode` | 发送邮箱验证码 | 否 |
| POST | `/user/forgetPassword` | `forgetPassword` | 忘记密码（multipart: `path` + `user` JSON） | 否 |
| POST | `/user/trolley` | `myTrolley` | 查询我的购物车 | 是 |
| POST | `/user/addTrolley` | `addTrolley` | 加入购物车 | 是 |
| DELETE | `/user/trolley/{id}` | `deleteTrolley` | 删除购物车项 | 是 |

**特殊设计**：`/user/login` 同时承担三种功能，通过请求头组合区分：
1. 账号密码登录（带 `code`、`frond:true`、user 含密码）
2. token 验证 + 静默登录（带 `token`、`frond:true`、空 user）
3. 后台登录（带 `code`、`frond:false`）

#### LandController

文件：[LandController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/LandController.java)
注解：`@RestController @RequestMapping("/land")`

| HTTP | 路径 | 方法 | 功能 | 认证 |
|---|---|---|---|---|
| GET | `/land/{id}` | `selectById` | 按 id 查询土地 | 否 |
| POST | `/land` | `addLand` | 新增（multipart: `landFiles[]` + `imageFiles[]` + `land` JSON） | 是 |
| DELETE | `/land` | `delete` | 批量删除 | 是 |
| PUT | `/land` | `update` | 更新（multipart，文件可选） | 是 |
| POST | `/land/page` | `selectPage` | 分页查询（`pageNum`/`pageSize` + `land` JSON） | 否 |

#### ProductController

文件：[ProductController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/ProductController.java)
注解：`@RestController @RequestMapping("/product")`

| HTTP | 路径 | 方法 | 功能 | 认证 |
|---|---|---|---|---|
| GET | `/product/{id}` | `selectById` | 按 id 查询 | 否 |
| POST | `/product` | `addProduct` | 新增（multipart: `productImg` + `product` JSON） | 是 |
| DELETE | `/product` | `delete` | 批量删除 | 是 |
| PUT | `/product` | `update` | 更新（multipart，`productImg` 可选） | 是 |
| POST | `/product/page` | `selectPage` | 分页查询 | 否 |

#### OrderController

文件：[OrderController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/OrderController.java)
注解：`@RestController @RequestMapping("/order")`

| HTTP | 路径 | 方法 | 功能 |
|---|---|---|---|
| POST | `/order/order` | `myTrolley` | 查询指定用户订单列表 |
| POST | `/order` | `addOrder` | 下单（`@RequestBody Order2`） |
| PUT | `/order` | `updateOrder` | 更新订单状态（multipart: `ids[]` + `status` + `time`） |
| DELETE | `/order/{id}` | `deleteOrder` | 删除/退单（已支付软删 del=1，否则恢复库存/土地状态后硬删） |
| POST | `/order/page` | `selectPage` | 分页查询（`num=-1` 走土地订单分支） |
| POST | `/order/download` | `getRank` | 导出 Excel（直接写 HttpServletResponse） |

#### MsgController

文件：[MsgController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/MsgController.java)
注解：`@RestController @RequestMapping("/msg")`

| HTTP | 路径 | 方法 | 功能 | 认证 |
|---|---|---|---|---|
| POST | `/msg` | `msg` | 提交留言（同 phone 超 9 条拒绝） | 否 |
| POST | `/msg/mail` | `subscribe` | 邮件订阅（同 mail 已存在拒绝） | 否 |
| POST | `/msg/page` | `selectPage` | 留言分页 | 是 |
| POST | `/msg/push` | `push` | 群发推送（遍历 MsgSend 调用 `sendOutEmail`） | 是 |

**注意**：MsgController 内直接构建 `QueryWrapper` 做业务判断，未下沉到 Service。

#### AlipayController

文件：[AlipayController.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/AlipayController.java)
注解：`@RestController @RequestMapping("/alipay") @Slf4j`

| HTTP | 路径 | 方法 | 功能 |
|---|---|---|---|
| POST | `/alipay/pay` | `pay` | 发起支付（生成 `CLD+时间戳+userId` 商户单号，返回支付表单 HTML） |
| GET | `/alipay/return` | `alipayReturn` | 同步返回（查询交易状态，`TRADE_SUCCESS` 则处理成功） |
| POST | `/alipay/notify` | `notify` | 异步通知（验签后处理，返回 `"success"`/`"failure"` 字符串） |

**特殊点**：`/alipay/notify` 直接返回 String 而非 `Result`，符合支付宝协议要求。

#### result 包（响应封装）

| 类 | 文件 | 职责 |
|---|---|---|
| `Result` | [Result.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Result.java) | 统一响应体 `{data, code, msg}`，提供 `success()/failure()` 静态工厂 |
| `Code` | [Code.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Code.java) | 业务状态码常量 + `isSuccess()/isFailure()` 工具方法 |
| `Msg` | [Msg.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Msg.java) | 中文消息文案常量 |

---

### 5.3 Service 层

Service 包：`src/main/java/com/cloudland/service/`（接口）与 `service/impl/`（实现）。所有接口继承 MyBatis-Plus `IService<T>`，实现类继承 `ServiceImpl<Mapper, T>`。

#### IUserService / UserServiceImpl

文件：[IUserService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/IUserService.java)、[UserServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/UserServiceImpl.java)

实现类额外注入：`UserMapper`、`FileUtil`、`BCryptPasswordEncoder`、`JwtUtils`、`HttpServletRequest`、`EmailUtils`、`LandMapper`、`StringRedisTemplate`、`StorageProperties`

| 方法 | 功能 |
|---|---|
| `loginUser(User, HttpServletRequest)` | 双模式登录。有 token 无密码 → 解析 JWT、校验 phone/status/power，签发新 token 放入 Result.msg；否则密码登录（校验 `code` 头验证码、BCrypt 比对、status 校验） |
| `save(MultipartFile, User)` | 注册。校验验证码/phone/mail 唯一，头像落盘（无则 `basic.png`），BCrypt 加密，insert |
| `delete(Integer[])` | 删除用户头像目录，LambdaUpdate 将 land.employee_id 置空，deleteBatchIds |
| `update(MultipartFile, User, HttpServletRequest)` | 更新。校验唯一性，按 `frond`/`contact` 头区分改密/改联系方式（含旧密码校验、相同检测） |
| `selectPage(int, int, User)` | 委托 `userMapper.selectByCondition` |
| `sendOutEmail(User)` | 生成 6 位验证码写 Redis（5 分钟 TTL），HTML 邮件发送 |
| `forgetPassword(User, String path)` | 两阶段：带 id + `forgetPassword` 头 → 重置密码；否则查用户、生成验证码、发送重置链接邮件 |

私有 `buildTokenClaims(User)`：构造 JWT claims（id/phone/username/power/debt）。

#### ILandService / LandServiceImpl

文件：[ILandService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/ILandService.java)、[LandServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/LandServiceImpl.java)

| 方法 | 功能 |
|---|---|
| `save(MultipartFile[], MultipartFile[], Land)` | id 空则 insert 否则 updateById；调 `fileUtil.defineDirectory` 落盘，按 type=1(资料zip)/0(图片) 写 `cloudland_file` 表 |
| `delete(Integer[])` | deleteBatchIds + 删除土地目录 |
| `update(MultipartFile[], MultipartFile[], Land)` | 无文件直接 updateById；有文件则先删旧文件与 `cloudland_file` 记录再复用 `save` |
| `selectPage(int, int, Land)` | 调 `landMapper.selectByCondition` 得 `Page<LandVO>`，逐条二次查 `cloudland_file` 补图片/资料 |
| `selectById(Integer)` | 复用 selectPage(1,1,land) 取首条 |

#### IProductService / ProductServiceImpl

文件：[IProductService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/IProductService.java)、[ProductServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/ProductServiceImpl.java)

| 方法 | 功能 |
|---|---|
| `save(MultipartFile, Product)` | `fileUtil.defineDirectory` 落盘 + insert |
| `delete(Integer[])` | 遍历删产品图片 + deleteBatchIds |
| `update(MultipartFile, Product)` | 有新图则删旧图落新图 + updateById |
| `selectPage` / `selectById` | 委托 `productMapper.selectByCondition` |

#### IOrder2Service / Order2ServiceImpl

文件：[IOrder2Service.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/IOrder2Service.java)、[Order2ServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/Order2ServiceImpl.java)

常量：`TRADE_KEY_PREFIX = "alipay:trade:"`

| 方法 | 功能 |
|---|---|
| `selectPage(int, int, Order2)` | `num=-1` 走土地订单分支（补 land 图片），否则产品订单分支 |
| `selectOrder(User)` | 按 u_id + del=0 查订单，逐条拼装 OrderVO |
| `addOrder(Order2)` `@Transactional` | num=-1 校验土地可用后置为已租；否则校验库存/状态后扣减；写 createTime/status=0/del=0 后 insert |
| `updateOrder(Integer[], Integer, Boolean)` `@Transactional` | 批量改 status，time=true 写 payTime |
| `deleteOrder(Integer)` `@Transactional` | 已支付(status=1)→软删 del=1；否则恢复土地 status=1 或产品库存后硬删 |
| `getOrder(HttpServletResponse, Order2)` | 按 num 分支查订单 + 补图片 + `orderExporter.exportToExcel` + `downloadUtil.getFile` 输出 |
| `saveTradeMapping(String, Integer[])` | Jackson 序列化 orderIds 存 Redis（30 分钟 TTL） |
| `handlePaySuccess(String)` `@Transactional` | 从 Redis 取 orderIds，调 `updateOrder(ids, 1, true)` 标记已支付，删 Redis 映射 |

#### IMsgService / MsgServiceImpl

文件：[IMsgService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/IMsgService.java)、[MsgServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/MsgServiceImpl.java)

- `selectPage(int, int, Msg)`：委托 `msgMapper.selectByCondition`

#### IMsgSendService / MsgSendServiceImpl

文件：[IMsgSendService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/IMsgSendService.java)、[MsgSendServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/MsgSendServiceImpl.java)

- `sendOutEmail(String mail, String pushMsg)`：构造订阅推送 HTML 邮件，调 `emailUtils.sendEmail`

#### ITrolleyService / TrolleyServiceImpl

文件：[ITrolleyService.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/ITrolleyService.java)、[TrolleyServiceImpl.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/service/impl/TrolleyServiceImpl.java)

| 方法 | 功能 |
|---|---|
| `selectTrolley(User)` | 按 u_id 查 Trolley，逐条按 num=-1 区分查 LandVO（补图片）或 Product，拼装 TrolleyVo |
| `deleteTrolley(Integer)` | `baseMapper.deleteById` |
| `addTrolley(Trolley)` | `baseMapper.insert` |

---

### 5.4 Mapper 层

Mapper 包：`src/main/java/com/cloudland/mapper/`，XML：`src/main/resources/com/cloudland/mapper/`

所有 Mapper 继承 MyBatis-Plus `BaseMapper<T>`，自带通用 CRUD。下表列出各自的自定义方法：

| Mapper | 继承 | 自定义方法 | XML | 关键 SQL |
|---|---|---|---|---|
| `UserMapper` | `BaseMapper<User>` | `selectByCondition(Page, User)` | [UserMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/UserMapper.xml) | 单表动态 where（status/id/power/username LIKE），ORDER BY id |
| `LandMapper` | `BaseMapper<Land>` | `selectAll()`、`selectById(Integer)`、`selectByCondition(Page, Land)` | [LandMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/LandMapper.xml) | **4 表 JOIN**：land × land_type × user(a_id) × user(employee_id)；动态过滤；`<choose>` 按 id=-1/-2 切换 ORDER BY price/area/id |
| `ProductMapper` | `BaseMapper<Product>` | `selectByCondition(Page, Product)` | [ProductMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/ProductMapper.xml) | 2 表 JOIN：product × user(a_id)；`<choose>` 按 id=-1/-2 切换排序 |
| `Order2Mapper` | `BaseMapper<Order2>` | `selectByCondition/selectByCondition2(Page, Order2)`、`selectOrder/selectOrder2(Order2)` | [Order2Mapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/Order2Mapper.xml) | 土地订单（`num=-1` JOIN land+user）、产品订单（`num!=-1` JOIN product+user）双查询体系 |
| `MsgMapper` | `BaseMapper<Msg>` | `selectByCondition(Page, Msg)` | [MsgMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/MsgMapper.xml) | 单表动态按 id/name LIKE，ORDER BY id DESC |
| `MsgSendMapper` | `BaseMapper<MsgSend>` | 无 | [MsgSendMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/MsgSendMapper.xml) | 空 mapper，纯通用方法 |
| `TrolleyMapper` | `BaseMapper<Trolley>` | 无 | [TrolleyMapper.xml](file:///d:/IDEA_Project/Cloudland/src/main/resources/com/cloudland/mapper/TrolleyMapper.xml) | 空 mapper，纯通用方法 |
| `CloudLandFileMapper` | `BaseMapper<CloudLandFile>` | 无 | 无 XML | 配合 `QueryWrapper` 按 land_id+type 查询 |

> **设计要点**：`Order2Mapper` 通过 `order.num` 字段区分土地订单（`num=-1`）与产品订单（`num>=0` 为数量），形成两套平行的 JOIN 查询。`selectByCondition` 与 `selectOrder` SQL 相同，仅返回类型 Page vs List 不同（分页查询与导出复用）。

---

### 5.5 POJO 层

POJO 包：`src/main/java/com/cloudland/pojo/`（实体）与 `pojo/vo/`（VO）。所有实体 `@Data @EqualsAndHashCode(callSuper=false) @Accessors(chain=true) implements Serializable`，主键 `@TableId(type=IdType.AUTO)`。

#### 实体类

| 实体 | 文件 | 表 | 关键字段 |
|---|---|---|---|
| `User` | [User.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/User.java) | user | username, password(BCrypt), age, phone(唯一), mail(唯一), address, detailedAddress, img, power(0/1/2), status(0禁用/1激活), debt(欠款) |
| `Land` | [Land.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Land.java) | land | landName, landType, description, address, ordered(排序权重), price(元/㎡/天), area, aId(所有者), employeeId(代理人), status(0可用/1已租), detailedAddress |
| `LandType` | [LandType.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/LandType.java) | land_type | id, typeName, description（6 类土地字典） |
| `Product` | [Product.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Product.java) | product | productName, description, ordered, price, num(Double 库存), status, aId, img |
| `Order2` | [Order2.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Order2.java) | order2 | pId, num(**-1 表示土地订单**，否则产品数量), uId, createTime, payTime, status(0未支付/1已支付), del(软删标志) |
| `Trolley` | [Trolley.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Trolley.java) | trolley | pId, num(-1 用地 / >=1 商品数量), uId |
| `Msg` | [Msg.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Msg.java) | msg | name, mail, phone, msg, sendTime（客户留言） |
| `MsgSend` | [MsgSend.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/MsgSend.java) | msg_send | mail（邮件订阅者） |
| `CloudLandFile` | [CloudLandFile.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/CloudLandFile.java) | cloudland_file | path, landId, type(**0 图片/1 用地资料**)，与 land 级联删除 |
| `Address` | [Address.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/Address.java) | address | addressAreaid, addressName, addressRegionid（预留地址字典，业务未直接使用） |

> **Order2 命名**：实体名为 `Order2`（非 `Order`）以避免 SQL 关键字 `ORDER` 冲突。

#### VO 类

| VO | 文件 | 扩展来源 | 新增字段 | 用途 |
|---|---|---|---|---|
| `LandVO` | [LandVO.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/vo/LandVO.java) | Land | landFiles(CloudLandFile 资料), imageFiles(List 图片), typeName/typeDescription, customerAUsername/customerAPhone, employeeUsername/employeePhone | LandMapper JOIN 结果映射 |
| `OrderVO` | [OrderVO.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/vo/OrderVO.java) | Order2(`@TableName("order2")`) | productName, img, price, username, phone | Order2Mapper JOIN 结果映射 |
| `ProductVO` | [ProductVO.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/vo/ProductVO.java) | Product | customerAUsername, aId, customerAPhone | ProductMapper JOIN 结果映射 |
| `TrolleyVo` | [TrolleyVo.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/pojo/vo/TrolleyVo.java) | Trolley(`@TableName("trolley")`) | productName, img, price, productNum(库存), status | TrolleyServiceImpl 手工拼装（非 SQL 映射） |

---

### 5.6 Util 层

Util 包：`src/main/java/com/cloudland/util/`

#### JwtUtils

文件：[JwtUtils.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/JwtUtils.java)，`@Component`

| 方法 | 功能 |
|---|---|
| `generateJwt(Map<String,Object> claims, Boolean remember)` | HS256 签名，remember=true 用 `week`（长有效期），否则 `expire` |
| `parseJWT(String jwt)` | 解析返回 `Claims` |

使用者：`UserServiceImpl`（签发）、`MyInterceptor`（校验）。

#### EmailUtils

文件：[EmailUtils.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/EmailUtils.java)，`@Component`

- `sendEmail(String to, String subject, String context)`：用 `MimeMessageHelper`（HTML 支持）发送，返回 `Result`（SEND_MAIL_OK/ERR）

使用者：`UserServiceImpl`（验证码、找回密码）、`MsgSendServiceImpl`（订阅推送）。

#### FileUtil

文件：[FileUtil.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/FileUtil.java)，`@Component @Slf4j`。文件落盘核心枢纽，依赖 `StorageProperties`。

核心方法 `defineDirectory(Integer parentId, MultipartFile[] landFiles, MultipartFile[] imageFiles, MultipartFile userIcon, MultipartFile productImg)`，按非空参数分支：

- **userIcon 分支**：UUID 重命名 `userIcon_xxx.后缀` 存 UserIcon 目录，返回 `[文件名]`
- **productImg 分支**：UUID 重命名 `product_xxx.后缀` 存 Product 目录，返回 `[文件名]`
- **land 分支**：资料/图片分别重命名；资料存 CloudLandFile 目录后 `zipFolder` 压缩为 `云用地_{id}{code}.zip` 并删原目录；图片存 Images 目录；返回 `[压缩包名, 图片名List]`（无文件时对应位置存 0/1 占位）

其他方法：`deleteFolder(File)` 递归删除、`createDirectory(path,name)`、`saveFiles(...)`、`rename/rename2(...)`、`zipFolder(source,zip)` 静态压缩 + 私有 `addFolderToZip` 递归。

#### OrderExporter

文件：[OrderExporter.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/OrderExporter.java)，`@Component`

- `exportToExcel(List<OrderVO> orderVOS, Integer type)`：用 Apache POI `XSSFWorkbook` 生成 .xlsx，表头 10 列（订单ID/商品名称/数量/价格/总价/创建时间/支付时间/状态/下单人/电话），status=1 显示"已支付"否则"已退单"，末行追加统计截止时间，写入 `storageProperties.getExportPath(type)`。

#### OrderClear

文件：[OrderClear.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/OrderClear.java)，`@Component`

- `executeTask()`：`@Scheduled(cron = "0 0 0 * * ?")` 每日 0 点执行，`@Transactional`。遍历全部订单，对 status=0（未支付）订单：num=-1 恢复 land.status=1；产品订单恢复 product.num 库存；然后 deleteById 硬删。**唯一无 Controller 入口、由调度器驱动的业务组件**。

#### DownloadUtil

文件：[DownloadUtil.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/DownloadUtil.java)，`@Component`

- `getFile(String name, HttpServletResponse response)`：从 `storageProperties.getRootFilePath(name)` 读文件，按扩展名（pdf/xls/xlsx/doc/docx）推断 mimeType，设置 `Content-Disposition: attachment;fileName=<URLEncode>`，1024 字节缓冲流式输出。

---

### 5.7 Interceptor 层

#### MyInterceptor

文件：[MyInterceptor.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/Interceptor/MyInterceptor.java)，`@Component`，实现 `HandlerInterceptor`

`preHandle` 处理流程：

1. URL 含 `login`/`register`/`code`/`forgetPassword` 关键字 → 直接放行
2. 取 `token` 头，空 → HTTP **401**
3. `jwtUtils.parseJWT` 解析，取 claims.id；id 为空 → 401
4. `userMapper.selectById(userId)`；用户不存在 → 401
5. `status == 0`（禁用） → HTTP **403**
6. token 中 power 与当前 power 不一致 → HTTP **402**（权限已变更）
7. 通过则放行

> **目录命名陷阱**：`Interceptor/` 目录首字母大写 I，与 Java 包命名惯例不一致。
>
> **HTTP 状态码非标准语义**：401=未认证、402=权限变更、403=账号禁用，前端 `interceptor.js` 据此处理。

---

## 6. 前端架构详解

### 6.1 入口与配置

#### main.js

文件：[main.js](file:///d:/IDEA_Project/Cloudland/vue/src/main.js)

- `Vue.use(ElementUI)` 安装 Element UI（实际唯一使用的 UI 库）
- 全局注册 `CollapseTransition` 组件
- 引入 `style.css` 与 `bootstrap.min.css`
- `Vue.prototype.$appConfig = APP_CONFIG` 挂载配置到原型
- 调用 `setupInterceptors()` 安装 axios 拦截器

> **注意**：`@opentiny/vue` 虽在 package.json 中，但 main.js 未实际引入，与 CLAUDE.md 描述不符。

#### App.vue

文件：[App.vue](file:///d:/IDEA_Project/Cloudland/vue/src/App.vue)

极简根组件，仅 `<div id="app"><router-view/></div>`，所有布局由路由组件负责。

#### config/app.js

文件：[app.js](file:///d:/IDEA_Project/Cloudland/vue/src/config/app.js)

导出 `APP_CONFIG`，并通过 `window.__APP_CONFIG__` 暴露便于调试：

```js
{
  apiBaseUrl,        // 后端 API 根地址（默认 http://localhost:9090，去尾部斜杠）
  frontendBaseUrl,   // 前端根地址
  resourceUrls: {
    userFile:    `${apiBaseUrl}/resource/userFile/`,
    landFile:    `${apiBaseUrl}/resource/landFile/`,
    productFile: `${apiBaseUrl}/resource/productFile/`,
  },
  forgetPasswordPath: `${frontendBaseUrl}/ForgetPassword/`,
}
```

**关键约定**：资源 URL 统一通过 `APP_CONFIG.resourceUrls` 拼接，**不硬编码后端地址**。

#### constants/code.js

文件：[code.js](file:///d:/IDEA_Project/Cloudland/vue/src/constants/code.js)

定义业务状态码常量（与后端 [Code.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Code.java) 一致）+ 工具函数 `isSuccess(code)`、`isFailure(code)`、`getCodeMessage(code)`。

> **实际使用**：业务组件中多数直接用数字字面量比较（`res.code === 20005`），未普遍引用常量。

---

### 6.2 路由结构

文件：[router/index.js](file:///d:/IDEA_Project/Cloudland/vue/src/router/index.js)，`mode: "history"`

#### 前台路由 `/`（`views/frontend/MainView`，`props: true`）

| 路径 | 组件 | 说明 |
|---|---|---|
| `/` | frontend/HomeView | 商城首页（meta.title: '云用地'） |
| `/land` | frontend/LandView | 土地列表 |
| `/landInfo/:id` | frontend/LandInfoView | 土地详情 |
| `/product` | frontend/ProductView | 产品列表 |
| `/productInfo/:id` | frontend/ProductInfoView | 产品详情 |
| `/ForgetPassword/:id` | frontend/ForgetPassword | 忘记密码（id 格式 `{6位code}-{userId}`） |
| `/user` | frontend/UserView | 用户中心容器 |
| `/user`（空） | userInfo/UserMsgView | 个人资料 |
| `/user/password` | userInfo/UserPassword | 修改密码 |
| `/user/contact` | userInfo/UserContactView | 账号绑定 |
| `/user/myOrder` | userInfo/MyOrder | 我的订单 |
| `/user/myTrolley` | userInfo/MyTrolley | 我的购物车 |

#### 后台路由 `/backend`（`views/backend/MainView`，meta.title: '云用地后台'）

| 路径 | 组件 | 说明 |
|---|---|---|
| `/backend`（空） | backend/HomeView | 后台首页 |
| `/backend/customerA` | backend/Customer_aView | 普通用户管理（power=0） |
| `/backend/customerB` | backend/Customer_bView | 加盟用户管理（power=1） |
| `/backend/employee` | backend/EmployeeView | 平台员工管理（power=2） |
| `/backend/product` | backend/ProductView | 产品管理 |
| `/backend/land` | backend/LandView | 土地管理 |
| `/backend/orderLand` | backend/LandOrderView | 土地订单管理 |
| `/backend/orderProduct` | backend/ProductOrderView | 产品订单管理 |
| `/backend/pushMsg` | backend/PushMsg | 留言查看与消息推送 |

所有非根路由采用动态 import 实现路由级代码分割。

#### 路由守卫 beforeEach

`isUserLoggedIn()`：用已存 token 调 `POST /user/login` 校验，`res.code === 20005` 视为已登录。

- `/backend/land`、`/backend/customerA`、`/backend/customerB`、`/backend/employee` 未登录 → alert + 跳 `/`
- `/user` 前缀路由未登录 → alert + 跳 `/`
- 其余放行，维护 sessionStorage 的 `replace`/`main` 标记

`afterEach`：按 `to.meta.title` 设置 `document.title`。

> **注意**：守卫每次跳转都触发一次 `/user/login` 校验请求，存在性能开销。后台其他路由（product/orderLand 等）由 `backend/MainView.vue` 的 `openCheck()` 二次保护。

---

### 6.3 请求层

#### axiosInstance.js

文件：[axiosInstance.js](file:///d:/IDEA_Project/Cloudland/vue/src/request/axiosInstance.js)

```js
const axiosInstance = axios.create({
  baseURL: APP_CONFIG.apiBaseUrl,  // 默认 http://localhost:9090
  timeout: 600000,                  // 600s = 10 分钟
});
```

#### interceptor.js

文件：[interceptor.js](file:///d:/IDEA_Project/Cloudland/vue/src/request/interceptor.js)，提供 `setupInterceptors()`（`installed` 闭包防重复安装）

**请求拦截器**：调 `getStoredToken()`，存在则附加到 `config.headers.token`。

**响应拦截器**：
- 成功：检查 `response.headers["updatedtoken"]`（小写），存在则 `persistToken(updatedToken, hasPersistentToken())` 刷新本地 token，保持原存储位置
- 失败（按 HTTP 状态码，**非业务码**）：
  - `401` → `clearAuthState()` + `router.push('/')`
  - `402` → alert 权限变更 + 清状态跳首页
  - `403` → alert 账号禁用 + 清状态跳首页
  - `500` → 仅 `console.error`
  - 其他 → `Promise.reject(error)`

> **双层状态码体系**：HTTP 状态码在拦截器处理，业务状态码在各业务组件处理，两者不混淆。

---

### 6.4 认证状态管理

文件：[auth.js](file:///d:/IDEA_Project/Cloudland/vue/src/utils/auth.js)

| 函数 | 功能 |
|---|---|
| `createEmptyUser()` | 返回空 user 对象模板 |
| `hasPersistentToken()` | 检查 `localStorage.token` 是否存在且非 `null` 字符串 |
| `getStoredToken()` | 优先 `sessionStorage.token`，回退 `localStorage.token`，过滤 `'null'` 字符串 |
| `persistToken(token, remember)` | remember=true 双写 localStorage+sessionStorage；false 移除 localStorage 仅写 sessionStorage |
| `clearAuthState()` | 清除 localStorage.token、sessionStorage.token/uid/userID |
| `fetchCurrentUser(frond=true)` | 静默登录：用已存 token 调 `POST /user/login`，成功 `persistToken`，20003 清状态 |
| `loginWithPassword(user, opts)` | 账号密码登录：构造 headers（token:null, remember, frond, 可选 code），成功 `persistToken` |

**Token 存储策略**：
- 双存储：sessionStorage（关闭浏览器失效）+ localStorage（持久化）
- "记住我"= true 时双写，false 时仅 sessionStorage 并移除 localStorage
- token 在响应体 `msg` 字段返回（非 `data`），用户信息在 `res.data`

---

### 6.5 共享组件

#### Top.vue

文件：[Top.vue](file:///d:/IDEA_Project/Cloudland/vue/src/components/Top.vue)

前台完整导航栏 + 登录/注册弹窗一体化组件，用于首页、LandView、ProductView。

- 顶部导航：logo、首页/介绍/功能/联系我们/订阅/后台链接
- 右上角动态显示：未登录显示登录图标（弹登录表单）；已登录显示"欢迎您，{username}"+头像
- 登录表单：手机号、密码、验证码、"记住我"、忘记密码、登录、"注册一个"
- 注册表单：用户名、手机号、密码、年龄、QQ邮箱、`el-cascader` 三级地址、详细地址、验证码
- 验证码发送：`POST /user/code`，59 秒倒计时
- 登录：`loginWithPassword({frond:true, remember, code})`
- 注册：`POST /user/register`（headers 带 `frond:'true'` + `code`）
- `mounted` 调 `openCheck()` 静默登录

#### Top2.vue

文件：[Top2.vue](file:///d:/IDEA_Project/Cloudland/vue/src/components/Top2.vue)

前台精简导航栏（无登录弹窗），用于 LandInfoView、ProductInfoView、UserView 等内页。

- 仅 logo + "首页" + 用户头像/未登录提示
- 解析用户地址（`CodeToText` 转省市区）
- 将 `user.id` 写入 `sessionStorage.userID`
- 处理 `sessionStorage.replace === '1'` 时 `location.reload()` 强制刷新

---

### 6.6 视图层

#### 前台视图（`views/frontend/`）

| 视图 | 文件 | 核心职责 |
|---|---|---|
| MainView | [MainView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/MainView.vue) | 布局容器，仅 `<router-view/>` |
| HomeView | [HomeView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/HomeView.vue) | 首页：banner、介绍、6 个功能区、留言表单（`POST /msg`）、订阅（`POST /msg/mail`）、百度地图 |
| LandView | [LandView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/LandView.vue) | 土地列表：搜索+多条件筛选+分页（`POST /land/page`） |
| LandInfoView | [LandInfoView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/LandInfoView.vue) | 土地详情：多图轮播、资料下载、加入预约/立即租用（`POST /user/addTrolley`，num=-1） |
| ProductView | [ProductView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/ProductView.vue) | 产品列表：卡片轮播、原价/现价（`POST /product/page`） |
| ProductInfoView | [ProductInfoView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/ProductInfoView.vue) | 产品详情：购物车/立即购买（num=1） |
| UserView | [UserView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/UserView.vue) | 用户中心容器：左侧菜单 + `<keep-alive><router-view/></keep-alive>` |
| ForgetPassword | [ForgetPassword.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/ForgetPassword.vue) | 忘记密码重置（headers: `forgetPassword:'true'` 跳过旧密码校验） |
| userInfo/MyOrder | [MyOrder.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/userInfo/MyOrder.vue) | 我的订单：多选、模拟支付、支付宝支付（`POST /alipay/pay` 返回 HTML 表单跳转） |
| userInfo/MyTrolley | [MyTrolley.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/userInfo/MyTrolley.vue) | 购物车：数量调节、结算（循环 `POST /order` + 删购物车） |
| userInfo/UserContactView | [UserContactView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/userInfo/UserContactView.vue) | 账号绑定（headers: `contact:'true'`） |
| userInfo/UserMsgView | [UserMsgView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/userInfo/UserMsgView.vue) | 个人资料：头像上传、资料修改（`PUT /user`） |
| userInfo/UserPassword | [UserPassword.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/frontend/userInfo/UserPassword.vue) | 密码修改：新密码放在 `user.detailedAddress` 字段（前后端约定复用） |

#### 后台视图（`views/backend/`）

| 视图 | 文件 | 核心职责 |
|---|---|---|
| MainView | [MainView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/MainView.vue) | 后台布局：顶栏+侧边菜单+内容区+登录弹窗；将 user 作为 props 传子路由；power=1 仅看自己数据，power=2 看全部 |
| HomeView | [HomeView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/HomeView.vue) | 后台首页（背景图+欢迎文案） |
| Customer_aView | [Customer_aView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/Customer_aView.vue) | 普通用户管理（power=0） |
| Customer_bView | [Customer_bView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/Customer_bView.vue) | 加盟用户管理（power=1），结构与 Customer_aView 几乎一致 |
| EmployeeView | [EmployeeView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/EmployeeView.vue) | 平台员工管理（power=2），自改信息后 `location.reload()` |
| LandView | [LandView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/LandView.vue) | 土地管理：图集预览、资料/图片上传、代理人选择（`POST /user/employee`） |
| ProductView | [ProductView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/ProductView.vue) | 产品管理：封面上传、库存 |
| LandOrderView | [LandOrderView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/LandOrderView.vue) | 土地订单管理（`order.num=-1`）、导出 `云用地_用地订单.xlsx` |
| ProductOrderView | [ProductOrderView.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/ProductOrderView.vue) | 产品订单管理、导出 `云用地_产品订单.xlsx` |
| PushMsg | [PushMsg.vue](file:///d:/IDEA_Project/Cloudland/vue/src/views/backend/PushMsg.vue) | 留言查看（`POST /msg/page`）+ 消息推送（`POST /msg/push`） |

> **代码重复**：Customer_aView/Customer_bView/EmployeeView 高度重复，仅默认 `user.power` 不同；LandOrderView/ProductOrderView 仅 `order.num` 与导出文件名不同。

---

## 7. 数据库设计

数据库初始化脚本：[cloudland.sql](file:///d:/IDEA_Project/Cloudland/cloudland.sql)，共 10 张表，存储引擎 InnoDB。

### 7.1 ER 关系概览

```
user (1) ─────< land (N)              [a_id: 所有者]
user (1) ─────< land (N)              [employee_id: 代理人]
land_type (1) ─< land (N)             [land_type: 类型]
land (1) ─────< cloudland_file (N)    [land_id, ON DELETE CASCADE]
user (1) ─────< order2 (N)            [u_id, ON DELETE CASCADE]
user (1) ─────< product (N)           [a_id, ON DELETE CASCADE]
user (1) ─────< trolley (N)           [u_id, ON DELETE CASCADE]
```

### 7.2 表结构

| 表 | 主键 | 关键字段 | 约束/索引 |
|---|---|---|---|
| `user` | id | username, password(BCrypt), phone, mail, power, status, debt, detailed_address | UNIQUE(phone)、UNIQUE(mail) |
| `land_type` | id | type_name, description | 土地类型字典（6 类） |
| `land` | id | land_name, land_type, price, area, a_id, employee_id, status, ordered, detailed_address | FK(land_type)、FK(a_id→user)、FK(employee_id→user) |
| `cloudland_file` | id | path, land_id, type(0图片/1资料) | FK(land_id→land) **ON DELETE CASCADE** |
| `product` | id | product_name, price, num(库存), status, a_id, img, ordered | FK(a_id→user) ON DELETE CASCADE |
| `order2` | id | p_id, num(-1用地/数量), u_id, create_time, pay_time, status, del | FK(u_id→user) ON DELETE CASCADE |
| `trolley` | id | p_id, num, u_id | FK(u_id→user)、FK(p_id) |
| `msg` | id | name, mail, phone, msg, send_time | 无外键 |
| `msg_send` | (id, mail) | mail | 复合主键 |
| `address` | address_areaid | address_name, address_regionid | 预留地址字典，业务未直接使用 |

### 7.3 土地类型字典

| id | type_name |
|---|---|
| 1 | 农用地 |
| 2 | 建设用地 |
| 3 | 商业用地 |
| 4 | 公共管理与公共服务用地 |
| 5 | 水域及水利设施用地 |
| 6 | 其他土地 |

### 7.4 关键业务字段约定

- `order2.num`：**-1 = 土地订单**（用地），>=0 = 产品数量。这是区分两类订单的核心字段，贯穿前后端。
- `trolley.num`：同上，-1 表示用地预约。
- `cloudland_file.type`：0 = 图片，1 = 用地资料压缩包。
- `user.power`：0 客户 / 1 员工 / 2 管理员。
- `user.status`：0 禁用 / 1 激活。
- `land.status`：0 可用 / 1 已租用。
- `order2.status`：0 未支付 / 1 已支付。
- `order2.del`：0 正常 / 1 软删。
- `address` 字段格式：`省代码,市代码,区代码`（如 `510000,510100,510101`）。

---

## 8. 认证与授权机制

### 8.1 JWT 认证流程

```
1. 登录 POST /api/user/login
   → UserServiceImpl.loginUser
   → BCrypt 校验密码 + status 校验
   → JwtUtils.generateJwt(claims, remember)
   → 返回 Result(LOGIN_OK, user, jwt)  // jwt 在 msg 字段

2. 后续请求带 token 头
   → MyInterceptor.preHandle
   → JwtUtils.parseJWT
   → UserMapper.selectById(claims.id)
   → 校验 status / power 一致性
   → 通过放行 / 失败返回 401/402/403

3. 响应头 updatedToken（token 刷新）
   → 前端 interceptor.js 检测 → persistToken 刷新本地
```

### 8.2 JWT Claims

`buildTokenClaims(User)` 构造：`id`、`phone`、`username`、`power`、`debt`。

### 8.3 免认证路径

以下路径拦截器放行（在 WebMvcConfig `excludePathPatterns` 或 URL 关键字匹配）：

- `/resource/**`（静态文件）
- `/land/page`、`/land/{id}`（公开土地）
- `/product/page`、`/product/{id}`（公开产品）
- `/msg`、`/msg/mail`（公开留言/订阅）
- `/alipay/notify`、`/alipay/return`（支付回调）
- `/error`
- URL 含 `login`、`register`、`code`、`forgetPassword` 关键字

### 8.4 自定义请求头语义

| 头 | 取值 | 语义 |
|---|---|---|
| `token` | JWT 字符串 | 认证令牌 |
| `remember` | `true`/`false` | 是否"记住我"（影响 JWT 有效期与存储位置） |
| `frond` | `true`/`false` | 前台(true)/后台(false)请求标识，影响登录与改密逻辑 |
| `code` | 6 位验证码 | 登录/注册邮箱验证码 |
| `contact` | `true` | 标识修改联系方式（手机/邮箱） |
| `forgetPassword` | `true` | 标识忘记密码重置流程，跳过旧密码校验 |

### 8.5 HTTP 状态码（非标准语义）

| HTTP | 语义 | 前端处理 |
|---|---|---|
| 401 | 未认证（无 token / token 无效 / 用户不存在） | 清状态跳首页 |
| 402 | 账号权限已变更 | alert + 清状态跳首页 |
| 403 | 账号被禁用 | alert + 清状态跳首页 |
| 500 | 服务器内部错误 | 仅 console.error |

---

## 9. 核心业务流程

### 9.1 用户登录（双模式）

```
POST /api/user/login
  → UserController.login(@RequestBody User, request)
  → UserServiceImpl.loginUser
      ├─ 读 header: remember/frond/token/code
      ├─ 有 token 无密码 → token 验证模式
      │    ├─ JwtUtils.parseJWT
      │    ├─ UserMapper.selectById
      │    ├─ 校验 phone/status/power
      │    └─ 签发新 token 放入 Result.msg
      └─ 有密码 → 密码登录模式
           ├─ (有 code) Redis 取验证码 CodeCheck
           ├─ UserMapper.selectOne(phone=?) 
           ├─ BCryptPasswordEncoder.matches
           ├─ status 校验
           └─ JwtUtils.generateJwt
```

### 9.2 土地分页查询（公开）

```
POST /api/land/page
  → LandController.selectPage(pageNum, pageSize, land JSON)
  → JSON.parseObject → Land
  → LandServiceImpl.selectPage
      ├─ LandMapper.selectByCondition  // XML: 4 表 JOIN + 动态 where + choose 排序
      └─ 遍历 LandVO 补 cloudland_file（type=0 图片 / type=1 资料）
```

### 9.3 下单（产品订单）

```
POST /api/order  (@RequestBody Order2)
  → Order2ServiceImpl.addOrder  @Transactional
      ├─ ProductMapper.selectById  // 校验库存/状态
      ├─ ProductMapper.updateById  // 扣减 num
      └─ Order2Mapper.insert       // status=0, del=0
```

### 9.4 支付宝支付完整链

```
1) POST /api/alipay/pay (orderIds[], totalAmount, userId)
   → AlipayController.pay
       ├─ 生成 outTradeNo = "CLD"+ts+userId
       ├─ AlipayClient.pageExecute → form HTML
       ├─ Order2ServiceImpl.saveTradeMapping  // Redis 存 orderIds (30min TTL)
       └─ 返回 form HTML

2) GET /api/alipay/return?out_trade_no=xxx  (用户跳回)
   → AlipayController.alipayReturn
       ├─ AlipayClient.execute(AlipayTradeQueryRequest) 查询
       └─ Order2ServiceImpl.handlePaySuccess  @Transactional
             ├─ Redis 取 orderIds
             ├─ updateOrder(ids, 1, true)  // status=1, 写 payTime
             └─ Redis 删映射

3) POST /api/alipay/notify  (异步通知，独立于 Result 体系)
   → AlipayController.notify
       ├─ AlipaySignature.rsaCheckV1 验签
       └─ 同 handlePaySuccess，返回 "success"/"failure"
```

### 9.5 订单 Excel 导出

```
POST /api/order/download (order JSON)
  → Order2ServiceImpl.getOrder
      ├─ (num=-1) Order2Mapper.selectOrder  → 补 Land 图片
      │  (else)  Order2Mapper.selectOrder2
      ├─ OrderExporter.exportToExcel  // POI 写 .xlsx 到磁盘
      └─ DownloadUtil.getFile  // 流式下载
```

### 9.6 定时清理挂单（无 Controller）

```
每日 00:00:00
  → OrderClear.executeTask  @Scheduled @Transactional
      ├─ Order2Mapper.selectList(null)
      └─ 遍历 status=0 订单：
           ├─ (num=-1) LandMapper.updateById(status=1)  // 恢复土地可用
           ├─ (else)   ProductMapper.updateById(恢复 num) // 恢复库存
           └─ Order2Mapper.deleteById  // 硬删
```

### 9.7 订单删除/退单逻辑

```
DELETE /api/order/{id}
  → Order2ServiceImpl.deleteOrder  @Transactional
      ├─ 已支付(status=1) → 软删 del=1
      └─ 未支付(status=0)
           ├─ (num=-1) 恢复 land.status=1
           ├─ (else)   恢复 product.num
           └─ 硬删 deleteById
```

---

## 10. 文件存储机制

### 10.1 存储根目录

由 `FILE_STORAGE_ROOT` 环境变量指定，默认 `D:/CloudLandFile`（项目目录外）。Docker 中挂载于卷 `cloudland-files` → `/cloudland-files`。

### 10.2 目录结构

```
{FILE_STORAGE_ROOT}/
├── UserIcon/                          # 用户头像
│   └── userIcon_{uuid}.jpg
├── Product/                           # 产品图片
│   └── product_{uuid}.jpg
└── LandFile/                          # 土地文件
    └── Land_{id}/
        ├── CloudLandFile/             # 用地资料原始文件（压缩前）
        ├── Images/                    # 土地预览图
        │   └── Img_{id}_{code}.jpg
        └── 云用地_{id}{code}.zip       # 用地资料压缩包
```

### 10.3 URL 映射

| 物理路径 | 访问 URL |
|---|---|
| `{root}/UserIcon/{file}` | `/resource/userFile/{file}` |
| `{root}/LandFile/Land_{id}/{file}` | `/resource/landFile/{file}` |
| `{root}/Product/{file}` | `/resource/productFile/{file}` |

由 [WebMvcConfig](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/WebMvcConfig.java) 的 `addResourceHandlers` 配置，所有 `/resource/**` 路径拦截器放行。

### 10.4 文件命名规则

- 用户头像：`userIcon_{UUID}.{ext}`
- 产品图片：`product_{UUID}.{ext}`
- 土地图片：`Img_{landId}_{code}.{ext}`
- 土地资料压缩包：`云用地_{landId}{code}.zip`
- 订单导出：`云用地_用地订单.xlsx` / `云用地_产品订单.xlsx`

### 10.5 文件上传约定

- Content-Type：`multipart/form-data`
- 业务参数为 **JSON 字符串**（非对象）：`formData.append('land', JSON.stringify(this.Land))`
- 文件参数名：`userIcon`（头像）、`imageFiles`（土地预览图，多张）、`landFiles`（土地资料，多个）、`productImg`（产品封面）
- 大小限制：前端校验 5MB，后端限制 60MB

---

## 11. 状态码体系

定义在 [Code.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Code.java)，前端镜像在 [code.js](file:///d:/IDEA_Project/Cloudland/vue/src/constants/code.js)。

**设计原则**：使用 10000+ 范围避开 HTTP 标准状态码（100-599）。

### 11.1 通用状态码

| 码 | 常量 | 说明 |
|---|---|---|
| 0 | SUCCESS | 通用成功 |
| -1 | FAILURE | 通用失败 |

### 11.2 CRUD（10000-10999）

| 码 | 常量 | 说明 |
|---|---|---|
| 10001 | ADD_OK / ADD_ERR(10005) | 添加成功/失败 |
| 10002 | DELETE_OK / DELETE_ERR(10006) | 删除成功/失败 |
| 10003 | UPDATE_OK / UPDATE_ERR(10007) | 修改成功/失败 |
| 10004 | SELECT_OK / SELECT_ERR(10008) | 查询成功/失败 |

### 11.3 认证授权（20000-20999）

| 码 | 常量 | 说明 |
|---|---|---|
| 20001 | PHONE_NO_EXIST | 账号不存在 |
| 20002 | PASSWORD_ERR | 密码错误 |
| 20003 | TOKEN_ERR | 令牌过期 |
| 20004 | STATUS_ERR | 账号被禁用 |
| 20005 | LOGIN_OK | 登录成功 |
| 20006 | REGISTER_OK | 注册成功 |
| 20007 | PHONE_EXIST | 手机号已注册 |
| 20008 | POWER_ERR | 权限不足 |
| 20009 | LOGIN_RETURN | 请重新登陆 |
| 20010 | PASSWORD_SAME | 密码相同 |
| 20011 | UPDATE_SAME | 绑定信息相同 |

### 11.4 邮件通知（30000-30999）

| 码 | 常量 | 说明 |
|---|---|---|
| 30001 | SEND_MAIL_OK | 邮件发送成功 |
| 30002 | SEND_MAIL_ERR | 邮件发送失败 |
| 30003 | CODE_ERR | 验证码错误 |
| 30004 | MAIL_EXIST | 邮箱已注册 |

`Code.isSuccess(code)`：命中 SUCCESS 或任意 `_OK` 后缀的成功码视为成功。

### 11.5 统一响应格式

```json
{
  "data": {},        // 响应数据
  "code": 10004,     // 业务状态码
  "msg": "查询成功"   // 消息说明（登录时 msg 字段存放 JWT）
}
```

---

## 12. 部署与运行

### 12.1 本地开发

#### 后端（端口 9090）

```bash
# 前置：MySQL 8（cloudland 库）+ Redis 7 运行中
mvn clean package -DskipTests    # 后端无测试类，mvn test 是 no-op
mvn spring-boot:run              # 主类 com.cloudland.CloudlandApplication
```

数据库初始化：导入 [cloudland.sql](file:///d:/IDEA_Project/Cloudland/cloudland.sql)。

本地配置覆盖：创建 `config/application-local.yml`（gitignored，含真实凭据，勿提交），通过 `spring.config.import` 加载。

#### 前端（端口 8080）

```bash
cd vue
npm install
npm run serve    # http://localhost:8080
npm run build    # 生产构建
```

前端环境变量（`vue/.env.local`）：
- `VUE_APP_API_BASE_URL`（dev `http://localhost:9090/api`，prod `http://8.137.114.176/api`）
- `VUE_APP_FRONTEND_BASE_URL`（默认 `window.location.origin`）

前端构建配置见 [vue.config.js](file:///d:/IDEA_Project/Cloudland/vue/vue.config.js)：`devServer.port=8080`、`host=localhost`、`productionSourceMap=true`。

### 12.2 Docker 完整栈部署

配置见 [docker-compose.yml](file:///d:/IDEA_Project/Cloudland/docker-compose.yml)：

```bash
docker-compose up -d
```

启动 4 个服务：

| 服务 | 容器 | 端口 | 说明 |
|---|---|---|---|
| backend | cloudland-backend | 9090（内部） | Spring Boot，挂载 cloudland-files 卷 |
| frontend | cloudland-frontend | 80 | Nginx 提供前端静态文件 + 反代 `/api/` |
| mysql | cloudland-mysql | 3306 | MySQL 8.0，初始化 cloudland.sql |
| redis | cloudland-redis | 6379（内部） | Redis 7-alpine，requirepass 123456 |

**镜像源**：从 `docker.1panel.live` 拉取（非 Docker Hub 直连）。

**Nginx 配置**（[nginx.conf](file:///d:/IDEA_Project/Cloudland/nginx.conf)）：
- `/api/` → `proxy_pass http://backend:9090/api/`（client_max_body_size 60m）
- `/` → 前端静态文件，`try_files $uri $uri/ /index.html`（SPA history 回退）

### 12.3 生产部署脚本

[scripts/deploy.sh](file:///d:/IDEA_Project/Cloudland/scripts/deploy.sh) 流程：

1. `flock` 文件锁防并发部署
2. `git fetch` + `git reset --hard origin/{branch}`
3. `docker compose pull mysql redis`
4. `docker compose build --pull backend frontend`
5. 停止宿主机 nginx（避免端口 80 冲突，`systemctl stop nginx` + `pkill`）
6. `docker compose up -d --force-recreate` 启动所有容器
7. 轮询等待 frontend 容器 running（最多 12 次 × 5s）
8. `docker image prune -f` 清理未使用镜像

### 12.4 CI/CD

[.gitee/workflows/cloudland-deploy.yml](file:///d:/IDEA_Project/Cloudland/.gitee/workflows/cloudland-deploy.yml)：push 到 `main` 分支触发，通过 SSH 执行 `scripts/deploy.sh`。需配置 Secrets：`SERVER_HOST`、`SERVER_USER`、`SERVER_SSH_KEY`、`SERVER_PROJECT_PATH`。

### 12.5 关键环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cloudland...` | 数据库连接 |
| `DB_USERNAME` / `DB_PASSWORD` | root / 123456 | 数据库凭据 |
| `REDIS_HOST` / `REDIS_PASSWORD` | localhost / 123456 | Redis |
| `MAIL_*` | smtp.qq.com | 邮件 |
| `JWT_SIGN_KEY` | change-me-in-production | JWT 密钥（生产必改） |
| `JWT_EXPIRE` / `JWT_WEEK` | 604800000 | JWT 有效期 |
| `FILE_STORAGE_ROOT` | D:/CloudLandFile | 文件根目录 |
| `CORS_ALLOWED_ORIGINS` | http://localhost:8080,... | CORS 白名单 |
| `ALIPAY_*` | 沙箱 | 支付宝配置 |

环境变量模板见 [.env.example](file:///d:/IDEA_Project/Cloudland/.env.example)。

---

## 13. 测试体系

### 13.1 后端测试

**无后端单元测试**。`mvn test` 不会执行任何测试。

### 13.2 前端 E2E 测试（Playwright）

配置：[vue/playwright.config.js](file:///d:/IDEA_Project/Cloudland/vue/playwright.config.js)，测试：[vue/tests/](file:///d:/IDEA_Project/Cloudland/vue/tests/)

```bash
cd vue
npx playwright test                       # 全部测试
npx playwright test tests/login.spec.js   # 单文件
npx playwright test --project=webkit      # 仅 webkit
```

**前提**：后端 + 前端 + MySQL + Redis 全部运行。

**配置要点**：
- baseURL：`http://localhost:8080`
- workers：1（串行执行）
- retries：本地 1，CI 2
- 项目：webkit + Microsoft Edge（Edge 启用 slowMo: 800ms）

**测试文件**：
- [login.spec.js](file:///d:/IDEA_Project/Cloudland/vue/tests/login.spec.js) / [login-smoke.spec.js](file:///d:/IDEA_Project/Cloudland/vue/tests/login-smoke.spec.js)
- [register-smoke.spec.js](file:///d:/IDEA_Project/Cloudland/vue/tests/register-smoke.spec.js)

**Helpers**（[vue/tests/helpers/](file:///d:/IDEA_Project/Cloudland/vue/tests/helpers/)）：
- [redis.js](file:///d:/IDEA_Project/Cloudland/vue/tests/helpers/redis.js)：用 `ioredis` 直接写验证码到 Redis，绕过邮件发送
- [auth.js](file:///d:/IDEA_Project/Cloudland/vue/tests/helpers/auth.js)
- [cleanup.js](file:///d:/IDEA_Project/Cloudland/vue/tests/helpers/cleanup.js)：用 admin API 清理测试数据

---

## 14. 已知陷阱与编码约定

### 14.1 已知陷阱

| 陷阱 | 说明 |
|---|---|
| groupId 拼写错误 | pom.xml groupId 为 `com.couldland`，实际包名 `com.cloudland` |
| README 描述不符 | README 称"Bootstrap 5"，实际 UI 是 Element UI（@opentiny/vue 已安装但未使用） |
| Order2 命名 | 实体名为 `Order2` 非 `Order`，避免 SQL 关键字 `ORDER` 冲突 |
| Interceptor 目录 | `Interceptor/` 首字母大写 I，与 Java 包命名惯例不一致 |
| HTTP 状态码非标准 | 401/402/403 非标准语义，前端拦截器专门处理 |
| token 头 | 是 `token` 不是 `Authorization`；响应头 `updatedToken`（小写） |
| 无后端测试 | 仅前端 Playwright E2E 测试 |
| Vuex 未使用 | 已安装但实际靠 sessionStorage/localStorage + 路由 props |
| local 配置含凭据 | `config/application-local.yml` 含真实邮件密码/支付宝密钥，勿提交 |
| 后台视图重复 | Customer_a/b/EmployeeView 高度重复；LandOrderView/ProductOrderView 高度重复 |
| PushMsg 组件名 | PushMsg.vue 内组件名 `OrderView`，与文件名不一致 |
| 密码字段复用 | UserPassword.vue 将新密码放 `user.detailedAddress` 字段传递 |
| 业务码使用混乱 | 虽定义 code.js 常量，组件中多数直接用数字字面量 |

### 14.2 编码约定

- **资源 URL**：统一通过 `APP_CONFIG.resourceUrls` 拼接，不硬编码后端地址
- **状态码分层**：HTTP 状态码在拦截器处理，业务状态码在业务组件处理
- **文件上传**：`multipart/form-data` + JSON 字符串参数
- **统一响应**：所有接口返回 `Result { data, code, msg }`（支付宝 notify 例外）
- **分页**：`pageNum`（从 1 开始）+ `pageSize`（默认 5），响应 MyBatis-Plus `Page` 结构
- **地址编码**：`省代码,市代码,区代码` 格式
- **角色权限**：power=1 仅看自己数据（`aId = parentData.id`），power=2 看全部

### 14.3 sessionStorage 标记位

| 标记 | 用途 |
|---|---|
| `token` | 当前会话 token |
| `uid` | 员工身份标识（后台 power=1） |
| `userID` | 当前用户 ID |
| `uImg` | 当前用户头像（员工自改信息后刷新判断） |
| `replace` | 强制刷新标记（`'1'` 时 `location.reload()`） |
| `main` | 是否首页标记 |

---

## 附录：关键文件索引

### 后端

| 类别 | 文件 |
|---|---|
| 启动类 | [CloudlandApplication.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/CloudlandApplication.java) |
| 配置 | [application.yml](file:///d:/IDEA_Project/Cloudland/src/main/resources/application.yml) |
| Controller | [UserController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/UserController.java)、[LandController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/LandController.java)、[ProductController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/ProductController.java)、[OrderController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/OrderController.java)、[MsgController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/MsgController.java)、[AlipayController](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/AlipayController.java) |
| 响应封装 | [Result.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Result.java)、[Code.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Code.java)、[Msg.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/controller/result/Msg.java) |
| Config | [WebMvcConfig](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/WebMvcConfig.java)、[StorageProperties](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/StorageProperties.java)、[RedisConfig](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/RedisConfig.java)、[AlipayConfig](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/config/AlipayConfig.java) |
| Interceptor | [MyInterceptor.java](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/Interceptor/MyInterceptor.java) |
| Util | [JwtUtils](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/JwtUtils.java)、[EmailUtils](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/EmailUtils.java)、[FileUtil](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/FileUtil.java)、[OrderExporter](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/OrderExporter.java)、[OrderClear](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/OrderClear.java)、[DownloadUtil](file:///d:/IDEA_Project/Cloudland/src/main/java/com/cloudland/util/DownloadUtil.java) |

### 前端

| 类别 | 文件 |
|---|---|
| 入口 | [main.js](file:///d:/IDEA_Project/Cloudland/vue/src/main.js)、[App.vue](file:///d:/IDEA_Project/Cloudland/vue/src/App.vue) |
| 配置 | [config/app.js](file:///d:/IDEA_Project/Cloudland/vue/src/config/app.js)、[constants/code.js](file:///d:/IDEA_Project/Cloudland/vue/src/constants/code.js) |
| 路由/请求 | [router/index.js](file:///d:/IDEA_Project/Cloudland/vue/src/router/index.js)、[axiosInstance.js](file:///d:/IDEA_Project/Cloudland/vue/src/request/axiosInstance.js)、[interceptor.js](file:///d:/IDEA_Project/Cloudland/vue/src/request/interceptor.js) |
| 认证 | [utils/auth.js](file:///d:/IDEA_Project/Cloudland/vue/src/utils/auth.js) |
| 组件 | [Top.vue](file:///d:/IDEA_Project/Cloudland/vue/src/components/Top.vue)、[Top2.vue](file:///d:/IDEA_Project/Cloudland/vue/src/components/Top2.vue) |

### 部署

| 文件 | 说明 |
|---|---|
| [docker-compose.yml](file:///d:/IDEA_Project/Cloudland/docker-compose.yml) | 容器编排 |
| [nginx.conf](file:///d:/IDEA_Project/Cloudland/nginx.conf) | 前端 Nginx 配置 |
| [Dockerfile](file:///d:/IDEA_Project/Cloudland/Dockerfile) | 后端镜像 |
| [vue/Dockerfile](file:///d:/IDEA_Project/Cloudland/vue/Dockerfile) | 前端镜像 |
| [scripts/deploy.sh](file:///d:/IDEA_Project/Cloudland/scripts/deploy.sh) | 生产部署脚本 |
| [cloudland.sql](file:///d:/IDEA_Project/Cloudland/cloudland.sql) | 数据库初始化 |

### 其他文档

| 文件 | 说明 |
|---|---|
| [AGENTS.md](file:///d:/IDEA_Project/Cloudland/AGENTS.md) | Agent 工作指南 |
| [CLAUDE.md](file:///d:/IDEA_Project/Cloudland/CLAUDE.md) | Claude Code 指导 |
| [vue/CLAUDE.md](file:///d:/IDEA_Project/Cloudland/vue/CLAUDE.md) | 前端专用指导 |
| [API.md](file:///d:/IDEA_Project/Cloudland/API.md) | API 接口文档 |

---

*文档生成时间：2026-08-06 · 基于 Cloudland 源码现状整理*
