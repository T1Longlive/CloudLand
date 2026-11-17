"""支付宝异步通知（POST /alipay/notify）用例。

为什么这条链路必须有自动化
--------------------------
它是全站**唯一免 JWT 鉴权**的写接口：请求来自支付宝服务器，不可能带我们的 token
（免认证白名单见 WebMvcConfig.addInterceptors）。它的安全性 100% 落在 RSA2 验签上，
而验签这东西的失效方式极其安静——待签串排序错了、空值没剔除、公钥配错、
状态判断写反，都不会报异常，只会让订单永远停在"未支付"。
线上表现就是用户付了钱、后台还没收到钱。

覆盖四件事：
1. 合法签名 + TRADE_SUCCESS → 订单翻转、Redis 交易映射被消费；
2. 同一条通知重复投递 → 仍返回 success，但状态不二次变更（支付宝本来就会重试，必须幂等）；
3. 签名后篡改任一字段 → 验签失败，订单不动（防伪造通知白嫖订单）；
4. trade_status 不是 TRADE_SUCCESS → 不置为已支付。

投递一律用裸客户端（anon，不带 token）：带上 token 测的就不是真实条件了。
"""
import os

import allure
import pytest

from common.alipay_signer import build_notify_params, sign_params, signing_pair_usable

TRADE_KEY_PREFIX = "alipay:trade:"
TRADE_KEY_PATTERN = TRADE_KEY_PREFIX + "*"


def _require_signer():
    """回调用例的前置：本地私钥必须能与后端配置的公钥配对。

    CI 里由 ci.yml 现场生成一对（必然配对）；本地 .env 是真实沙箱凭据
    （应用私钥 + 支付宝平台公钥，本来就不是一对），所以本地 skip 是正常的。

    但 CI 里缺密钥**不能 skip**：那等于回调覆盖被静默阉割，比红更危险。
    """
    if signing_pair_usable():
        return
    if os.environ.get("CI"):
        pytest.fail(
            "CI 未注入可配对的 ALIPAY_PRIVATE_KEY / ALIPAY_PUBLIC_KEY，"
            "notify 用例被跳过等于这条链路没有覆盖，按失败处理"
        )
    pytest.skip(
        "本地 ALIPAY_PRIVATE_KEY / ALIPAY_PUBLIC_KEY 不是一对（应用私钥 + 支付宝平台公钥），"
        "无法自签出后端能验过的报文；CI 用现场生成的一对密钥跑这条用例"
    )


def _order_of(customer, order_id):
    """取自己的订单 VO（前台接口按登录态过滤，body 里的 id 会被覆盖）。"""
    records = customer.order.my_orders(0).json()["data"]
    hit = next((r for r in records if r["id"] == order_id), None)
    assert hit is not None, f"订单 {order_id} 不在当前用户的订单列表里"
    return hit


def _create_unpaid_order_with_trade_mapping(customer, product, redis_client):
    """造一条未支付订单并发起支付，返回 (order_id, out_trade_no)。

    为什么不直接把映射写进 Redis：映射由 `saveTradeMapping` 写入，里面有归属校验。
    手工塞 Redis 等于绕过被测代码，测出来的"通"没有意义。
    """
    before = set(redis_client.keys(TRADE_KEY_PATTERN))

    create = customer.order.create(product["id"], 1)
    assert create.json()["code"] == 10001, f"下单失败：{create.text[:300]}"
    latest = customer.order.my_orders(0).json()["data"][0]
    order_id = latest["id"]
    assert latest["status"] == 0, f"新建订单应为未支付，实际 status={latest['status']}"

    pay = customer.pay.pay([order_id])
    assert pay.json()["code"] == 10001, f"发起支付失败：{pay.text[:300]}"

    new_keys = set(redis_client.keys(TRADE_KEY_PATTERN)) - before
    assert len(new_keys) == 1, f"应新增恰好一个交易映射，实际 {new_keys}"
    out_trade_no = new_keys.pop()[len(TRADE_KEY_PREFIX):]
    return order_id, out_trade_no


