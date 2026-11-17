"""购物车接口用例（UserController 的 trolley 三接口）。

购物车条目有删除接口，写副作用可完全清理，因此可以做真实增删链路。
重点：自己的条目能增查删；他人的条目客户删不动（归属校验在 Service 层）。
"""
import allure
import pytest


def _entry_of(bundle, user_id, product_id):
    records = bundle.user.my_trolley(user_id).json()["data"]
    return next((r for r in records if r["pid"] == product_id), None)


@allure.feature("购物车")
class TestTrolleyOwn:
    @allure.story("加购-查看-删除闭环")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_add_list_delete_own(self, temp_account, first_product_id):
        with allure.step("加购"):
            add = temp_account.api.user.add_trolley(first_product_id, temp_account.id, 1)
            assert add.json()["code"] == 10001, f"加购失败：{add.text[:300]}"

        with allure.step("列表可见"):
            entry = _entry_of(temp_account.api, temp_account.id, first_product_id)
            assert entry is not None, "加购后列表里看不到该条目"
            assert entry["num"] == 1

        with allure.step("删除后列表不可见"):
            delete = temp_account.api.user.delete_trolley(entry["id"])
            assert delete.json()["code"] == 10002
            assert _entry_of(temp_account.api, temp_account.id, first_product_id) is None


@allure.feature("购物车")
class TestTrolleyAuthorization:
    @allure.story("客户删他人购物车条目被拒")
    @pytest.mark.p0
    def test_customer_cannot_delete_others_entry(self, make_temp_account, first_product_id):
        victim = make_temp_account()
        attacker = make_temp_account()
        add = victim.api.user.add_trolley(first_product_id, victim.id, 1)
        assert add.json()["code"] == 10001
        entry = _entry_of(victim.api, victim.id, first_product_id)

        resp = attacker.api.user.delete_trolley(entry["id"])

        assert resp.json()["code"] == 20008, (
            f"客户删他人购物车条目应返回 20008，实际 {resp.json()['code']}"
        )
        assert _entry_of(victim.api, victim.id, first_product_id) is not None, (
            "越权删除竟然生效了：受害者条目消失"
        )
        victim.api.user.delete_trolley(entry["id"])

    @allure.story("员工可删任意条目（后台能力）")
    @pytest.mark.p1
    def test_staff_can_delete_any_entry(self, employee, make_temp_account, first_product_id):
        victim = make_temp_account()
        victim.api.user.add_trolley(first_product_id, victim.id, 1)
        entry = _entry_of(victim.api, victim.id, first_product_id)

        resp = employee.user.delete_trolley(entry["id"])

        assert resp.json()["code"] == 10002
        assert _entry_of(victim.api, victim.id, first_product_id) is None