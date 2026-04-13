# Cloudland API 接口文档

## 1. 概述

### 基础信息
- **Base URL**: `http://localhost:9090`
- **后端端口**: 9090
- **前端端口**: 8080
- **数据格式**: JSON
- **字符编码**: UTF-8

### 认证方式
- **认证类型**: JWT Token
- **请求头字段**: `token`（注意：不是 `Authorization`）
- **Token 内容**: 包含 `id`（用户ID）和 `power`（权限级别）

### 统一响应格式
所有接口返回统一的 Result 对象：

```json
{
  "data": {},           // 响应数据，类型根据接口而定
  "code": 10004,        // 业务状态码（使用10000+范围）
  "msg": "查询成功"      // 消息说明
}
```

### 免认证接口
以下接口无需提供 token：
- `/user/login` - 用户登录
- `/user/register` - 用户注册
- `/user/code` - 发送验证码
- `/user/forgetPassword` - 忘记密码
- `/land/page` - 土地分页查询（公开）
- `/land/{id}` - 土地详情（公开）
- `/product/page` - 产品分页查询（公开）
- `/product/{id}` - 产品详情（公开）
- `/msg` - 提交留言（公开）
- `/msg/mail` - 邮件订阅（公开）
- `/resource/**` - 静态资源访问

---

## 2. 状态码说明

**重要提示**：本项目使用 10000+ 范围的业务状态码，避免与 HTTP 标准状态码（100-599）冲突。

### 通用状态码
| 状态码 | 常量名 | 说明 |
|--------|--------|------|
| 0 | SUCCESS | 通用成功标识 |
| -1 | FAILURE | 通用失败标识 |

### CRUD 操作状态码（10000-10999）
| 状态码 | 常量名 | 说明 |
|--------|--------|------|
| 10001 | ADD_OK | 添加成功 |
| 10002 | DELETE_OK | 删除成功 |
| 10003 | UPDATE_OK | 修改成功 |
| 10004 | SELECT_OK | 查询成功 |
| 10005 | ADD_ERR | 添加失败 |
| 10006 | DELETE_ERR | 删除失败 |
| 10007 | UPDATE_ERR | 修改失败 |
| 10008 | SELECT_ERR | 查询失败 |

### 认证授权状态码（20000-20999）
| 状态码 | 常量名 | 说明 |
|--------|--------|------|
| 20001 | PHONE_NO_EXIST | 账号不存在 |
| 20002 | PASSWORD_ERR | 密码错误 |
| 20003 | TOKEN_ERR | 令牌过期 |
| 20004 | STATUS_ERR | 账号被禁用 |
| 20005 | LOGIN_OK | 登录成功 |
| 20006 | REGISTER_OK | 注册成功 |
| 20007 | PHONE_EXIST | 该手机号已注册 |
| 20008 | POWER_ERR | 权限不足 |
| 20009 | LOGIN_RETURN | 请重新登陆 |
| 20010 | PASSWORD_SAME | 密码相同 |
| 20011 | UPDATE_SAME | 绑定信息相同 |

### 邮件通知状态码（30000-30999）
| 状态码 | 常量名 | 说明 |
|--------|--------|------|
| 30001 | SEND_MAIL_OK | 邮件发送成功 |
| 30002 | SEND_MAIL_ERR | 邮件发送失败 |
| 30003 | CODE_ERR | 验证码错误 |
| 30004 | MAIL_EXIST | 该邮箱已注册 |

---

## 3. 数据模型

### User（用户）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 用户ID（主键） |
| username | String | 用户名 |
| password | String | 密码（BCrypt加密） |
| age | Integer | 年龄 |
| phone | String | 手机号（唯一） |
| mail | String | 邮箱（唯一） |
| address | String | 地址（省市区代码） |
| detailedAddress | String | 详细地址 |
| img | String | 头像路径 |
| power | Integer | 权限（0=客户，1=员工，2=管理员） |
| status | Integer | 状态（0=禁用，1=激活） |
| debt | Double | 欠款 |

### Land（土地）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 土地ID（主键） |
| landName | String | 土地名称 |
| landType | Integer | 土地类型（关联land_type表） |
| description | String | 描述 |
| address | String | 地址（省市区代码） |
| detailedAddress | String | 详细地址 |
| ordered | Integer | 排序权重 |
| price | Double | 价格（元/平方米/天） |
| area | Double | 面积（平方米） |
| aId | Integer | 所有者ID |
| employeeId | Integer | 代理人ID |
| status | Integer | 状态（0=可用，1=已租用） |

