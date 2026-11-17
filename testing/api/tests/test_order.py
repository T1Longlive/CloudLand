"""订单接口用例（OrderController）。

订单是"越权"风险最高的模块：查询入参是 `@RequestBody User`，userId 由调用方给。
后端在 Service 层用 currentUserId() 强制覆盖，所以这里重点验证这个覆盖是否真的生效。

为什么没测"下单"：
POST /order 会扣减产品库存（product.num）、把土地状态置为已售（land.status=0），
是**不可逆的写副作用**。在共享的本地 dev 库上跑，会让后面的用例和手工验证都对不上。
正确做法是给这类用例准备独立数据集（Stage 4 的数据治理），而不是在现有库上边测边改。
"""
import allure
import pytest


@allure.feature("订单")
class TestMyOrders:
    @allure.story("我的订单返回列表")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_my_orders_returns_list(self, customer):
        resp = customer.order.my_orders(692)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004
        assert isinstance(body["data"], list), (
            f"我的订单应返回数组，实际 {type(body['data']).__name__}：{resp.text[:200]}"
        )

    @allure.story("客户传他人 id 无效")
    @pytest.mark.p0
    def test_customer_id_is_overridden_by_login_state(self, customer):
        """客户无论传谁的 id，拿到的都必须是自己那份订单。

        不变量（恒等式）而不是具体条数：这库里历史订单引用的产品大多已删除，
        Service 层会 `continue` 跳过，导致返回空数组。
        所以这里断言"三次请求结果完全一致"，而不是断言"有多少条"——
        数据结构变了它不会误报，但一旦后端开始信任请求体里的 id，立刻会红。
        """
        own = customer.order.my_orders(692).json()["data"]
        admin_id = customer.order.my_orders(694).json()["data"]
        not_exist = customer.order.my_orders(99_999_999).json()["data"]

        assert own == admin_id, (
            "客户传管理员 id 后返回的订单与自己的不同，说明请求体里的 userId 被信任了（越权）"
        )
        assert own == not_exist, (
            "客户传不存在的 userId 后返回结果发生变化，说明 userId 未被登录态覆盖"
        )

    @allure.story("员工可按任意 userId 查询")
    @pytest.mark.p1
    def test_staff_can_query_any_user(self, employee):
        """员工/管理员是客服角色，需要按用户查订单（后台订单页），因此不被覆盖。"""
        resp = employee.order.my_orders(692)
        assert resp.status_code == 200
        assert resp.json()["code"] == 10004
        assert isinstance(resp.json()["data"], list)


@allure.feature("订单")
class TestOrderPage:
    @allure.story("后台订单分页")
    @pytest.mark.p0
    def test_order_page_structure(self, employee):
        resp = employee.order.page(page_num=1, page_size=5)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004

        page = body["data"]
        for field in ("records", "total", "size", "current"):
            assert field in page, f"订单分页响应缺少字段 {field}"
        assert len(page["records"]) <= 5

    @allure.story("后台订单分页 pageSize 生效")
    @pytest.mark.p1
    def test_order_page_size_respected(self, employee):
        page = employee.order.page(page_num=1, page_size=2).json()["data"]
        assert page["size"] == 2
        assert len(page["records"]) <= 2


@allure.feature("订单")
class TestOrderWrite:
    """下单/删单的真实 DB 副作用与回滚。

    单测层用 mock 钉规则，这里验证真实 SQL 真的扣了/回了。
    每条用例自己负责删单回滚；挑不到合适数据就 skip，不硬造脏数据。
    """

    @allure.story("产品下单扣库存、删单回补")
    @pytest.mark.p0
    def test_product_order_decrements_and_delete_restores(self, customer, sellable_product):
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法验证扣库存")
        pid = sellable_product["id"]
        before = sellable_product["num"]

        with allure.step("下单后库存减 1"):
            create = customer.order.create(pid, 1)
            assert create.json()["code"] == 10001, f"下单失败：{create.text[:300]}"
            after = customer.product.get_by_id(pid).json()["data"]["num"]
            assert after == before - 1, f"库存应 {before - 1}，实际 {after}"

        with allure.step("删单后库存回补"):
            oid = customer.order.my_orders(0).json()["data"][0]["id"]
            delete = customer.order.delete(oid)
            assert delete.json()["code"] == 10002
            restored = customer.product.get_by_id(pid).json()["data"]["num"]
            assert restored == before, f"删单后库存应回补到 {before}，实际 {restored}"

    @allure.story("土地下单锁地、删单释放")
    @pytest.mark.p0
    def test_land_order_locks_and_delete_releases(self, customer, rentable_land):
        if rentable_land is None:
            pytest.skip("库中没有 status=1 的可租土地，无法验证锁地")
        lid = rentable_land["id"]

        with allure.step("下单后土地被锁定 status=0"):
            create = customer.order.create(lid, -1)
            assert create.json()["code"] == 10001, f"土地下单失败：{create.text[:300]}"
            locked = customer.land.get_by_id(lid).json()["data"]["status"]
            assert locked == 0, f"下单后土地应锁定为 0，实际 {locked}"

        with allure.step("删单后土地释放 status=1"):
            oid = customer.order.my_orders(0).json()["data"][0]["id"]
            delete = customer.order.delete(oid)
            assert delete.json()["code"] == 10002
            released = customer.land.get_by_id(lid).json()["data"]["status"]
            assert released == 1, f"删单后土地应释放为 1，实际 {released}"

    @allure.story("客户删他人订单被拒")
    @pytest.mark.p0
    def test_customer_cannot_delete_others_order(
        self, employee, customer, make_temp_account, sellable_product, seed_customer_id
    ):
        if sellable_product is None:
            pytest.skip("库中没有库存>=1 且上架的产品，无法造他人订单")
        create = employee.order.create(sellable_product["id"], 1, user_id=seed_customer_id)
        assert create.json()["code"] == 10001, f"员工代录订单失败：{create.text[:300]}"
        oid = customer.order.my_orders(seed_customer_id).json()["data"][0]["id"]
        attacker = make_temp_account()

        resp = attacker.api.order.delete(oid)

        assert resp.json()["code"] == 20008, (
            f"客户删他人订单应返回 20008，实际 {resp.json()['code']}"
        )
        cleanup = customer.order.delete(oid)
        assert cleanup.json()["code"] == 10002, "用例清理失败：他人订单未被所有者删除"
