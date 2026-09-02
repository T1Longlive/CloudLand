# -*- coding: utf-8 -*-
"""订单模块：下单锁地/扣库存、删单回滚、归属校验（越权防护）。

业务规则（对应 Order2ServiceImpl）：
- 土地订单 num=-1：下单即锁地（land.status→0），删单回滚（→1）
- 产品订单 num>0：下单扣库存，删单回补；库存不足返回 ADD_ERR
- 归属：客户下单 uid 强制为本人；power=0 只能删自己的订单（POWER_ERR）
"""
import allure
import pytest

from common.assertions import ADD_ERR, ADD_OK, DELETE_OK, POWER_ERR, assert_success
from conftest import SEED


@allure.epic("API 自动化层")
@allure.feature("订单模块")
class TestLandOrderFlow:
    """土地订单生命周期（serial：锁地 → 重复下单被拒 → 删单回滚）。"""


    @pytest.fixture(autouse=True)
    def _land(self, land_factory):
        self.land_id = land_factory(price=66.0)
        yield

    @allure.story("下单锁地")
    @allure.title("土地下单 → land.status 置 0（锁地），订单 uid 强制为本人")
    def test_land_order_locks_land(self, customer_client, admin_client):
        # 伪造 uid 试图替管理员下单：客户路径下 uid 会被服务端强制覆盖
        resp = customer_client.add_order(self.land_id, num=-1, uid=admin_client.user_id)
        assert_success(resp, ADD_OK)

        land = admin_client.land_detail(self.land_id)
        assert land["status"] == 0, f"下单后土地应锁定, 实际 status={land['status']}"

        order = customer_client.find_order_by_pid(self.land_id)
        assert order is not None, "订单应出现在本人订单列表"
        assert order["uid"] == customer_client.user_id, (
            f"uid 防劫持失败: 订单 uid={order['uid']}, 期望 {customer_client.user_id}"
        )
        assert order["price"] == 66.0, "土地订单价格应为 land.price"

    @allure.story("下单锁地")
    @allure.title("已锁定土地重复下单 → ADD_ERR(10005)")
    def test_locked_land_rejects_second_order(self, customer_client, employee_client):
        resp = customer_client.add_order(self.land_id, num=-1)
        assert_success(resp, ADD_OK)

        resp = employee_client.add_order(self.land_id, num=-1, uid=employee_client.user_id)
        assert_success(resp, ADD_ERR)

    @allure.story("删单回滚")
    @allure.title("删除未支付土地订单 → land.status 回滚为 1（放地）")
    def test_delete_land_order_restores_land(self, customer_client, admin_client):
        customer_client.add_order(self.land_id, num=-1)
        order = customer_client.find_order_by_pid(self.land_id)

        resp = customer_client.delete_order(order["id"])
        assert_success(resp, DELETE_OK)

        land = admin_client.land_detail(self.land_id)
        assert land["status"] == 1, f"删单后土地应放回, 实际 status={land['status']}"
        assert customer_client.find_order_by_pid(self.land_id) is None, "订单应被硬删除"


@allure.epic("API 自动化层")
@allure.feature("订单模块")
class TestProductOrderFlow:
    """产品订单生命周期（serial：扣库存 → 超卖拒绝 → 删单回补）。"""

    @pytest.fixture(autouse=True)
    def _product(self, product_factory):
        self.product = product_factory(price=10.5, num=10)
        yield

    @allure.story("下单扣库存")
    @allure.title("产品下单 num=3 → 库存减 3，订单 status=0（待支付）")
    def test_product_order_decrements_stock(self, customer_client):
        resp = customer_client.add_order(self.product["id"], num=3)
        assert_success(resp, ADD_OK)

        detail = customer_client.product_detail(self.product["id"])
        assert detail["num"] == 7, f"库存应减 3, 实际 num={detail['num']}"

        order = customer_client.find_order_by_pid(self.product["id"])
        assert order["status"] == 0 and order["num"] == 3

    @allure.story("下单扣库存")
    @allure.title("超库存下单 → ADD_ERR(10005)，库存不变")
    def test_insufficient_stock_rejected(self, customer_client):
        resp = customer_client.add_order(self.product["id"], num=11)  # 库存只有 10
        assert_success(resp, ADD_ERR)

        detail = customer_client.product_detail(self.product["id"])
        assert detail["num"] == 10, f"失败订单不应扣库存, 实际 num={detail['num']}"

    @allure.story("删单回滚")
    @allure.title("删除未支付产品订单 → 库存回补")
    def test_delete_product_order_restores_stock(self, customer_client):
        customer_client.add_order(self.product["id"], num=3)
        order = customer_client.find_order_by_pid(self.product["id"])

        resp = customer_client.delete_order(order["id"])
        assert_success(resp, DELETE_OK)

        detail = customer_client.product_detail(self.product["id"])
        assert detail["num"] == 10, f"删单后库存应回补, 实际 num={detail['num']}"
        assert customer_client.find_order_by_pid(self.product["id"]) is None


@allure.epic("API 自动化层")
@allure.feature("订单模块")
class TestOrderOwnership:
    """订单归属校验：power=0 不可删他人订单。"""

    @allure.story("越权防护")
    @allure.title("客户删除他人订单 → POWER_ERR(20008)")
    def test_customer_cannot_delete_others_order(self, customer_client, employee_client):
        # 员工为自己下单（staff 路径 uid 不被覆盖，需显式传）
        resp = employee_client.add_order(self.product_id, num=1, uid=employee_client.user_id)
        assert_success(resp, ADD_OK)
        order = employee_client.find_order_by_pid(self.product_id, uid=employee_client.user_id)

        # 客户试图删员工的订单
        resp = customer_client.delete_order(order["id"])
        assert_success(resp, POWER_ERR)

        # 订单仍在（员工视角）
        assert employee_client.find_order_by_pid(self.product_id, uid=employee_client.user_id) is not None

        # 员工清理自己的订单
        assert_success(employee_client.delete_order(order["id"]), DELETE_OK)

    @pytest.fixture(autouse=True)
    def _product(self, product_factory):
        self.product_id = product_factory()["id"]
        yield
