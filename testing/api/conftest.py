# -*- coding: utf-8 -*-
"""pytest fixtures：API 客户端 + 种子账号登录态。

环境变量（与 vue/.env.test 语义一致，可被 CI 注入覆盖）：
- TEST_API_BASE_URL：后端地址，默认 http://localhost:9090/api
- TEST_MASTER_CODE：万能验证码，默认 000000（本地开发默认开启，生产关闭）

种子账号复用 vue/tests/fixtures/seed.sql 的 199 段体系（密码 Test@123456）。
API 测试临时注册账号使用 134 段专用前缀（避开 E2E 的 131/132 段与 seed 的 199 段）。
"""
import os

import pytest

from common.client import ApiClient

BASE_URL = os.environ.get("TEST_API_BASE_URL", "http://localhost:9090/api")
MASTER_CODE = os.environ.get("TEST_MASTER_CODE", "000000")

# seed.sql 账号（与 vue/tests/fixtures/accounts.js 保持同步）
SEED = {
    "customer": {"phone": "19900000001", "password": "Test@123456", "power": 0},
    "employee": {"phone": "19900000002", "password": "Test@123456", "power": 1},
    "admin": {"phone": "19900000003", "password": "Test@123456", "power": 2},
}

TEMP_PREFIX = "134"  # API 测试专用临时号段


@pytest.fixture(scope="session")
def api():
    """未登录的裸客户端。"""
    return ApiClient(BASE_URL)


def _logged_client(account):
    client = ApiClient(BASE_URL)
    resp = client.login(account["phone"], account["password"], master_code=MASTER_CODE)
    assert resp.status_code == 200 and resp.json().get("code") == 20005, (
        f"种子账号登录失败: {account['phone']} -> {resp.text[:200]}"
    )
    return client


@pytest.fixture(scope="session")
def admin_client():
    """管理员（power=2）登录态客户端。"""
    return _logged_client(SEED["admin"])


@pytest.fixture(scope="session")
def customer_client():
    """普通客户（power=0）登录态客户端。"""
    return _logged_client(SEED["customer"])


@pytest.fixture(scope="session")
def employee_client():
    """员工（power=1）登录态客户端。"""
    return _logged_client(SEED["employee"])


@pytest.fixture
def temp_user_factory(admin_client):
    """注册 134 段临时账号，测试结束后自动清理。返回注册函数。"""
    created = []

    def _register(user: dict):
        resp = admin_client.register(user, master_code=MASTER_CODE)
        created.append(user["phone"])
        return resp

    yield _register

    for phone in created:
        found = admin_client.find_user_by_phone(phone)
        if found:
            admin_client.delete_user(found["id"])


def make_temp_user(seq: int, **overrides) -> dict:
    """构造 134 段临时用户数据（字段与前端真实注册流程一致）。"""
    user = {
        "username": f"api测试{seq:03d}",
        "phone": f"134{seq:08d}",
        "password": "Api@Test123",
        "age": 25,
        "mail": f"134{seq:08d}@qq.com",
        "address": "四川省,成都市,锦江区",
        "detailedAddress": "API自动化测试地址",
    }
    user.update(overrides)
    return user
