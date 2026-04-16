# 支付宝沙箱支付集成计划

## 目标
在 zfbpay 分支上为 Cloudland 项目集成支付宝沙箱电脑网站支付。

## 实现步骤

### Step 1: 添加依赖
- `pom.xml` 添加 `alipay-sdk-java` 依赖

### Step 2: 添加配置
- `application.yml` 添加 alipay 配置块（appId, privateKey, publicKey, notifyUrl, returnUrl, gatewayUrl）

### Step 3: 后端 - AlipayConfig.java
- 路径: `src/main/java/com/cloudland/config/AlipayConfig.java`
- 读取配置，初始化 `AlipayClient` Bean

### Step 4: 后端 - AlipayService.java + AlipayServiceImpl.java
- 路径: `src/main/java/com/cloudland/service/`
- `pay(String orderNo, String totalAmount, String subject)` → 返回支付表单 HTML
- `verifyNotify(Map<String, String> params)` → 验签

### Step 5: 后端 - AlipayController.java
- 路径: `src/main/java/com/cloudland/controller/AlipayController.java`
- `POST /alipay/pay` — 接收 orderIds + totalAmount，生成支付表单返回
- `POST /alipay/notify` — 支付宝异步回调，验签后批量更新订单状态

### Step 6: 拦截器白名单
- `WebMvcConfig.java` 的 `excludePathPatterns` 添加 `/alipay/notify`

### Step 7: 前端 - MyOrder.vue 改造
- 弹窗新增"支付宝支付"按钮
- 点击后请求 `/alipay/pay`，获取表单 HTML，新窗口提交跳转

## 关键约定
- 交易号格式: `CLD` + `System.currentTimeMillis()` + userId
- 多订单合并为一笔支付，回调时批量更新
- `/alipay/notify` 不需要 token（加入拦截器白名单）
- 沙箱 gatewayUrl: `https://openapi-sandbox.dl.alipay.com/gateway.do`