@allure.feature("支付回调")
class TestAlipayNotify:
    @allure.story("合法回调置为已支付，且重复投递幂等")
    @pytest.mark.p0
    def test_notify_marks_order_paid_and_is_idempotent(
        self, customer, anon, sellable_product, redis_client
    ):
        _require_signer()
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造未支付订单")

        before = set(redis_client.keys(TRADE_KEY_PATTERN))
        order_id = None
        try:
            order_id, out_trade_no = _create_unpaid_order_with_trade_mapping(
                customer, sellable_product, redis_client
            )
            params = sign_params(
                build_notify_params(out_trade_no, total_amount=str(sellable_product["price"]))
            )

            with allure.step("裸客户端投递（不带 token）→ 返回 success"):
                first = anon.pay.notify(params)
                assert first.status_code == 200, (
                    f"回调应免认证可访问，实际 HTTP {first.status_code}：{first.text[:200]}"
                )
                assert first.text.strip() == "success", (
                    f"合法回调应返回 success（支付宝只认这个字符串），实际 {first.text[:200]!r}"
                )

            with allure.step("订单翻转为已支付，支付时间落库"):
                paid = _order_of(customer, order_id)
                assert paid["status"] == 1, f"订单应变为已支付（status=1），实际 {paid['status']}"
                assert paid["payTime"], "支付时间未写入"

            with allure.step("交易映射被消费，不能重复使用"):
                assert not redis_client.exists(TRADE_KEY_PREFIX + out_trade_no), (
                    "回调成功后应删除交易映射，否则同一笔交易号可被反复使用"
                )

            with allure.step("同一条通知再投一次：仍返回 success，状态不二次变更"):
                # 支付宝对未收到 success 的通知会重试，网络抖动也会重投，
                # 所以"重复投递"是常态而不是异常。这里断言的是幂等，不是"拒绝"。
                second = anon.pay.notify(params)
                assert second.text.strip() == "success", (
                    f"重复通知也应返回 success（否则支付宝会一直重试），实际 {second.text[:200]!r}"
                )
                again = _order_of(customer, order_id)
                assert again["status"] == 1
                assert again["payTime"] == paid["payTime"], "重复通知改动了支付时间，回调不幂等"
        finally:
            for key in set(redis_client.keys(TRADE_KEY_PATTERN)) - before:
                redis_client.delete(key)
            if order_id is not None:
                customer.order.delete(order_id)

    @allure.story("签名后被篡改的通知被拒绝")
    @pytest.mark.p0
    def test_notify_rejects_tampered_payload(
        self, customer, anon, sellable_product, redis_client
    ):
        _require_signer()
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造未支付订单")

        before = set(redis_client.keys(TRADE_KEY_PATTERN))
        order_id = None
        try:
            order_id, out_trade_no = _create_unpaid_order_with_trade_mapping(
                customer, sellable_product, redis_client
            )
            # 关键动作：先签，再改。改完签名就对不上了——这是"伪造通知"的最小形态。
            signed = build_notify_params(
                out_trade_no, total_amount=str(sellable_product["price"])
            )
            tampered = sign_params(signed)
            tampered["total_amount"] = "0.01"
            # 防呆：如果签名的原值本来就是 0.01，"篡改"就成了空操作，
            # 用例会静默退化成"再投一次合法通知"。这条断言就是为了让那种情况不成立。
            assert tampered["total_amount"] != signed["total_amount"], (
                "篡改值与原值相同，用例没有真正构造出被改过的报文"
            )

            resp = anon.pay.notify(tampered)

            assert resp.text.strip() == "failure", (
                f"篡改后的回调必须验签失败，实际返回 {resp.text[:200]!r}"
            )
            assert _order_of(customer, order_id)["status"] == 0, "验签失败却把订单改成了已支付"
            assert redis_client.exists(TRADE_KEY_PREFIX + out_trade_no), (
                "验签失败不应消费交易映射，否则用户付了款也无法再回调"
            )
        finally:
            for key in set(redis_client.keys(TRADE_KEY_PATTERN)) - before:
                redis_client.delete(key)
            if order_id is not None:
                customer.order.delete(order_id)

    @allure.story("非 TRADE_SUCCESS 通知不置为已支付")
    @pytest.mark.p1
    def test_notify_ignores_non_success_status(
        self, customer, anon, sellable_product, redis_client
    ):
        _require_signer()
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造未支付订单")

        before = set(redis_client.keys(TRADE_KEY_PATTERN))
        order_id = None
        try:
            order_id, out_trade_no = _create_unpaid_order_with_trade_mapping(
                customer, sellable_product, redis_client
            )
            # 签名合法、状态还没成功：这才是"提前到货"的通知，必须被忽略而不是被信任。
            params = sign_params(
                build_notify_params(out_trade_no, trade_status="WAIT_BUYER_PAY")
            )

            resp = anon.pay.notify(params)

            assert resp.text.strip() == "failure", (
                f"非 TRADE_SUCCESS 应返回 failure，实际 {resp.text[:200]!r}"
            )
            assert _order_of(customer, order_id)["status"] == 0, "未成功交易把订单标成了已支付"
        finally:
            for key in set(redis_client.keys(TRADE_KEY_PATTERN)) - before:
                redis_client.delete(key)
            if order_id is not None:
                customer.order.delete(order_id)
