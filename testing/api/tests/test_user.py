"""用户管理接口用例（UserController 非认证部分）。

重点不是"接口能返回 200"，而是三类容易出事的地方：
1. 分页契约（前端 el-pagination 依赖 current/size/total/records 四个字段）；
2. 敏感字段（密码哈希绝不能出后端）；
3. 越权（客户能不能改别人的资料、能不能改自己的密码之外的密码）。
"""
import allure
import pytest

from common.data_factory import DEFAULT_PASSWORD, temp_phone, temp_user

NEW_PASSWORD = "NewPwd@20261116"


@allure.feature("用户管理")
class TestUserQuery:
    @allure.story("后台分页")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_admin_page_users_returns_pagination(self, employee):
        """员工可查用户列表，且分页结构满足前端契约。

        为什么断言这四个字段：Element UI 的分页组件直接吃
        current / size / total / records，少一个前端就渲染成空白，
        而 HTTP 依然是 200 —— 只判状态码发现不了。
        """
        resp = employee.user.page(page_num=1, page_size=5)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004

        page = body["data"]
        for field in ("records", "total", "size", "current"):
            assert field in page, f"分页响应缺少字段 {field}，前端分页会渲染失败"
        assert page["current"] == 1
        assert page["size"] == 5
        assert page["total"] >= 1
        assert len(page["records"]) <= 5, "返回条数超过 pageSize，分页参数未生效"

    @allure.story("后台分页：pageSize 生效")
    @pytest.mark.p1
    def test_page_size_is_respected(self, employee):
        """pageSize=2 时必须只返回 2 条。

        这条看着很傻，但分页参数被后端忽略是极常见的隐蔽 bug：
        页面只显示 2 条、实际拉了 500 条，用户看不出，只有抓包才知道。
        """
        page = employee.user.page(page_num=1, page_size=2).json()["data"]
        assert page["size"] == 2
        assert len(page["records"]) == 2, (
            f"pageSize=2 但返回了 {len(page['records'])} 条，分页未生效"
        )

    @allure.story("后台按手机号搜索失效")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：POST /user/page 的 phone 条件被静默忽略。"
               "UserMapper.selectByCondition 只拼了 status / id / power / username 四个 <if>，"
               "传 phone 会返回全部用户（与 msg/page 的 phone 过滤失效同源）。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_page_users_filter_by_phone_must_narrow_results(self, employee):
        """按手机号搜索必须收窄结果，而不是把全表倒给调用方。

        为什么这条重要（不只是"少个筛选"）：
        后台运营拿手机号搜用户，是核对"这个人到底有没有注册"的常用动作。
        条件被忽略时界面返回的是一整页无关账号，运营会得出"查到了"的错觉，
        进而对错误的人做改绑/禁用。**静默错误比报错危险**。

        这条是 E2E 造数时撞出来的：E2E 想按手机号定位刚注册的临时账号，
        结果每次都拿到全表，才发现条件根本没实现。
        """
        target_phone = "19900000001"  # seed 客户，库里必然存在
        page = employee.user.page({"phone": target_phone}, page_size=10).json()["data"]
        records = page["records"]

        assert records, "按手机号过滤后返回空，条件语义至少应包含自己"
        assert len(records) == 1, (
            f"按手机号 {target_phone} 过滤返回了 {len(records)} 条，条件未生效"
        )
        assert records[0]["phone"] == target_phone

    @allure.story("按 id 查用户")
    @pytest.mark.p0
    def test_get_by_id_hides_password(self, employee, temp_account):
        """按 id 查详情必须把 password 置空。

        这条用临时账号而不是 seed 账号：断言的是"任何账号都不该回吐密码"，
        用自己造的数据更干净，也不会因为别人改了 seed 账号而波动。
        """
        resp = employee.user.get_by_id(temp_account.id)
        body = resp.json()
        assert body["code"] == 10004
        assert body["data"]["id"] == temp_account.id
        assert body["data"]["phone"] == temp_account.phone
        assert body["data"]["password"] is None, (
            "按 id 查用户时返回了密码字段，即使是哈希也不应出后端"
        )