### Product（产品）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 产品ID（主键） |
| productName | String | 产品名称 |
| description | String | 描述 |
| ordered | Integer | 排序权重 |
| price | Double | 价格 |
| num | Double | 库存数量 |
| status | Integer | 状态 |
| aId | Integer | 所有者ID |
| img | String | 图片路径 |

### Order2（订单）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 订单ID（主键） |
| pId | Integer | 产品ID |
| uId | Integer | 用户ID |
| num | Integer | 数量 |
| createTime | LocalDateTime | 创建时间 |
| payTime | LocalDateTime | 支付时间 |
| status | Integer | 状态 |
| del | Integer | 删除标记 |

### Msg（留言）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 留言ID（主键） |
| phone | String | 手机号 |
| content | String | 留言内容 |
| sendTime | LocalDateTime | 发送时间 |

### MsgSend（邮件订阅）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 订阅ID（主键） |
| mail | String | 邮箱地址 |

---

## 4. 接口详情

### 4.1 用户接口 (/user)

#### 4.1.1 用户登录
- **接口**: `POST /user/login`
- **需要认证**: ❌ 否
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "phone": "18140213287",
  "password": "123456"
}
```

**响应示例**:
```json
{
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 32,
      "username": "左厚博",
      "power": 2,
      ...
    }
  },
  "code": 20005,
  "msg": "登录成功"
}
```

#### 4.1.2 用户注册
- **接口**: `POST /user/register`
- **需要认证**: ❌ 否
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userIcon | File | 否 | 用户头像文件 |
| user | String | 是 | User对象的JSON字符串 |

**user JSON 示例**:
```json
{
  "username": "张三",
  "password": "123456",
  "age": 25,
  "phone": "13800138000",
  "mail": "zhangsan@example.com",
  "address": "510000,510100,510101",
  "detailedAddress": "某某街道123号"
}
```

**响应示例**:
```json
{
  "data": null,
  "code": 20006,
  "msg": "注册成功"
}
```

#### 4.1.3 删除用户
- **接口**: `DELETE /user`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "data": null,
  "code": 10002,
  "msg": "删除成功"
}
```

#### 4.1.4 更新用户信息
- **接口**: `PUT /user`
- **需要认证**: ✅ 是
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userIcon | File | 否 | 新的用户头像 |
| user | String | 是 | User对象的JSON字符串 |

**响应示例**:
```json
{
  "data": null,
  "code": 10003,
  "msg": "修改成功"
}
```

#### 4.1.5 分页查询用户
- **接口**: `POST /user/page`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 5 | 每页大小 |
| user | String | 是 | - | 查询条件（JSON字符串） |

**响应示例**:
```json
{
  "data": {
    "records": [...],
    "total": 100,
    "size": 5,
    "current": 1,
    "pages": 20
  },
  "code": 10004,
  "msg": "查询成功"
}
```

#### 4.1.6 查询所有员工
- **接口**: `POST /user/employee`
- **需要认证**: ✅ 是

**响应示例**:
```json
{
  "data": [
    {"id": 26, "username": "左厚博"},
    {"id": 29, "username": "东邪"}
  ],
  "code": 408,
  "msg": "success"
}
```

#### 4.1.7 根据ID查询用户
- **接口**: `POST /user/id`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
32
```

**响应示例**:
```json
{
  "data": {
    "id": 32,
    "username": "左厚博",
    "password": null,
    ...
  },
  "code": 10004,
  "msg": "查询成功"
}
```

#### 4.1.8 发送验证码
- **接口**: `POST /user/code`
- **需要认证**: ❌ 否
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "mail": "676104035@qq.com"
}
```

**响应示例**:
```json
{
  "data": null,
  "code": 30001,
  "msg": "邮件发送成功"
}
```

#### 4.1.9 忘记密码
- **接口**: `POST /user/forgetPassword`
- **需要认证**: ❌ 否

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| path | String | 是 | 验证码 |
| user | String | 是 | User对象的JSON字符串（包含mail和新密码） |

**响应示例**:
```json
{
  "data": null,
  "code": 10003,
  "msg": "修改成功"
}
```

#### 4.1.10 查询购物车
- **接口**: `POST /user/trolley`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "id": 32
}
```

**响应示例**:
```json
{
  "data": [
    {
      "id": 1,
      "pId": 10,
      "productName": "有机肥料",
      "price": 50.0,
      "num": 2,
      ...
    }
  ],
  "code": 404,
  "msg": "查询成功"
}
```

#### 4.1.11 添加到购物车
- **接口**: `POST /user/addTrolley`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "pId": 10,
  "uId": 32,
  "num": 2
}
```

