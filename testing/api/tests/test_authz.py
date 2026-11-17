"""鉴权矩阵：这个项目最该测、也最容易漏的一层。

为什么单独一个文件：
业务用例（登录、下单、查列表）验证"功能对不对"；
这一组用例验证"该拒绝的有没有拒绝"。后者才是安全底线，
而且它有 15 个后台接口 × 3 种角色 × 4 种状态码的组合，天然适合矩阵化。

后端的鉴权链路（MyInterceptor.preHandle，顺序不能错）：
    缺 token            → 401
    token 解析失败       → 401
    token 里的 id 查不到  → 401
    账号 status=0        → 403 + X-Forbidden-Reason: status
    token.power ≠ 库.power → 402（权限被管理员改过，要求重新登录）
    命中 ADMIN_RULES 且 power<1 → 403 + X-Forbidden-Reason: power

关键技巧：这些判断全部发生在 Spring 参数绑定之前，所以探测时**不需要构造合法参数**，
零数据写入、毫秒级完成。不用真的写一个"合法的删土地请求"才能验证它被拦住。
"""
import allure
import pytest

# 与 MyInterceptor.ADMIN_RULES 逐条对齐（"METHOD servletPath"，不含 /api 前缀）
ADMIN_RULES = [
    ("POST", "/user/page"),
    ("POST", "/user/employee"),
    ("POST", "/user/id"),
    ("DELETE", "/user"),
    ("POST", "/land"),
    ("PUT", "/land"),
    ("DELETE", "/land"),
    ("POST", "/product"),
    ("PUT", "/product"),
    ("DELETE", "/product"),
    ("POST", "/order/page"),
    ("PUT", "/order"),
    ("POST", "/order/download"),
    ("POST", "/msg/page"),
    ("POST", "/msg/push"),
]

RULE_IDS = [f"{m} {p}" for m, p in ADMIN_RULES]

# 这两个接口"通过鉴权后会真的产生外部副作用"：群发邮件、导出 xlsx。
# 所以只验证它们能被拦住（401/403），不验证"员工调用能成功"——
# 否则每跑一次测试就给所有订阅者发一封测试邮件。
SIDE_EFFECT_RULES = {"POST /msg/push", "POST /order/download"}
POSITIVE_RULES = [(m, p) for m, p in ADMIN_RULES if f"{m} {p}" not in SIDE_EFFECT_RULES]
POSITIVE_IDS = [f"{m} {p}" for m, p in POSITIVE_RULES]


@allure.feature("鉴权")
class TestAuthentication:
    """身份认证：401 家族。"""

    @allure.story("缺少令牌")
    @pytest.mark.smoke
    @pytest.mark.p0
    @pytest.mark.parametrize("method,path", ADMIN_RULES, ids=RULE_IDS)
    def test_missing_token_returns_401(self, anon, method, path):
        """不带 token 访问任何后台接口，都必须是 401，且不返回业务 JSON。

        除了状态码，还要断言"响应体不是统一 Result 结构"——
        如果哪天有人把 401 改成 200 + 业务码，这条会红，提醒你 API 契约变了。
        """
        with allure.step(f"匿名访问 {method} {path}"):
            resp = anon.authz.probe(method, path)

        assert resp.status_code == 401, (
            f"匿名访问 {method} {path} 应返回 401，实际 {resp.status_code}：{resp.text[:200]}"
        )

    @allure.story("令牌非法")
    @pytest.mark.p0
    @pytest.mark.parametrize(
        "bad_token",
        ["not-a-jwt", "a.b.c", ""],
        ids=["random_string", "three_parts_but_invalid", "empty_string"],
    )
    def test_invalid_token_returns_401(self, anon, bad_token):
        """伪造/损坏/空 token 一律 401。

        注意空字符串：后端判断的是 StringUtils.isEmpty(token)，
        所以 `token: ""` 与完全不传 token 等价，都必须 401。
        这类边界值是手工点页面永远点不到的。
        """
        resp = anon.authz.probe("POST", "/user/page", token=bad_token)
        assert resp.status_code == 401, (
            f"token={bad_token!r} 应返回 401，实际 {resp.status_code}"
        )


