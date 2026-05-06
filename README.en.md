# Cloudland Cloud Land System

Cloudland is a cloud-based land leasing and agricultural product trading platform built on Spring Boot and Vue 2. The system provides core functionalities including land management, product ordering, order processing, and user management, with integrated Alipay payment support.

## Project Architecture

### Backend Technology Stack
- **Framework**: Spring Boot 2.7.14
- **Java Version**: JDK 8
- **Database**: MySQL + MyBatis Plus
- **Cache**: Redis
- **Authentication**: JWT
- **Payment**: Alipay Sandbox Payment

### Frontend Technology Stack
- **Framework**: Vue 2.6
- **UI**: Bootstrap 5
- **State Management**: Vuex
- **Networking**: Axios
- **Routing**: Vue Router

## Core Functional Modules

### 1. User Management (`/user`)
- User login and registration
- Password recovery (email verification code)
- Shopping cart management
- User information modification
- Role-based access control (Regular User / Staff / Admin)

### 2. Land Management (`/land`)
- CRUD operations for land information
- Land file uploads (cloud files/images)
- Paginated queries
- Land type management

### 3. Product Management (`/product`)
- CRUD operations for agricultural products
- Product image management
- Paginated queries
- Inventory management

### 4. Order Management (`/order`)
- Order creation and querying
- Order status updates
- Order report export (Excel)
- Shopping cart functionality

### 5. Message Management (`/msg`)
- Message submission
- Email subscription and推送
- Paginated message queries

### 6. Payment Integration (`/alipay`)
- Alipay payment processing
- Payment callback handling
- Order payment status synchronization

## Project Structure

```
cloudland/
├── src/main/java/com/cloudland/
│   ├── CloudlandApplication.java          # Spring Boot startup class
│   ├── config/                             # Configuration classes
│   │   ├── AlipayConfig.java               # Alipay configuration
│   │   ├── MybatisPlusConfig.java          # MyBatis Plus configuration
│   │   ├── RedisConfig.java                # Redis configuration
│   │   ├── StorageProperties.java          # File storage configuration
│   │   └── WebMvcConfig.java               # Web MVC configuration
│   ├── controller/                         # Controllers
│   │   ├── UserController.java
│   │   ├── LandController.java
│   │   ├── ProductController.java
│   │   ├── OrderController.java
│   │   ├── MsgController.java
│   │   └── AlipayController.java
│   ├── service/                            # Business logic
│   │   ├── impl/
│   │   └── IXXXService.java
│   ├── mapper/                             # Data access layer
│   ├── pojo/                               # Entity classes
│   │   ├── User.java
│   │   ├── Land.java
│   │   ├── Product.java
│   │   ├── Order2.java
│   │   └── ...
│   ├── util/                               # Utility classes
│   │   ├── JwtUtils.java                   # JWT utility
│   │   ├── EmailUtils.java                 # Email utility
│   │   ├── FileUtil.java                   # File utility
│   │   └── OrderClear.java                 # Scheduled task
│   └── Interceptor/
│       └── MyInterceptor.java              # JWT interceptor
├── vue/                                    # Frontend project
│   ├── src/
│   │   ├── views/                          # Page components
│   │   │   ├── frontend/                   # Frontend pages
│   │   │   └── backend/                    # Backend admin pages
│   │   ├── components/                     # Common components
│   │   ├── router/                         # Routing configuration
│   │   ├── request/                        # HTTP request encapsulation
│   │   └── config/                         # Configuration files
│   └── package.json
├── cloudland.sql                           # Database initialization script
├── docker-compose.yml                      # Docker composition
├── Dockerfile                              # Backend Docker image
└── pom.xml                                 # Maven dependency configuration
```

## Quick Start

### Environment Requirements
- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis
- Node.js 16+ (for frontend)

### Backend Configuration

1. Import the database:
```bash
mysql -u root -p < cloudland.sql
```

2. Configure `application.yml`:
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

3. Start the backend:
```bash
./mvnw spring-boot:run
# or
java -jar target/cloudland.jar
```

### Frontend Configuration

1. Install dependencies:
```bash
cd vue
npm install
```

2. Start the development server:
```bash
npm run serve
# Access http://localhost:8080
```

3. Build for production:
```bash
npm run build
```

### Docker Deployment

```bash
# Build and start all services
docker-compose up -d

# Build backend separately
docker build -t cloudland-backend .

# Run backend container
docker run -d -p 9090:9090 --name cloudland-backend cloudland-backend
```

## API Interface Overview

| Module | Endpoint | Description |
|--------|----------|-------------|
| User | `/user/login` | User login |
| User | `/user/register` | User registration |
| User | `/user/code` | Send verification code |
| User | `/user/trolley` | Shopping cart |
| Land | `/land` | Land CRUD |
| Product | `/product` | Product CRUD |
| Order | `/order` | Order CRUD |
| Order | `/order/download` | Export order report |
| Message | `/msg` | Message management |
| Payment | `/alipay/pay` | Alipay payment |
| Payment | `/alipay/notify` | Payment callback |

For detailed API documentation, refer to [API.md](./API.md)

## Core Business Logic

### User Roles (power field)
- `0`: Regular User
- `1`: Staff
- `2`: Admin

### Order Status
- `0`: Pending Payment
- `1`: Paid
- `2`: Cancelled
- `3`: Completed

### Scheduled Task
- **Order Cleanup** (`OrderClear`): Runs daily at midnight to remove expired unpaid orders and restore inventory

### File Storage
The system supports the following file storage paths:
- User avatars: `/files/user/`
- Product images: `/files/product/`
- Land files: `/files/land/{landId}/`
- Land cloud files: `/files/land/{landId}/cloud/`
- Land images: `/files/land/{landId}/images/`

## Development Notes

1. **JWT Authentication**: All endpoints except login, registration, password recovery, and code verification require a valid JWT token.
2. **Payment Integration**: Alipay payment requires registering a sandbox account on the Alipay Open Platform.
3. **Email Configuration**: A valid SMTP mail server must be configured.
4. **File Uploads**: Ensure the upload directories have write permissions.

## License

This project is for learning and communication purposes only.