#### 4.1.12 删除购物车项
- **接口**: `DELETE /user/trolley/{id}`
- **需要认证**: ✅ 是

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Integer | 购物车项ID |

---

### 4.2 土地接口 (/land)

#### 4.2.1 根据ID查询土地
- **接口**: `GET /land/{id}`
- **需要认证**: ✅ 是

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Integer | 土地ID |

**响应示例**:
```json
{
  "data": {
    "id": 213,
    "landName": "测试数据1",
    "landType": 1,
    "typeName": "农用地",
    "description": "该地处于温带",
    "address": "510000,510700,510722",
    "detailedAddress": "大同",
    "price": 23.0,
    "area": 100.0,
    "status": 0,
    "aId": 32,
    "employeeId": 26,
    "landFiles": "云用地_213609674.zip",
    "imageFiles": ["Img_213_0.jpg", "Img_213_1.jpg"]
  },
  "code": 10004,
  "msg": "查询成功"
}
```

#### 4.2.2 添加土地
- **接口**: `POST /land`
- **需要认证**: ✅ 是
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| landFiles | File[] | 是 | 土地资料文件（支持多个） |
| imageFiles | File[] | 是 | 土地图片文件（支持多个） |
| land | String | 是 | Land对象的JSON字符串 |

**land JSON 示例**:
```json
{
  "landName": "优质农田",
  "landType": 1,
  "description": "土壤肥沃，灌溉便利",
  "address": "510000,510100,510101",
  "detailedAddress": "某某镇某某村",
  "price": 25.0,
  "area": 150.0,
  "aId": 32,
  "employeeId": 26,
  "status": 0,
  "ordered": 100
}
```

#### 4.2.3 删除土地
- **接口**: `DELETE /land`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
[213, 214, 215]
```

#### 4.2.4 更新土地
- **接口**: `PUT /land`
- **需要认证**: ✅ 是
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| landFiles | File[] | 否 | 新的土地资料文件 |
| imageFiles | File[] | 否 | 新的土地图片文件 |
| land | String | 是 | Land对象的JSON字符串（必须包含id） |

#### 4.2.5 分页查询土地
- **接口**: `POST /land/page`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 5 | 每页大小 |
| land | String | 是 | - | 查询条件（JSON字符串） |

---

### 4.3 产品接口 (/product)

#### 4.3.1 根据ID查询产品
- **接口**: `GET /product/{id}`
- **需要认证**: ✅ 是

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Integer | 产品ID |

**响应示例**:
```json
{
  "data": {
    "id": 10,
    "productName": "有机肥料",
    "description": "纯天然有机肥",
    "price": 50.0,
    "num": 1000.0,
    "status": 0,
    "img": "product_10.jpg",
    "aId": 32,
    "customerAUsername": "左厚博",
    "customerAPhone": "18140213287"
  },
  "code": 10004,
  "msg": "查询成功"
}
```

#### 4.3.2 添加产品
- **接口**: `POST /product`
- **需要认证**: ✅ 是
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productImg | File | 是 | 产品图片 |
| product | String | 是 | Product对象的JSON字符串 |

**product JSON 示例**:
```json
{
  "productName": "有机肥料",
  "description": "纯天然有机肥",
  "price": 50.0,
  "num": 1000.0,
  "aId": 32,
  "status": 0,
  "ordered": 100
}
```

#### 4.3.3 删除产品
- **接口**: `DELETE /product`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
[10, 11, 12]
```

#### 4.3.4 更新产品
- **接口**: `PUT /product`
- **需要认证**: ✅ 是
- **Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| productImg | File | 否 | 新的产品图片 |
| product | String | 是 | Product对象的JSON字符串（必须包含id） |

#### 4.3.5 分页查询产品
- **接口**: `POST /product/page`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 5 | 每页大小 |
| product | String | 是 | - | 查询条件（JSON字符串） |

---

### 4.4 订单接口 (/order)

#### 4.4.1 查询订单
- **接口**: `POST /order/order`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "id": 32
}
```

#### 4.4.2 添加订单
- **接口**: `POST /order`
- **需要认证**: ✅ 是
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "pId": 10,
  "uId": 32,
  "num": 5
}
```

#### 4.4.3 更新订单状态
- **接口**: `PUT /order`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | Integer[] | 是 | 订单ID数组 |
| status | Integer | 是 | 新状态值 |

