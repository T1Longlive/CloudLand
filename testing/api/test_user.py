# -*- coding: utf-8 -*-
"""用户模块：注册安全与查重。

核心用例是提权漏洞回归（commit 60b1487 修复）：
注册请求携带 power=2/status=0/debt=999 恶意字段 → 服务端必须强制落库
power=0/status=1/debt=0。此用例固化该安全修复，防止回归。
"""
import allure
import pytest

from common.assertions import (
    PHONE_EXIST,
    REGISTER_OK,
    assert_http,
    assert_success,
)
from conftest import MASTER_CODE, SEED, make_temp_user


@allure.epic("API 自动化层")
@allure.feature("用户模块")
class TestRegisterSecurity:
    """注册接口安全用例。"""

    @allure.story("提权漏洞回归")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("注册携带 power=2 → 服务端强制入库 power=0（提权回归）")
    def test_register_privilege_escalation_blocked(self, admin_client, temp_user_factory):
        """修复前：客户端 power 原样入库，传 power=2 即成管理员（高危）。

        断言三层：注册成功 + 库内字段被强制重置 + 新账号无法访问后台接口。
        """
        user = make_temp_user(1, power=2, status=0, debt=999)  # 恶意字段
        resp = temp_user_factory(user)
        assert_success(resp, REGISTER_OK)

        # 断言入库字段：服务端重置，不信任客户端
        found = admin_client.find_user_by_phone(user["phone"])
        assert found is not None, "注册后应能查到该用户"
        assert found["power"] == 0, f"提权漏洞回归失败: power={found['power']}, 期望强制为 0"
        assert found["status"] == 1, f"status={found['status']}, 期望强制为 1（激活）"
        assert float(found["debt"]) == 0.0, f"debt={found['debt']}, 期望强制为 0"

        # 断言横向越权面：该账号登录后访问后台接口应被拒绝（403 + power 原因）
        from common.client import ApiClient
        from conftest import BASE_URL

        fresh = ApiClient(BASE_URL)
        fresh.login(user["phone"], user["password"], master_code=MASTER_CODE)
        resp = fresh.user_page()
        assert_http(resp, 403)
        assert resp.headers.get("X-Forbidden-Reason") == "power", (
            "power=0 账号访问后台接口应以 power 原因拒绝"
        )

    @allure.story("注册查重")
    @allure.title("手机号已注册 → 返回 PHONE_EXIST(20007)")
    def test_register_duplicate_phone(self, admin_client, temp_user_factory):
        user = make_temp_user(2)
        assert_success(temp_user_factory(user), REGISTER_OK)

        dup = make_temp_user(3, phone=user["phone"])  # 同手机号不同邮箱
        body = assert_success(admin_client.register(dup, master_code=MASTER_CODE), PHONE_EXIST)
        assert "手机号" in body["msg"]

    @allure.story("注册查重")
    @allure.title("邮箱已注册 → 返回 MAIL_EXIST")
    def test_register_duplicate_mail(self, admin_client, temp_user_factory):
        user = make_temp_user(4)
        assert_success(temp_user_factory(user), REGISTER_OK)

        dup = make_temp_user(5, mail=user["mail"])  # 同邮箱不同手机号
        resp = admin_client.register(dup, master_code=MASTER_CODE)
        body = assert_http(resp, 200).json()
        assert body["code"] != REGISTER_OK, "邮箱重复注册不应成功"
        assert "邮箱" in body["msg"], f"应提示邮箱重复, 实际: {body['msg']}"

    @allure.story("注册字段锁定")
    @allure.title("正常注册 → 入库仅基础字段，power/status/debt 取服务端默认值")
    def test_register_normal_defaults(self, admin_client, temp_user_factory):
        """不带任何恶意字段的正常注册，也应落入相同的默认值（白盒对照）。"""
        user = make_temp_user(6)
        assert_success(temp_user_factory(user), REGISTER_OK)

        found = admin_client.find_user_by_phone(user["phone"])
        assert found["power"] == 0 and found["status"] == 1 and float(found["debt"]) == 0.0

    @allure.story("数据隔离")
    @allure.title("seed 账号不受临时账号增删影响")
    def test_seed_accounts_untouched(self, admin_client):
        """134 段用例运行前后，seed 的 199 段账号数据保持不变（防误删护栏）。"""
        for role, info in SEED.items():
            found = admin_client.find_user_by_phone(info["phone"])
            assert found is not None, f"seed {role} 账号 {info['phone']} 不应被删除"
            assert found["power"] == info["power"], (
                f"seed {role} power 被改动: {found['power']} != {info['power']}"
            )
