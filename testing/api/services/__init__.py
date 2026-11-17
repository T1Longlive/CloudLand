"""业务层：一个类对应一个后端 Controller，只拼参数、只发请求，不做断言。

为什么有 ServiceBundle：
用例里如果写 `UserService(admin_client).page()`，每行都要重复"用哪个登录态 + 哪个服务"。
ServiceBundle 把"某个登录态下的全套业务对象"一次性组装好，
用例里就只剩 `admin.user.page()`，读起来就是"管理员查用户列表"。

这种组装方式也天然防止了拿错身份：`customer.user.page()` 一看就知道是用客户身份调后台接口，
写错登录态一眼能发现，而 `UserService(client)` 看不出 client 是谁。
"""
from services.auth_service import AuthService
from services.authz_service import AuthzService
from services.land_service import LandService
from services.msg_service import MsgService
from services.order_service import OrderService
from services.pay_service import AlipayService
from services.product_service import ProductService
from services.user_service import UserService


class ServiceBundle:
    def __init__(self, client):
        self.client = client
        self.auth = AuthService(client)
        self.authz = AuthzService(client)
        self.user = UserService(client)
        self.land = LandService(client)
        self.product = ProductService(client)
        self.order = OrderService(client)
        self.pay = AlipayService(client)
        self.msg = MsgService(client)


__all__ = [
    "ServiceBundle",
    "AuthService",
    "AuthzService",
    "UserService",
    "LandService",
    "ProductService",
    "OrderService",
    "AlipayService",
    "MsgService",
]