#### 4.4.4 删除订单
- **接口**: `DELETE /order/{id}`
- **需要认证**: ✅ 是

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Integer | 订单ID |

#### 4.4.5 分页查询订单
- **接口**: `POST /order/page`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 5 | 每页大小 |
| order | String | 是 | - | 查询条件（JSON字符串） |

#### 4.4.6 下载订单报表
- **接口**: `POST /order/download`
- **需要认证**: ✅ 是
- **响应类型**: Excel文件流

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| order | String | 是 | 查询条件（JSON字符串） |

**说明**: 该接口直接返回Excel文件流，用于下载订单报表。

---

### 4.5 消息接口 (/msg)

#### 4.5.1 提交留言
- **接口**: `POST /msg`
- **需要认证**: ❌ 否
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "phone": "18140213287",
  "content": "这是一条留言内容"
}
```

**响应示例**:
```json
{
  "data": {...},
  "code": 10001,
  "msg": "发送成功!"
}
```

**业务规则**: 每个手机号最多提交10条留言。

#### 4.5.2 邮件订阅
- **接口**: `POST /msg/mail`
- **需要认证**: ❌ 否
- **Content-Type**: `application/json`

**请求参数**:
```json
{
  "mail": "example@qq.com"
}
```

**响应示例**:
```json
{
  "data": null,
  "code": 10001,
  "msg": "订阅成功!"
}
```

#### 4.5.3 分页查询留言
- **接口**: `POST /msg/page`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | Integer | 否 | 1 | 页码 |
| pageSize | Integer | 否 | 5 | 每页大小 |
| msg | String | 是 | - | 查询条件（JSON字符串） |

#### 4.5.4 推送消息
- **接口**: `POST /msg/push`
- **需要认证**: ✅ 是

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pushMsg | String | 是 | 推送消息内容 |

**响应示例**:
```json
{
  "data": null,
  "code": 30001,
  "msg": "推送成功"
}
```

**说明**: 该接口会向所有订阅用户的邮箱发送消息。

---

## 5. 附录

### 5.1 文件上传说明

#### 支持的文件类型
- **用户头像**: 图片格式（jpg, png, gif等）
- **土地资料**: 压缩包（zip）
- **土地图片**: 图片格式
- **产品图片**: 图片格式

#### 文件大小限制
- 单个文件最大: 60MB
- 单次请求最大: 60MB

#### 文件存储路径
- 用户头像: `{FILE_STORAGE_ROOT}/UserIcon/`
- 土地文件: `{FILE_STORAGE_ROOT}/LandFile/`
- 产品文件: `{FILE_STORAGE_ROOT}/Product/`

#### 文件访问URL
- 用户头像: `/resource/userFile/{filename}`
- 土地文件: `/resource/landFile/{filename}`
- 产品文件: `/resource/productFile/{filename}`

### 5.2 分页参数说明

所有分页接口都支持以下参数：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| pageNum | Integer | 1 | 当前页码（从1开始） |
| pageSize | Integer | 5 | 每页记录数 |

**分页响应格式**:
```json
{
  "records": [],      // 当前页数据
  "total": 100,       // 总记录数
  "size": 5,          // 每页大小
  "current": 1,       // 当前页
  "pages": 20         // 总页数
}
```

### 5.3 地址编码说明

地址字段使用省市区代码，格式为：`省代码,市代码,区代码`

示例：
- `510000,510100,510101` - 四川省成都市锦江区
- `110000,110100,110101` - 北京市市辖区东城区

### 5.4 常见错误处理

#### 401 Unauthorized
- **原因**: 未提供token或token无效
- **解决**: 检查请求头中的token字段

#### 402 Payment Required
- **原因**: 用户权限已变更
- **解决**: 重新登录获取新token

#### 403 Forbidden
- **原因**: 账号被禁用
- **解决**: 联系管理员解除禁用

#### 500 Internal Server Error
- **原因**: 服务器内部错误
- **解决**: 查看服务器日志，检查请求参数格式

### 5.5 开发建议

1. **Token管理**: 建议在响应拦截器中检查 `updatedToken` 响应头，如果存在则更新本地token
2. **错误处理**: 统一处理响应中的code字段，根据不同状态码给出相应提示
3. **文件上传**: 使用FormData对象，注意JSON字符串字段的序列化
4. **分页查询**: 查询条件对象中的空字段会被忽略，可用于动态查询

---

**文档版本**: v1.0
**最后更新**: 2026-04-07
**维护者**: Cloudland开发团队
