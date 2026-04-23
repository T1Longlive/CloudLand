

# Cloudland 云地系统

Cloudland 是一个基于 Spring Boot 和 Vue 2 的云端土地租赁与农产品交易平台。系统提供土地管理、产品订购、订单处理、用户管理等核心功能，支持支付宝支付集成。

## 项目架构

### 后端技术栈
- **框架**: Spring Boot 2.7.14
- **Java 版本**: JDK 8
- **数据库**: MySQL + MyBatis Plus
- **缓存**: Redis
- **认证**: JWT
- **支付**: 支付宝沙箱支付

### 前端技术栈
- **框架**: Vue 2.6
- **UI**: Bootstrap 5
- **状态管理**: Vuex
- **网络**: Axios
- **路由**: Vue Router

## 核心功能模块

### 1. 用户管理 (`/user`)
- 用户登录与注册
- 密码找回（邮箱验证码）
- 购物车管理
- 用户信息修改
- 权限控制（普通用户/员工/管理员）

### 2. 土地管理 (`/land`)
- 土地信息CRUD
- 土地文件上传（云文件/图片）
- 分页查询
- 土地类型管理

### 3. 产品管理 (`/product`)
- 农产品CRUD
- 产品图片管理
- 分页查询
- 库存管理

### 4. 订单管理 (`/order`)
- 订单创建与查询
- 订单状态更新
- 订单报表导出（Excel）
- 购物车功能

### 5. 消息管理 (`/msg`)
- 留言提交
- 邮件订阅推送
- 消息分页查询

### 6. 支付集成 (`/alipay`)
- 支付宝支付
- 支付回调处理
- 订单支付状态同步

## 项目结构

```
cloudland/
├── src/main/java/com/cloudland/
│   ├── CloudlandApplication.java          # Spring Boot 启动类
│   ├── config/                             # 配置类
│   │   ├── AlipayConfig.java               # 支付宝配置
│   │   ├── MybatisPlusConfig.java          # MyBatis Plus 配置
│   │   ├── RedisConfig.java                # Redis 配置
│   │   ├── StorageProperties.java          # 文件存储配置
│   │   └── WebMvcConfig.java               # Web MVC 配置
│   ├── controller/                         # 控制器
│   │   ├── UserController.java
│   │   ├── LandController.java
│   │   ├── ProductController.java
│   │   ├── OrderController.java
│   │   ├── MsgController.java
│   │   └── AlipayController.java
│   ├── service/                            # 业务逻辑
│   │   ├── impl/
│   │   └── IXXXService.java
│   ├── mapper/                             # 数据访问层
│   ├── pojo/                               # 实体类
│   │   ├── User.java
│   │   ├── Land.java
│   │   ├── Product.java
│   │   ├── Order2.java
│   │   └── ...
│   ├── util/                               # 工具类
│   │   ├── JwtUtils.java                   # JWT 工具
│   │   ├── EmailUtils.java                 # 邮件工具
│   │   ├── FileUtil.java                   # 文件工具
│   │   └── OrderClear.java                 # 定时任务
│   └── Interceptor/
│       └── MyInterceptor.java              # JWT 拦截器
├── vue/                                    # 前端项目
│   ├── src/
│   │   ├── views/                          # 页面组件
│   │   │   ├── frontend/                   # 前台页面
│   │   │   └── backend/                    # 后台管理页面
│   │   ├── components/                     # 公共组件
│   │   ├── router/                         # 路由配置
│   │   ├── request/                        # HTTP 请求封装
│   │   └── config/                         # 配置文件
│   └── package.json
├── cloudland.sql                           # 数据库初始化脚本
├── docker-compose.yml                      # Docker 编排
├── Dockerfile                              # 后端 Docker 镜像
└── pom.xml                                 # Maven 依赖配置
```

## 快速开始

### 环境要求
- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis
- Node.js 16+ (前端)

### 后端配置

1. 导入数据库：
```bash
mysql -u root -p < cloudland.sql
```

2. 配置 `application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/cloudland
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
  mail:
    host: smtp.example.com
    username: your_email
    password: your_password

jwt:
  signKey: your_sign_key
  expire: 3600000
  week: 604800

alipay:
  appId: your_app_id
  privateKey: your_private_key
  publicKey: alipay_public_key
  gatewayUrl: https://openapi-sandbox.alipay.com/gateway.do
  notifyUrl: http://localhost:9090/alipay/notify
  returnUrl: http://localhost:8080

access-file:
  location: /app/files
  resource-handler1: /files/**
```

3. 启动后端：
```bash
./mvnw spring-boot:run
# 或
java -jar target/cloudland.jar
```

### 前端配置

1. 安装依赖：
```bash
cd vue
npm install
```

2. 启动开发服务器：
```bash
npm run serve
# 访问 http://localhost:8080
```

3. 生产环境构建：
```bash
npm run build
```

### Docker 部署

```bash
# 构建并启动所有服务
docker-compose up -d

# 后端单独构建
docker build -t cloudland-backend .

# 运行后端容器
docker run -d -p 9090:9090 --name cloudland-backend cloudland-backend
```

## API 接口概览

| 模块 | 接口路径 | 说明 |
|------|----------|------|
| 用户 | `/user/login` | 用户登录 |
| 用户 | `/user/register` | 用户注册 |
| 用户 | `/user/code` | 发送验证码 |
| 用户 | `/user/trolley` | 购物车 |
| 土地 | `/land` | 土地CRUD |
| 产品 | `/product` | 产品CRUD |
| 订单 | `/order` | 订单CRUD |
| 订单 | `/order/download` | 导出订单报表 |
| 消息 | `/msg` | 留言管理 |
| 支付 | `/alipay/pay` | 支付宝支付 |
| 支付 | `/alipay/notify` | 支付回调 |

详细 API 文档请参考 [API.md](./API.md)

## 核心业务逻辑

### 用户角色 (power 字段)
- `0`: 普通用户
- `1`: 员工
- `2`: 管理员

### 订单状态
- `0`: 待支付
- `1`: 已支付
- `2`: 已取消
- `3`: 已完成

### 定时任务
- **订单清理** (`OrderClear`): 每天0点执行，清除未支付过期订单并恢复库存

### 文件存储
系统支持以下文件存储：
- 用户头像: `/files/user/`
- 产品图片: `/files/product/`
- 土地文件: `/files/land/{landId}/`
- 土地云文件: `/files/land/{landId}/cloud/`
- 土地图片: `/files/land/{landId}/images/`

## 开发注意事项

1. **JWT 认证**: 除登录、注册、找回密码、验证码接口外，其他接口需要携带 JWT Token
2. **支付集成**: 支付宝支付需要在支付宝开放平台申请沙箱账号
3. **邮件配置**: 需要配置有效的 SMTP 邮件服务器
4. **文件上传**: 确保上传目录有写权限

## 许可证

本项目仅供学习交流使用。