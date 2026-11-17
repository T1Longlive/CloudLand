"""认证接口用例。

用例层只做三件事：拿数据 → 调 service → 断言。不许出现 requests、不许拼 URL。

组织方式说明（面试常被问"你的用例怎么组织"）：
- 正常流单独写成显式用例（test_login_success）：它需要一堆"正常业务该有的样子"的断言，
  塞进数据驱动会让 YAML 变成断言脚本，反而看不懂。
- 异常流用数据驱动（test_login_failure）：异常场景共同点强、单条差异小、数量多，
  最适合 YAML 化，加一条新异常只加一段数据。
"""
import allure
import pytest

from common.assertions import assert_code, assert_http
from common.data_factory import temp_user
from common.data_loader import load_yaml

# 加载一次，模块级常量；yaml 改动后重跑即可生效
LOGIN_CASES = load_yaml("auth/login.yaml")
SUCCESS_CASE = next(c for c in LOGIN_CASES if c["name"] == "login_ok")
FAILURE_CASES = [c for c in LOGIN_CASES if c["name"] != "login_ok"]


@allure.feature("认证")
class TestLogin:
    @allure.story("密码登录成功")
    @allure.title("客户账号密码正确时登录成功，返回可用 JWT 与账号信息")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_login_success(self, auth):
        """正常流：不只判成功码，还要判"承诺给前端的东西"是否真的给了。

        这一条用例同时守住了四个契约，任何一条被改坏都该红：
        1. 业务码 20005；
        2. JWT 在 msg 里（不是 data、不是响应头）；
        3. data 回显账号信息且 password 被清空（安全：接口不能回吐密码哈希）；
        4. 客户账号带 frond 头可登录（power=0 的放行条件）。
        """
        case = SUCCESS_CASE
        phone = case["request"]["phone"]

        with allure.step(f"调用登录接口：{phone}"):
            resp = auth.login(**case["request"])

        with allure.step("双层断言：HTTP 状态码 + 业务码"):
            assert_http(resp, case["expect"]["http"])
            assert_code(resp, case["expect"]["code"])

        body = resp.json()

        with allure.step("断言 JWT 放在 msg 字段且结构合法"):
            token = body["msg"]
            assert isinstance(token, str) and token.count(".") == 2, (
                f"登录成功应返回三段式 JWT，实际 msg={token!r}"
            )

        with allure.step("断言账号信息回显，且密码不被返回"):
            data = body["data"]
            assert data["phone"] == phone, f"回显手机号不符：{data.get('phone')}"
            assert data["id"] is not None, "回显账号信息缺少 id"
            assert data["password"] is None, "接口不应回吐密码（含 BCrypt 哈希）"

    @allure.story("登录失败分支")
    @pytest.mark.p1
    @pytest.mark.parametrize("case", FAILURE_CASES, ids=[c["name"] for c in FAILURE_CASES])
    def test_login_failure(self, auth, case):
        """异常流：密码错、账号不存在、验证码错、缺 frond 头。

        所有异常分支的共同点：
        - HTTP 依然是 200（本项目用业务码表达失败，只有拦截器才返回 401/402/403）；
        - data 为 null；
        - 不返回 token。

        这里把"HTTP 200 + 业务码失败"作为显式断言写下，
        就是为了让"只断言 HTTP 200"这种低级错误在这份代码里无处藏身。
        """
        with allure.step(case["title"]):
            resp = auth.login(**case["request"])

        assert_http(resp, case["expect"]["http"])
        assert_code(resp, case["expect"]["code"])

        body = resp.json()
        assert body["data"] is None, f"失败场景 data 应为 null，实际 {body['data']}"
        # 失败时 msg 是错误文案；JWT 一定是两段点号分隔，因此用点号数量做粗判足够
        assert str(body["msg"]).count(".") < 2, (
            f"失败场景不应返回 JWT，实际 msg={body['msg']!r}"
        )