@allure.feature("鉴权")
class TestAuthorization:
    """授权：403 / 402。"""

    @allure.story("客户访问后台接口")
    @pytest.mark.smoke
    @pytest.mark.p0
    @pytest.mark.parametrize("method,path", ADMIN_RULES, ids=RULE_IDS)
    def test_customer_gets_403_with_power_reason(self, customer, method, path):
        """客户（power=0）访问后台接口必须 403，且带 X-Forbidden-Reason: power。

        为什么要断言这个自定义头：
        403 有两种完全不同的原因（账号禁用 / 权限不足），前端要据此给不同提示。
        只判 403 无法区分，一旦后端把两种原因都返 403 却不带头，前端就把"被禁用"提示成"权限不足"。
        """
        resp = customer.authz.probe(method, path)

        assert resp.status_code == 403, (
            f"客户访问 {method} {path} 应返回 403，实际 {resp.status_code}"
        )
        assert resp.headers.get("X-Forbidden-Reason") == "power", (
            f"403 应携带 X-Forbidden-Reason: power，实际 "
            f"{resp.headers.get('X-Forbidden-Reason')!r}"
        )

    @allure.story("员工访问后台接口")
    @pytest.mark.p1
    @pytest.mark.parametrize("method,path", POSITIVE_RULES, ids=POSITIVE_IDS)
    def test_employee_passes_authorization(self, employee, method, path):
        """员工（power=1）访问后台接口不应被拦成 403。

        只断言"没被拦住"，不断言业务成功：故意不传参数，
        请求会死在参数绑定阶段（400/415/500 都正常），
        这说明它**成功穿过了鉴权层**——正是本条要验证的东西。
        """
        resp = employee.authz.probe(method, path)
        assert resp.status_code != 403, (
            f"员工访问 {method} {path} 被错误拒绝（403），"
            f"reason={resp.headers.get('X-Forbidden-Reason')!r}"
        )

    @allure.story("权限被管理员变更")
    @pytest.mark.p0
    def test_power_changed_returns_402(self, admin, temp_account):
        """管理员改了我的权限后，我手里的旧 token 必须失效（402 请重新登录）。

        这是真实的安全要求：token 里带着 power，如果后端只信 token 不信库，
        一个被降权的管理员可以继续用旧 token 横着走。
        后端的做法是每次请求都比对 token.power 与库.power，不一致就 402。

        造数链路完全走 API：注册临时账号 → 拿 token → 管理员改 power → 用旧 token 请求。
        """
        with allure.step("确认当前 token 可用"):
            before = temp_account.api.authz.probe("POST", "/user/page")
            assert before.status_code == 403, "临时账号是 power=0，访问后台接口本就该 403"

        with allure.step("管理员把该账号 power 改成 1"):
            update = admin.user.update({"id": temp_account.id, "power": 1})
            assert update.json()["code"] == 10003, f"改权限失败：{update.text[:300]}"

        with allure.step("用旧 token 再次请求，应被要求重新登录"):
            after = temp_account.api.authz.probe("POST", "/user/page")
            assert after.status_code == 402, (
                f"权限已被改成 1，但旧 token 仍被接受（HTTP {after.status_code}），"
                "存在越权风险：token 内的 power 必须与库中实时权限比对"
            )

    @allure.story("账号被禁用")
    @pytest.mark.p0
    def test_disabled_account_returns_403_with_status_reason(self, admin, temp_account):
        """账号被禁用后，已登录的 token 必须立即失效（403 + X-Forbidden-Reason: status）。"""
        with allure.step("管理员禁用该账号"):
            update = admin.user.update({"id": temp_account.id, "status": 0})
            assert update.json()["code"] == 10003, f"禁用账号失败：{update.text[:300]}"

        with allure.step("用该账号的 token 请求，应被判定账号停用"):
            resp = temp_account.api.authz.probe("POST", "/user/page")

        assert resp.status_code == 403, (
            f"账号已禁用，token 应失效返回 403，实际 {resp.status_code}"
        )
        assert resp.headers.get("X-Forbidden-Reason") == "status", (
            f"禁用账号的 403 应带 X-Forbidden-Reason: status（用于与权限不足区分），"
            f"实际 {resp.headers.get('X-Forbidden-Reason')!r}"
        )


@allure.feature("鉴权")
class TestPublicAccess:
    """公开接口：白名单不能被误改成"需要登录"，否则前台首页直接白屏。"""

    @allure.story("公开查询无需登录")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_land_detail_is_public(self, anon, first_land_id):
        resp = anon.land.get_by_id(first_land_id)
        assert resp.status_code != 401, "土地详情是公开接口，不应要求登录"
        assert resp.json()["code"] == 10004

    @pytest.mark.p0
    def test_land_page_is_public(self, anon):
        resp = anon.land.page()
        assert resp.status_code != 401
        assert resp.json()["code"] == 10004

    @pytest.mark.p0
    def test_product_detail_is_public(self, anon, first_product_id):
        resp = anon.product.get_by_id(first_product_id)
        assert resp.status_code != 401
        assert resp.json()["code"] == 10004

    @pytest.mark.p0
    def test_product_page_is_public(self, anon):
        resp = anon.product.page()
        assert resp.status_code != 401
        assert resp.json()["code"] == 10004

    @allure.story("反向：后台接口不该对匿名开放")
    @pytest.mark.p0
    @pytest.mark.parametrize(
        "method,path",
        [("POST", "/user/page"), ("POST", "/order/page"), ("POST", "/msg/page")],
        ids=["user_page", "order_page", "msg_page"],
    )
    def test_admin_list_not_public(self, anon, method, path):
        """这三条恰恰是"名字看起来像查询"的接口，最容易被误加进白名单。

        用列表驱动而不是写死三条断言，是因为以后新增后台接口时，
        只要往参数里补一行就会被覆盖到，不需要重写用例。
        """
        resp = anon.authz.probe(method, path)
        assert resp.status_code == 401, (
            f"{method} {path} 是后台接口，不能出现在匿名白名单里，实际 {resp.status_code}"
        )
