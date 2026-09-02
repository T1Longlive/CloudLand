# -*- coding: utf-8 -*-
"""pytest fixtures：API 客户端 + 种子账号登录态 + 业务数据工厂。

环境变量（与 vue/.env.test 语义一致，可被 CI 注入覆盖）：
- TEST_API_BASE_URL：后端地址，默认 http://localhost:9090/api
- TEST_MASTER_CODE：万能验证码，默认 000000（本地开发默认开启，生产关闭）
- TEST_REDIS_HOST/PORT/PASSWORD：Redis 连接（验证交易映射）

种子账号复用 vue/tests/fixtures/seed.sql 的 199 段体系（密码 Test@123456）。
API 测试临时注册账号使用 134 段专用前缀（避开 E2E 的 131/132 段与 seed 的 199 段）。
"""
import json
import os
import time

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

# 造数用的最小图片字节（仅落盘占位，不校验内容）
TINY_PNG = b"\x89PNG\r\n\x1a\n" + b"\x00" * 32


def _unique(name):
    return f"{name}{int(time.time() * 1000) % 100000000}"


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
    client.user_id = resp.json()["data"]["id"]  # 下单归属/造数 aId 需要
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


@pytest.fixture
def product_factory(admin_client):
    """创建测试产品（multipart 上传，aId=管理员），测试结束自动删除。

    注意：product.a_id 是 NOT NULL 外键 → user.id，必须显式指定；
    save 响应 data=null，需按唯一产品名回查拿 id（分页查询 INNER JOIN user）。
    """
    created_ids = []

    def _create(price=10.5, num=50):
        name = _unique("APIT产品")
        product = {
            "productName": name,
            "description": "API自动化测试产品",
            "price": price,
            "num": num,
            "status": 1,
            "ordered": 1,
            "aId": admin_client.user_id,
        }
        resp = admin_client.session.post(
            f"{BASE_URL}/product",
            headers={"token": admin_client.token},
            data={"product": json.dumps(product, ensure_ascii=False)},
            files={"productImg": ("t.png", TINY_PNG, "image/png")},
        )
        assert resp.status_code == 200 and resp.json().get("code") == 10001, (
            f"创建产品失败: {resp.text[:200]}"
        )
        # 按唯一名回查 id
        found = admin_client.find_product_by_name(name)
        assert found is not None, f"回查产品失败: {name}"
        created_ids.append(found["id"])
        return found

    yield _create

    if created_ids:
        admin_client.delete("/product", json=created_ids)


@pytest.fixture
def land_factory(admin_client):
    """创建测试土地（multipart 上传 landFiles+imageFiles，aId=管理员），自动删除。

    土地订单的查询链路（selectOrder）依赖 CloudLandFile 图片行，
    故 imageFiles 必传；save 响应 data=land 自带自增 id。
    """
    created_ids = []

    def _create(price=66.0):
        land = {
            "landName": _unique("APIT土地"),
            "landType": 1,
            "description": "API自动化测试土地",
            "address": "510000,510100,510101",
            "ordered": 1,
            "price": price,
            "aId": admin_client.user_id,
            "status": 1,
            "area": 100.0,
            "employeeId": admin_client.user_id,  # 代理人必填：selectById 为 INNER JOIN user，缺省查不到行
        }
        resp = admin_client.session.post(
            f"{BASE_URL}/land",
            headers={"token": admin_client.token},
            data={"land": json.dumps(land, ensure_ascii=False)},
            files=[
                ("landFiles", ("doc.zip", TINY_PNG, "application/octet-stream")),
                ("imageFiles", ("img.png", TINY_PNG, "image/png")),
            ],
        )
        assert resp.status_code == 200 and resp.json().get("code") == 10001, (
            f"创建土地失败: {resp.text[:200]}"
        )
        land_id = resp.json()["data"]["id"]
        created_ids.append(land_id)
        return land_id

    yield _create

    if created_ids:
        admin_client.delete("/land", json=created_ids)


@pytest.fixture
def redis_client():
    """Redis 直连（校验支付交易映射；与 E2E helpers/redis.js 同一套环境变量）。"""
    import redis as redis_lib

    client = redis_lib.Redis(
        host=os.environ.get("TEST_REDIS_HOST", "localhost"),
        port=int(os.environ.get("TEST_REDIS_PORT", "6379")),
        password=os.environ.get("TEST_REDIS_PASSWORD", "123456"),
        decode_responses=True,
        protocol=2,  # 本地 Redis 不支持 HELLO（RESP3），强制 RESP2
    )
    yield client
    client.close()


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