@allure.feature("认证")
class TestRegister:
    @allure.story("注册并登录")
    @allure.title("新用户注册成功，且注册时的 power 提权尝试被服务端忽略")
    @pytest.mark.p1
    def test_register_then_login(self, employee, make_temp_account):
        """注册 → 登录 的连通性用例，同时守住一个真实修过的安全缺陷。

        历史问题：注册接口曾经直接把客户端传来的 power 存库，
        构造 power=2 的请求就能注册成管理员。
        现在服务端强制 power=0/status=1/debt=0。

        用例里故意传 power=2，然后从后台读回真实 power，断言必须是 0。
        比"读源码确认"强得多 —— 它会在任何人重新引入该缺陷时立刻变红。

        数据从 `make_temp_account` 工厂来，不用自己调 register()：
        工厂会在用例结束后删账号。自己手工注册的账号没有归宿，
        跑几轮库里就堆一堆 134 段垃圾数据 —— 这是数据治理最容易漏的一环。
        """
        # 提权尝试：power=2 应被服务端忽略
        account = make_temp_account(power=2)

        with allure.step("断言提权被忽略：新账号 power 必须是 0"):
            data = employee.user.get_by_id(account.id).json()["data"]
            assert data["power"] == 0, (
                f"注册时传 power=2 被接受了，存在提权漏洞：实际 power={data['power']}"
            )
            assert data["status"] == 1, f"新账号应为激活状态，实际 status={data.get('status')}"

        with allure.step("断言新账号能用注册时设置的密码登录"):
            login_resp = account.api.auth.login(
                phone=account.phone, password=account.password
            )
            assert_http(login_resp, 200)
            assert_code(login_resp, 20005)

    @allure.story("注册参数缺失")
    @allure.title("缺陷记录：注册不传详细地址时后端返回 500 而不是 4xx 参数错误")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="后端缺参数校验：detailed_address 为 NOT NULL 且无默认值，Missing 时 MySQL 抛 "
               "DataIntegrityViolationException，被兜底成 HTTP 500 并打印堆栈。"
               "期望改为 4xx 或业务码，修复后本条会 XPASS（strict 模式转红）提示更新用例。",
    )
    def test_register_without_detailed_address(self, auth):
        """把"真实缺陷"变成一条会自己提醒你的用例。

        为什么用 xfail(strict=True) 而不是删掉不测：
        - 它让缺陷在报告里"可见"，而不是消失在口头描述里；
        - 后端一旦修复，这条会 XPASS → strict 模式直接失败，强制你回来把它改成正式用例；
        - 面试时可以指着它讲"我是怎么把线上问题沉淀成回归资产的"。

        当前事实：前端注册表单有必填校验，所以正常 UI 路径不会触发；
        但接口层面任何人都能构造缺字段请求，5xx + 堆栈属于信息泄露与可用性问题。
        """
        user = temp_user()
        del user["detailedAddress"]

        resp = auth.register(user)

        # 期望：参数不合法应被明确拒绝（400/422，或按项目惯例返回 HTTP 200 + 业务码），
        # 但绝不该是 5xx —— 5xx 意味着服务端未做参数校验，且把堆栈暴露给了调用方。
        assert resp.status_code < 500, (
            f"注册缺少必填字段时返回了 {resp.status_code}，"
            f"服务端未做参数校验：{resp.text[:300]}"
        )


@allure.feature("认证")
class TestTokenChannel:
    """token 续期通道：带 token 头且 body 无密码时走 JWT 校验+重签。"""

    @allure.story("token 续期成功")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_token_renewal_returns_new_jwt(self, auth, customer_client):
        resp = auth.login_by_token(customer_client.token)

        assert_http(resp, 200)
        assert_code(resp, 20005)
        body = resp.json()
        assert str(body["msg"]).count(".") == 2, "续期应返回新的三段式 JWT"
        assert body["data"]["password"] is None, "续期回显不应带密码哈希"

    @allure.story("坏 token 续期被拒")
    @pytest.mark.p0
    def test_garbage_token_renewal_rejected(self, auth):
        resp = auth.login_by_token("not-a-jwt")

        assert_http(resp, 200)
        assert_code(resp, 20003)

    @allure.story("禁用账号的 token 续期被拒")
    @pytest.mark.p0
    def test_disabled_account_token_rejected(self, auth, admin, make_temp_account):
        account = make_temp_account()
        update = admin.user.update({"id": account.id, "status": 0})
        assert update.json()["code"] == 10003

        resp = auth.login_by_token(account.token)

        assert_http(resp, 200)
        assert_code(resp, 20004), "账号禁用后 token 续期必须被拒（20004）"
