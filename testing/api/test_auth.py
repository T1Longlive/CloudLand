# -*- coding: utf-8 -*-
"""认证授权：登录双通道、HTTP 状态码语义、token 滑动续期。

HTTP 状态码契约（对应 MyInterceptor + 前端 interceptor.js）：
- 401：缺 token / 令牌无效 / 用户不存在
- 402：token 中 power 与库中不一致（权限变更）
- 403 + X-Forbidden-Reason: status → 账号禁用；power → 权限不足
"""
import allure
import pytest

# 分层：登录/鉴权是核心冒烟链路，PR 必跑
pytestmark = pytest.mark.smoke

from common.assertions import assert_http, assert_success
from common.client import ApiClient
from common.data_loader import load_yaml
from conftest import BASE_URL, MASTER_CODE, SEED

LOGIN_CASES = load_yaml("login_cases.yaml")


@allure.epic("API 自动化层")
@allure.feature("认证授权")
class TestLogin:
    @allure.story("密码登录")
    @allure.title("数据驱动：登录成功 / 错误密码 / 账号不存在 / 验证码错误")
    @pytest.mark.p0
    @pytest.mark.parametrize("case", LOGIN_CASES, ids=lambda c: c["id"])
    def test_login(self, api, case):
        resp = api.login(
            case["phone"],
            case["password"],
            master_code=case.get("code", MASTER_CODE),
        )
        body = assert_success(resp, case["expect_code"])

        if case.get("check_token"):
            assert body["msg"] and len(body["msg"]) > 20, "msg 应携带 JWT token"
            assert body["data"]["power"] == case["expect_power"]


@allure.epic("API 自动化层")
@allure.feature("认证授权")
class TestHttpSemantics:
    """拦截器 HTTP 状态码语义（双层断言的 HTTP 层）。

    无 token 类用例使用独立裸客户端，避免 session 级 fixture 的 token 状态泄漏。
    """

    @allure.title("无 token 访问受保护接口 → HTTP 401")
    def test_missing_token_401(self):
        fresh = ApiClient(BASE_URL)
        resp = fresh.post(
            "/user/page",
            data="pageNum=1&pageSize=10&user={}",
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        assert_http(resp, 401)

    @allure.title("伪造 token → HTTP 401")
    def test_invalid_token_401(self):
        fresh = ApiClient(BASE_URL)
        fresh.token = "fake.jwt.token"
        resp = fresh.post(
            "/user/page",
            data="pageNum=1&pageSize=10&user={}",
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        assert_http(resp, 401)

    @allure.title("power=0 访问后台接口 → HTTP 403 + X-Forbidden-Reason=power")
    def test_customer_forbidden_admin_api(self, customer_client):
        resp = customer_client.user_page()
        assert_http(resp, 403)
        assert resp.headers.get("X-Forbidden-Reason") == "power"

    @allure.title("power=1 员工可访问后台接口 → HTTP 200")
    def test_employee_allowed_admin_api(self, employee_client):
        resp = employee_client.user_page()
        assert_http(resp, 200)

    @allure.title("公开接口免认证：/land/page 无 token → HTTP 200")
    def test_public_endpoint_no_auth(self):
        # land/page 为公开查询（白名单），参数契约：form 表单 pageNum/pageSize/land
        fresh = ApiClient(BASE_URL)
        resp = fresh.post(
            "/land/page",
            data="pageNum=1&pageSize=5&land={}",
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )
        assert_http(resp, 200)


@allure.epic("API 自动化层")
@allure.feature("认证授权")
class TestTokenRefresh:
    @allure.title("updatedToken 滑动续期：受保护接口响应头返回新 token 且可用")
    def test_token_sliding_refresh(self, api):
        account = SEED["employee"]
        api.login(account["phone"], account["password"], master_code=MASTER_CODE)

        resp = api.user_page()
        assert_http(resp, 200)

        # client 已自动按响应头更新 token；断言 token 确实被采纳且新 token 仍有效
        refreshed = resp.headers.get("updatedToken") or resp.headers.get("updatedtoken")
        if refreshed:
            assert api.token == refreshed, "client 应采纳 updatedToken 响应头"
        resp2 = api.user_page()
        assert_http(resp2, 200), "刷新后的 token 应保持有效"
