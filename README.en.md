# Cloudland

[![CI](https://github.com/T1Longlive/CloudLand/actions/workflows/ci.yml/badge.svg)](https://github.com/T1Longlive/CloudLand/actions/workflows/ci.yml)
[![Allure Report](https://img.shields.io/badge/Allure-report-6F6FFF)](https://t1longlive.github.io/CloudLand/)
[![E2E Report](https://img.shields.io/badge/Playwright-report-22A5F7)](https://t1longlive.github.io/CloudLand/e2e/)

Cloudland is a cloud-based land leasing and management platform built on Spring Boot + Vue 2, offering users comprehensive features including land browsing, leasing, and ordering.

## Project Overview

Cloudland is a full-stack web application designed to provide users with convenient cloud land resource leasing services. The system supports land information display, product browsing, online ordering, shopping cart management, order management, and message feedback.

## Technology Stack

### Backend
- **Framework**: Spring Boot 2.7.14
- **Language**: Java 8
- **Database**: MySQL
- **Cache**: Redis
- **ORM**: MyBatis-Plus

### Frontend
- **Framework**: Vue 2.6
- **UI**: Bootstrap 5
- **Routing**: Vue Router
- **State Management**: Vuex

## Features

### User Features
- User registration and login
- Password recovery (email verification code)
- Personal information management
- Shopping cart management
- Order inquiry and management

### Land Management
- Land information publishing and display
- Land type categorization
- Land image/file upload
- Pagination query

### Product Center
- Product browsing and search
- Product detail viewing
- Online purchase

### Order System
- Online order placement
- Order status management
- Alipay payment integration
- Report download

### Messaging System
- Message feedback
- Email subscription and notifications

## Project Structure

```
cloudland/
├── src/main/java/com/cloudland/
│   ├── controller/      # Controller layer
│   ├── service/        # Business logic layer
│   ├── mapper/         # Data access layer
│   ├── pojo/           # Entity classes
│   ├── util/           # Utility classes
│   └── config/         # Configuration classes
├── src/main/resources/
│   ├── mapper/          # MyBatis XML mappings
│   └── application.yml # Application configuration
└── vue/                # Frontend project
    ├── src/
    │   ├── views/     # Page components
    │   ├── components/# Common components
    │   ├── router/    # Routing configuration
    │   └── request/   # HTTP requests
    └── public/         # Static resources
```

## Quick Start

### Environment Requirements

- JDK 8+
- Maven 3.6+
- MySQL 5.7+
- Redis
- Node.js 16+

### Backend Configuration

1. Create a database and import the initialization script:
```bash
mysql -u root
```