@allure.feature("用户管理")
class TestUserSecurity:
    @allure.story("越权修改他人资料")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_customer_cannot_modify_others_profile(self, employee, make_temp_account):
        """客户拿着别人的 id 去改资料，只能改到自己身上（后端强制回填登录者 id）。

        这是典型的 IDOR（越权访问对象）漏洞测试：
        请求体里带 victim 的 id，如果不做归属校验，就能改任意用户资料。

        用两个临时账号（攻击者/受害者）而不是拿 seed 账号当受害者 ——
        即使哪天后端真的出了漏洞，被写坏的也只是用完即删的临时账号。
        """
        attacker = make_temp_account()
        victim = make_temp_account()

        with allure.step(f"攻击者 {attacker.id} 尝试修改受害者 {victim.id} 的用户名"):
            resp = attacker.api.user.update({"id": victim.id, "username": "HACKED_BY_IDOR"})
            assert resp.json()["code"] == 10003, f"改资料接口未成功返回：{resp.text[:300]}"

        with allure.step("断言受害者未被修改"):
            victim_after = employee.user.get_by_id(victim.id).json()["data"]
            assert victim_after["username"] != "HACKED_BY_IDOR", (
                "越权漏洞：客户用他人的 id 成功修改了他人资料"
            )

        with allure.step("断言改动落在了攻击者自己身上（后端把 id 改回登录者）"):
            attacker_after = employee.user.get_by_id(attacker.id).json()["data"]
            assert attacker_after["username"] == "HACKED_BY_IDOR", (
                "后端应把 id 强制改成登录者本人，实际未生效"
            )

    @allure.story("后台用户列表泄露密码哈希")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：POST /user/page 直接返回 user 全字段，含 BCrypt 密码哈希；"
               "而 POST /user/id 会 setPassword(null)。同一份数据两套标准，属实现遗漏。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_page_users_must_not_leak_password_hash(self, employee):
        """把"分页接口泄露密码哈希"变成一条会自己提醒你的用例。

        风险说明（面试可讲的判断）：
        BCrypt 本身不可逆，但哈希外泄意味着攻击者可以离线爆破弱口令，
        而且一旦数据库其他表关联泄露，可横向撞库。
        正确做法是在 VO/DTO 层剥离 password，而不是依赖每个方法各自记得置空。
        """
        records = employee.user.page(page_size=10).json()["data"]["records"]
        leaked = [r["id"] for r in records if r.get("password")]
        assert not leaked, (
            f"用户分页接口泄露了 {len(leaked)} 个账号的密码哈希（id={leaked[:5]}），"
            "应在返回前统一剥离 password 字段"
        )


@allure.feature("用户管理")
class TestUpdatePassword:
    @allure.story("原密码错误")
    @pytest.mark.p0
    def test_wrong_old_password_rejected(self, temp_account):
        resp = temp_account.api.user.update_password("WrongOld@123", NEW_PASSWORD)
        assert resp.json()["code"] == 20002, (
            f"原密码错误应返回 20002，实际 {resp.json()['code']}"
        )

    @allure.story("新旧密码相同")
    @pytest.mark.p1
    def test_same_password_rejected(self, temp_account):
        """新旧密码相同应明确拒绝（20010），而不是白跑一次加密。"""
        resp = temp_account.api.user.update_password(DEFAULT_PASSWORD, DEFAULT_PASSWORD)
        assert resp.json()["code"] == 20010

    @allure.story("改密成功并用新密码登录")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_change_password_then_login_with_new(self, temp_account):
        """改密不只是"返回成功"，必须能用新密码登录、且旧密码失效。

        只断言 10003 是不够的：更新了错误的字段、或者加密方式不一致，
        都会返回成功但实际登录不上。端到端验证才是真的验证。
        """
        with allure.step("修改密码"):
            resp = temp_account.api.user.update_password(DEFAULT_PASSWORD, NEW_PASSWORD)
            assert resp.json()["code"] == 10003

        with allure.step("新密码可登录"):
            new_login = temp_account.api.auth.login(
                phone=temp_account.phone, password=NEW_PASSWORD
            )
            assert new_login.json()["code"] == 20005, (
                f"改密后无法用新密码登录：{new_login.text[:300]}"
            )

        with allure.step("旧密码已失效"):
            old_login = temp_account.api.auth.login(
                phone=temp_account.phone, password=DEFAULT_PASSWORD
            )
            assert old_login.json()["code"] == 20002, (
                "改密后旧密码仍然可用，存在安全风险"
            )


@allure.feature("用户管理")
class TestRegisterUniqueness:
    @allure.story("手机号查重")
    @pytest.mark.p0
    def test_duplicate_phone_rejected(self, anon, make_temp_account):
        account = make_temp_account()
        user = temp_user()
        user["phone"] = account.phone

        resp = anon.auth.register(user)

        assert resp.json()["code"] == 20007, (
            f"重复手机号注册应返回 20007，实际 {resp.json()['code']}"
        )

    @allure.story("邮箱查重")
    @pytest.mark.p0
    def test_duplicate_mail_rejected(self, anon, make_temp_account):
        account = make_temp_account()
        user = temp_user()
        user["mail"] = account.user["mail"]

        resp = anon.auth.register(user)

        assert resp.json()["code"] == 30004, (
            f"重复邮箱注册应返回 30004，实际 {resp.json()['code']}"
        )


@allure.feature("用户管理")
class TestContactChange:
    @allure.story("改绑手机后可用新手机号登录")
    @pytest.mark.p0
    def test_change_phone_then_login_with_new(self, temp_account):
        new_phone = temp_phone()

        resp = temp_account.api.user.update(
            {"id": temp_account.id, "phone": new_phone,
             "mail": temp_account.user["mail"], "password": DEFAULT_PASSWORD},
            frond=True, contact=True,
        )
        assert resp.json()["code"] == 10003, f"改绑手机失败：{resp.text[:300]}"

        login = temp_account.api.auth.login(phone=new_phone, password=DEFAULT_PASSWORD)
        assert login.json()["code"] == 20005, "改绑后新手机号应能登录"

    @allure.story("绑定信息未变化被拒")
    @pytest.mark.p1
    def test_contact_unchanged_rejected(self, temp_account):
        resp = temp_account.api.user.update(
            {"id": temp_account.id, "phone": temp_account.phone,
             "mail": temp_account.user["mail"], "password": DEFAULT_PASSWORD},
            frond=True, contact=True,
        )

        assert resp.json()["code"] == 20011, (
            f"手机/邮箱均未变化应返回 20011，实际 {resp.json()['code']}"
        )
