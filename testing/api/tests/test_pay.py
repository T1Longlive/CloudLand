"""支付接口用例（AlipayController /alipay/pay）。

只测 pay：本地签名构造表单，不联网、不碰 notify/return（公网不可达）。
核心叙事：前端传的 totalAmount/userId 一律不被信任——金额服务端重算、
交易号用登录态 id；归属/状态校验拒绝非法支付；交易映射存 Redis 30 分钟。

副作用治理：pay 只写 Redis 映射（不改订单），用例结束删映射键 + 删单回滚库存。
"""
import re

import allure
import pytest

TRADE_KEY_PATTERN = "alipay:trade:*"


def _server_amount(form: str) -> float:
    """从支付宝表单 HTML 里提取服务端写入的 total_amount（bizContent 被 HTML 转义）。"""
    match = re.search(r'total_amount(?:&quot;|"):(?:&quot;|")([0-9.]+)', form)
    assert match, f"表单中找不到 total_amount：{form[:300]}"
    return float(match.group(1))


@allure.feature("支付")
class TestPayAmount:
    @allure.story("金额服务端重算，篡改无效")
    @pytest.mark.p0
    def test_server_recomputes_amount(self, customer, sellable_product, redis_client):
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造未支付订单")
        pid = sellable_product["id"]
        price = float(sellable_product["price"])
        create = customer.order.create(pid, 1)
        assert create.json()["code"] == 10001
        oid = customer.order.my_orders(0).json()["data"][0]["id"]

        before = set(redis_client.keys(TRADE_KEY_PATTERN))
        try:
            with allure.step("篡改金额 0.01 与 userId 发起支付"):
                resp = customer.pay.pay([oid], total_amount="0.01", user_id=999999)
            assert resp.json()["code"] == 10001, f"支付发起失败：{resp.text[:300]}"
            form = resp.json()["data"]

            with allure.step("表单金额等于服务端重算值，而非篡改值"):
                assert _server_amount(form) == pytest.approx(price), (
                    f"支付金额应为服务端重算的 {price}，表单里却是 {_server_amount(form)}"
                )
                if price != 0.01:
                    assert "0.01" not in form, "篡改金额出现在支付表单中"
                assert "999999" not in form, "交易号使用了前端传入的 userId"

            with allure.step("交易映射写入 Redis 且 TTL 30 分钟"):
                new_keys = set(redis_client.keys(TRADE_KEY_PATTERN)) - before
                assert len(new_keys) == 1, f"应新增恰好一个交易映射，实际 {new_keys}"
                ttl = redis_client.ttl(new_keys.pop())
                assert 0 < ttl <= 1800, f"交易映射 TTL 应在 30 分钟内，实际 {ttl}s"
        finally:
            for key in set(redis_client.keys(TRADE_KEY_PATTERN)) - before:
                redis_client.delete(key)
            customer.order.delete(oid)


@allure.feature("支付")
class TestPayAuthorization:
    @allure.story("客户支付他人订单被拒")
    @pytest.mark.p0
    def test_customer_cannot_pay_others_order(
        self, make_temp_account, employee, customer, sellable_product, seed_customer_id
    ):
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造他人未支付订单")
        create = employee.order.create(sellable_product["id"], 1, user_id=seed_customer_id)
        assert create.json()["code"] == 10001, f"员工代录订单失败：{create.text[:300]}"
        oid = customer.order.my_orders(seed_customer_id).json()["data"][0]["id"]
        attacker = make_temp_account()

        try:
            resp = attacker.api.pay.pay([oid])
            assert resp.json()["code"] == 10005, (
                f"客户支付他人订单应被拒（10005），实际 {resp.json()['code']}"
            )
        finally:
            customer.order.delete(oid)

    @allure.story("已支付订单拒绝重复支付")
    @pytest.mark.p0
    def test_paid_order_rejected(self, employee):
        records = employee.order.page(page_size=5).json()["data"]["records"]
        if not records:
            pytest.skip("后台订单页没有已支付/已退单订单，无法验证重复支付拒绝")

        resp = employee.pay.pay([records[0]["id"]])

        assert resp.json()["code"] == 10005, (
            f"已支付订单再支付应被拒（10005），实际 {resp.json()['code']}"
        )

    @allure.story("不存在的订单拒绝支付")
    @pytest.mark.p0
    def test_missing_order_rejected(self, customer):
        resp = customer.pay.pay([99_999_999])

        assert resp.json()["code"] == 10005, (
            f"不存在订单支付应被拒（10005），实际 {resp.json()['code']}"
        )
