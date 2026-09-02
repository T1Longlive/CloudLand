# -*- coding: utf-8 -*-
"""双层断言：HTTP 状态码 + 业务状态码。

两层不可混淆（对应前端 interceptor.js 的重要约定）：
- HTTP 状态码：网络层/认证层（401 未认证、402 权限变更、403 禁用/越权）
- 业务状态码：响应体 code 字段（20005 LOGIN_OK 等，见 src/constants/code.js）
"""
import allure

# 业务码（与后端 Code.java / 前端 code.js 对齐，按需补充）
# ── 通用 CRUD ──
ADD_OK = 10001
DELETE_OK = 10002
UPDATE_OK = 10003
SELECT_OK = 10004
ADD_ERR = 10005
DELETE_ERR = 10006
# ── 认证授权 ──
LOGIN_OK = 20005
REGISTER_OK = 20006
PHONE_EXIST = 20007
POWER_ERR = 20008
PHONE_NO_EXIST = 20001
PASSWORD_ERR = 20002
CODE_ERR = 30003  # 验证码有误


def assert_http(resp, status=200):
    """断言 HTTP 状态码。"""
    assert resp.status_code == status, (
        f"HTTP {resp.status_code} != {status}: {resp.text[:200]}"
    )
    return resp


def assert_code(resp, expected):
    """断言业务状态码（HTTP 层须先 200）。"""
    assert_http(resp, 200)
    body = resp.json()
    assert body.get("code") == expected, (
        f"业务码 {body.get('code')} != {expected}, msg={body.get('msg')}"
    )
    return body


def assert_success(resp, expected):
    """语义化组合：HTTP 200 + 指定业务码，返回响应体 dict。"""
    body = assert_code(resp, expected)
    allure.attach(
        f"{resp.request.method} {resp.request.url}\n-> code={body.get('code')} msg={body.get('msg')}",
        name="api-result",
        attachment_type=allure.attachment_type.JSON,
    )
    return body
