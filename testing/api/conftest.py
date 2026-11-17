"""pytest 夹具集中定义处。

夹具的唯一目的：把"准备环境"从用例里挤出去，让用例只剩"输入 → 断言"。

作用域（scope）怎么选：
- session：整个测试会话只建一次。HttpClient / 已登录客户端都用它，
  因为登录一次拿到的 token 可以给几十条用例复用，不必每条都重新登录。
- function（默认）：每条用例一份，用于会被用例修改的数据（如临时账号）。

注意：session 级夹具里的客户端如果被某条用例"改坏了"（比如改了 token 或 header），
会影响后续所有用例。所以这里每个角色都单独一个 client，互不干扰。
"""
import logging

import pytest
import redis

from common.config import SEED_ACCOUNTS, settings
from common.data_factory import temp_user
from common.http_client import HttpClient
from services import ServiceBundle
from services.auth_service import AuthService

logger = logging.getLogger(__name__)


def _build_client() -> HttpClient:
    return HttpClient(settings.base_url, settings.timeout)


# ---------- 裸客户端与认证对象 ----------
@pytest.fixture(scope="session")
def api():
    """未登录的裸客户端：给登录、注册这类免认证接口用。"""
    client = _build_client()
    yield client
    client.session.close()


@pytest.fixture(scope="session")
def auth(api) -> AuthService:
    """认证业务对象，绑定在裸客户端上（test_auth.py 使用）。"""
    return AuthService(api)


@pytest.fixture(scope="session")
def anon():
    """未登录状态下的全套业务对象。"""
    client = _build_client()
    yield ServiceBundle(client)
    client.session.close()


# ---------- 三种角色的登录态 ----------
@pytest.fixture(scope="session")
def customer_client():
    """已登录的客户账号客户端（power=0）。"""
    client = _build_client()
    AuthService(client).login_as("customer")
    yield client
    client.session.close()


@pytest.fixture(scope="session")
def employee_client():
    """已登录的员工账号客户端（power=1）。"""
    client = _build_client()
    AuthService(client).login_as("employee")
    yield client
    client.session.close()


@pytest.fixture(scope="session")
def admin_client():
    """已登录的管理员账号客户端（power=2）。"""
    client = _build_client()
    AuthService(client).login_as("admin")
    yield client
    client.session.close()


@pytest.fixture(scope="session")
def customer(customer_client):
    return ServiceBundle(customer_client)


@pytest.fixture(scope="session")
def employee(employee_client):
    return ServiceBundle(employee_client)


@pytest.fixture(scope="session")
def admin(admin_client):
    return ServiceBundle(admin_client)


@pytest.fixture(scope="session")
def seed_customer():
    """seed 客户账号的原始数据，供需要 phone/password 的用例直接取用。"""
    return SEED_ACCOUNTS["customer"]


@pytest.fixture(scope="session")
def redis_client():
    """直连 Redis：仅用于支付交易映射的 TTL 断言与清理。

    protocol=2 是刻意的：redis-py 6.x 默认 RESP3 会发 HELLO，本地 Redis 不支持。
    """
    client = redis.Redis(
        host=settings.redis_host,
        port=settings.redis_port,
        password=settings.redis_password,
        db=settings.redis_db,
        protocol=2,
        decode_responses=True,
    )
    yield client
    client.close()


@pytest.fixture(scope="session")
def seed_customer_id(employee):
    """seed 客户的真实 id（动态查，不硬编码自增 id）。"""
    records = employee.user.page({"username": "测试客户"}, page_size=5).json()["data"]["records"]
    assert records, "seed 客户账号不存在，无法执行依赖其 id 的用例"
    return records[0]["id"]


# ---------- 可写业务数据（动态挑选，挑不到就 skip 而不是硬造脏数据） ----------
@pytest.fixture
def sellable_product(anon):
    """库存 >=1 且上架的产品；下单扣库存用例的前置。"""
    records = anon.product.page(page_size=50).json()["data"]["records"]
    return next((r for r in records if (r.get("num") or 0) >= 1 and r.get("status") == 1), None)


@pytest.fixture
def rentable_land(anon):
    """status=1（可租用）的土地；锁地用例的前置。"""
    records = anon.land.page(page_size=50).json()["data"]["records"]
    return next((r for r in records if r.get("status") == 1), None)


# ---------- 真实业务数据 id（动态取，不硬编码） ----------
# 硬编码 id 的用例在别人机器上必挂：库一导入，自增 id 就变了。
# 这里通过公开分页接口拿"当前库里真实存在的第一条"，保证用例可移植。
@pytest.fixture(scope="session")
def first_land_id(anon) -> int:
    records = anon.land.page().json()["data"]["records"]
    assert records, "库里没有任何土地数据，无法执行依赖土地 id 的用例"
    return records[0]["id"]


@pytest.fixture(scope="session")
def first_product_id(anon) -> int:
    records = anon.product.page().json()["data"]["records"]
    assert records, "库里没有任何产品数据，无法执行依赖产品 id 的用例"
    return records[0]["id"]


# ---------- 临时账号（function 级，用完自动删） ----------
class TempAccount:
    """一个新注册的临时账号，带上它自己的登录态。

    为什么要 function 级：这类账号会被用例修改（禁用、改权限、改资料），
    如果 session 级共享，前一条用例把它禁了，后面全部连坐。
    """

    def __init__(self, user: dict, account_id: int, token: str, bundle: ServiceBundle):
        self.user = user
        self.id = account_id
        self.token = token
        self.api = bundle  # 该账号自己的登录态

    @property
    def phone(self) -> str:
        return self.user["phone"]

    @property
    def password(self) -> str:
        return self.user["password"]


@pytest.fixture
def make_temp_account(anon, admin):
    """临时账号工厂：调用一次造一个，用例结束后统一按创建顺序删除。

    为什么是工厂而不是单个夹具：
    越权类用例需要"两个人"（攻击者和受害者）。如果夹具只能造一个，
    就只能去改 seed 账号当受害者 —— 那是把只读数据写坏了，
    下一个跑用例的人会莫名失败。宁可多造一个用完即删的账号。

    清理走 API（DELETE /user）而不是直连数据库：
    - 不引入 pymysql 依赖，也不用在测试里存数据库口令；
    - 顺带把"删除用户"这条链路也跑了一遍，等于免费的连通性验证。
    """
    created = []

    def _make(**overrides):
        """造一个临时账号；overrides 会覆盖默认字段（如 power=2 做提权测试）。"""
        user = temp_user()
        user.update(overrides)

        register_resp = anon.auth.register(user)
        assert register_resp.json()["code"] == 20006, (
            f"临时账号注册失败，后续用例无法执行：{register_resp.text[:300]}"
        )

        client = _build_client()
        bundle = ServiceBundle(client)
        login_resp = bundle.auth.login(phone=user["phone"], password=user["password"])
        body = login_resp.json()
        assert body["code"] == 20005, f"临时账号登录失败：{login_resp.text[:300]}"
        bundle.client.token = body["msg"]

        account = TempAccount(user, body["data"]["id"], body["msg"], bundle)
        created.append(account)
        return account

    yield _make

    for account in created:
        account.api.client.session.close()
        try:
            resp = admin.user.delete([account.id])
            if resp.json().get("code") != 10002:
                logger.warning("临时账号 %s 清理失败：%s", account.phone, resp.text[:200])
        except Exception as exc:  # 清理异常不能吃掉测试结果
            logger.warning("临时账号 %s 清理异常：%s", account.phone, exc)


@pytest.fixture
def temp_account(make_temp_account):
    """单个临时账号（最常用的情况）。"""
    return make_temp_account()
