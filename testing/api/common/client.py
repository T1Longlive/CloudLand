# -*- coding: utf-8 -*-
"""API 客户端门面：保持既有方法签名，内部委托到 services 层。

分层约定：
- common/http.py   驱动层：发请求、token、日志
- services/*.py    业务层：登录/用户/商品/订单/支付
- 本文件           兼容门面：老用例继续 api.xxx()，新用例可用 api.auth_service.xxx()

这样拆分后，已有用例一行不用改，新用例可以按“驱动 → 业务 → 用例”三层写。

字段命名契约（重要）：后端 @RequestBody 的 Lombok 风格属性（pId/uId）
经 Jackson 绑定后的实际 JSON 键是全小写 pid/uid，与前端 MyTrolley.vue 一致；
序列化响应同理（订单记录里是 "pid"/"uid"）。
"""
from common.http import HttpClient
from services.auth_service import AuthService
from services.catalog_service import CatalogService
from services.order_service import OrderService
from services.pay_service import PayService
from services.user_service import UserService


class ApiClient(HttpClient):
    def __init__(self, base_url, timeout=10):
        super().__init__(base_url, timeout)
        self.auth_service = AuthService(self)
        self.user_service = UserService(self)
        self.catalog_service = CatalogService(self)
        self.order_service = OrderService(self)
        self.pay_service = PayService(self)

    # ── 认证 ──────────────────────────────────────────────
    def login(self, phone, password, master_code="000000"):
        return self.auth_service.login(phone, password, master_code)

    def register(self, user: dict, master_code="000000"):
        return self.auth_service.register(user, master_code)

    # ── 用户 ──────────────────────────────────────────────
    def user_page(self, phone=None, page_num=1, page_size=100):
        return self.user_service.user_page(phone, page_num, page_size)

    def find_user_by_phone(self, phone):
        return self.user_service.find_user_by_phone(phone)

    def delete_user(self, user_id):
        return self.user_service.delete_user(user_id)

    # ── 商品/土地 ─────────────────────────────────────────
    def product_page(self, name=None, page_num=1, page_size=100):
        return self.catalog_service.product_page(name, page_num, page_size)

    def find_product_by_name(self, name):
        return self.catalog_service.find_product_by_name(name)

    def product_detail(self, product_id):
        return self.catalog_service.product_detail(product_id)

    def land_detail(self, land_id):
        return self.catalog_service.land_detail(land_id)

    # ── 订单 ──────────────────────────────────────────────
    def add_order(self, pid, num, uid=None):
        return self.order_service.add_order(pid, num, uid)

    def my_orders(self, uid=None):
        return self.order_service.my_orders(uid)

    def find_order_by_pid(self, pid, uid=None):
        return self.order_service.find_order_by_pid(pid, uid)

    def delete_order(self, order_id):
        return self.order_service.delete_order(order_id)

    def mark_order_paid(self, order_ids, status=1, pay_time=True):
        return self.order_service.mark_order_paid(order_ids, status, pay_time)

    # ── 支付 ──────────────────────────────────────────────
    def pay(self, order_ids, total_amount=None):
        return self.pay_service.pay(order_ids, total_amount)
