# -*- coding: utf-8 -*-
"""支付模块：金额服务端重算（防篡改）、交易映射、支付前置校验。

安全契约（对应 AlipayController + Order2ServiceImpl）：
- 金额必须由服务端按 DB 重算（土地=land.price，产品=price×num），客户端 totalAmount 仅作日志
- 交易号 out_trade_no 与 orderIds 的映射写入 Redis（alipay:trade:*，TTL 30 分钟）
- 仅未支付订单可参与支付；客户只能支付自己的订单
- 业务规则违规返回 ADD_ERR 业务码（曾为未捕获 RuntimeException → HTTP 500，已修复）
"""
import html
import json
import re

import allure
import pytest

from common.assertions import ADD_ERR, ADD_OK, UPDATE_OK, assert_success
from conftest import SEED

TRADE_KEY_PREFIX = "alipay:trade:"


def parse_biz_content(pay_body):
    """从支付响应的支付宝表单 HTML 中解析 biz_content JSON。

    返回 dict（含 out_trade_no / total_amount / subject 等）。
    """
    form = pay_body.get("data") or ""
    m = re.search(r'name="biz_content" value="([^"]*)"', form)
    assert m, "支付表单应包含 biz_content 隐藏域"
    return json.loads(html.unescape(m.group(1)))


@allure.epic("API 自动化层")
@allure.feature("支付模块")
class TestPayAmountRecalc:
    """金额防篡改：客户端金额一律被忽略。"""

    @allure.story("金额服务端重算")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("篡改 totalAmount=0.01 → 支付单金额仍为服务端重算值")
    def test_amount_recalculated_server_side(self, customer_client, product_factory):
        product = product_factory(price=10.5, num=10)
        assert_success(customer_client.add_order(product["id"], num=2), ADD_OK)
        order = customer_client.find_order_by_pid(product["id"])

        # 恶意请求：金额改成 0.01 元
        resp = customer_client.pay(order["id"], total_amount="0.01")
        body = assert_success(resp, ADD_OK)

        biz = parse_biz_content(body)
        assert biz["total_amount"] == "21.00", (
            f"支付金额应为服务端重算 10.5×2=21.00, 实际 {biz['total_amount']}"
        )
        assert biz["out_trade_no"].startswith("CLD"), "交易号应为 CLD 前缀"

    @allure.story("交易映射")
    @allure.title("支付成功 → Redis 存 out_trade_no→orderIds 映射（TTL 30 分钟）")
    def test_trade_mapping_saved_to_redis(self, customer_client, product_factory, redis_client):
        product = product_factory(price=8.0, num=10)
        assert_success(customer_client.add_order(product["id"], num=1), ADD_OK)
        order = customer_client.find_order_by_pid(product["id"])

        body = assert_success(customer_client.pay(order["id"]), ADD_OK)
        biz = parse_biz_content(body)

        key = TRADE_KEY_PREFIX + biz["out_trade_no"]
        assert redis_client.exists(key) == 1, f"交易映射应写入 Redis: {key}"
        assert json.loads(redis_client.get(key)) == [order["id"]], "映射值应为订单 ID 数组"
        ttl = redis_client.ttl(key)
        assert 0 < ttl <= 30 * 60, f"映射 TTL 应在 30 分钟内, 实际 {ttl}s"


@allure.epic("API 自动化层")
@allure.feature("支付模块")
class TestPayPreconditions:
    """支付前置校验：业务规则违规返回 ADD_ERR（防 500 回归）。"""

    @allure.story("重复支付防护")
    @allure.title("已支付订单再次支付 → ADD_ERR(10005)")
    def test_paid_order_rejected(self, customer_client, admin_client, product_factory):
        product = product_factory(price=10.5, num=10)
        assert_success(customer_client.add_order(product["id"], num=1), ADD_OK)
        order = customer_client.find_order_by_pid(product["id"])

        # 后台将订单标记为已支付（PUT /order 为 admin 接口，query 参数）
        resp = admin_client.mark_order_paid(order["id"])
        assert_success(resp, UPDATE_OK)

        resp = customer_client.pay(order["id"])
        body = assert_success(resp, ADD_ERR)
        assert "状态" in body["msg"], f"应提示订单状态异常, 实际: {body['msg']}"

    @allure.story("越权防护")
    @allure.title("客户支付他人订单 → ADD_ERR(10005) 归属校验失败")
    def test_pay_others_order_rejected(self, customer_client, employee_client, product_factory):
        product = product_factory(price=10.5, num=10)
        assert_success(
            employee_client.add_order(product["id"], num=1, uid=employee_client.user_id), ADD_OK
        )
        order = employee_client.find_order_by_pid(product["id"], uid=employee_client.user_id)

        resp = customer_client.pay(order["id"])
        body = assert_success(resp, ADD_ERR)
        assert "归属" in body["msg"], f"应提示归属校验失败, 实际: {body['msg']}"

    @allure.story("参数校验")
    @allure.title("支付不存在的订单 → ADD_ERR(10005)")
    def test_nonexistent_order_rejected(self, customer_client):
        resp = customer_client.pay(99999999)
        body = assert_success(resp, ADD_ERR)
        assert "不存在" in body["msg"], f"应提示订单不存在, 实际: {body['msg']}"
