# -*- coding: utf-8 -*-
"""requests 封装：token 头自动附加 + updatedToken 滑动续期。

对应前端 src/request/interceptor.js 的行为契约：
- 请求头 `token` 携带 JWT
- 响应头 `updatedToken` 出现时自动替换本地 token（滑动续期）
- HTTP 401/402/403 语义由调用方结合 assertions 判断

字段命名契约（重要）：后端 @RequestBody 的 Lombok 风格属性（pId/uId）
经 Jackson 绑定后的实际 JSON 键是全小写 pid/uid，与前端 MyTrolley.vue 一致；
序列化响应同理（订单记录里是 "pid"/"uid"）。
"""
import json

import requests


class ApiClient:
    def __init__(self, base_url, timeout=10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.token = None
        self.user_id = None  # 登录成功后填充（下单归属/造数 aId 使用）

    # ── 底层请求 ─────────────────────────────────────────
    def request(self, method, path, *, frond=False, code=None, **kwargs):
        """发请求并处理 token 续期。返回 requests.Response。

        - frond/code：模拟前端自定义头（登录/注册的验证码通道）
        - 其余 kwargs 原样传给 session.request（json=/data=/headers= 等）
        """
        headers = kwargs.pop("headers", {}) or {}
        if self.token:
            headers["token"] = self.token
        if frond:
            headers["frond"] = "true"
        if code is not None:
            headers["code"] = code

        resp = self.session.request(
            method, f"{self.base_url}{path}", headers=headers, timeout=self.timeout, **kwargs
        )

        # updatedToken 滑动续期（HTTP 头不区分大小写）
        updated = resp.headers.get("updatedToken") or resp.headers.get("updatedtoken")
        if updated:
            self.token = updated
        return resp

    def get(self, path, **kw):
        return self.request("GET", path, **kw)

    def post(self, path, **kw):
        return self.request("POST", path, **kw)

    def put(self, path, **kw):
        return self.request("PUT", path, **kw)

    def delete(self, path, **kw):
        return self.request("DELETE", path, **kw)

    # ── 认证 ──────────────────────────────────────────────
    def login(self, phone, password, master_code="000000"):
        """密码登录（frond 前台通道 + 万能码）。成功后 token/user_id 存入自身。"""
        resp = self.post(
            "/user/login",
            frond=True,
            code=master_code,
            json={"phone": phone, "password": password},
        )
        body = resp.json() if resp.status_code == 200 else {}
        if body.get("code") == 20005 and body.get("msg"):
            self.token = body["msg"]  # 后端契约：token 位于 msg 字段
            self.user_id = body["data"]["id"]
        return resp

    def register(self, user: dict, master_code="000000"):
        """注册（FormData user 字段 + frond/code 头，与前端真实流程一致）。"""
        return self.post(
            "/user/register",
            frond=True,
            code=master_code,
            data={"user": json.dumps(user, ensure_ascii=False)},
        )

    # ── 用户 ──────────────────────────────────────────────
    def user_page(self, phone=None, page_num=1, page_size=100):
        """后台用户分页查询（power>=1 专用接口）。"""
        cond = {"phone": phone} if phone else {}
        body = f"pageNum={page_num}&pageSize={page_size}&user={json.dumps(cond)}"
        return self.post(
            "/user/page",
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )

    def find_user_by_phone(self, phone):
        """按手机号精确查找，返回用户 dict 或 None。"""
        resp = self.user_page(phone=phone)
        records = resp.json().get("data", {}).get("records", [])
        return next((u for u in records if u.get("phone") == phone), None)

    def delete_user(self, user_id):
        return self.delete("/user", json=[user_id])

    # ── 商品数据 ──────────────────────────────────────────
    def product_page(self, name=None, page_num=1, page_size=100):
        """产品分页查询（公开接口；按名称模糊过滤）。"""
        cond = {"productName": name} if name else {}
        body = f"pageNum={page_num}&pageSize={page_size}&product={json.dumps(cond)}"
        return self.post(
            "/product/page",
            data=body,
            headers={"Content-Type": "application/x-www-form-urlencoded"},
        )

    def find_product_by_name(self, name):
        """按产品名精确查找（先用模糊查询再精确匹配），返回 dict 或 None。"""
        resp = self.product_page(name=name)
        records = resp.json().get("data", {}).get("records", [])
        return next((p for p in records if p.get("productName") == name), None)

    def product_detail(self, product_id):
        """产品详情（公开接口），返回 data dict。"""
        return self.get(f"/product/{product_id}").json().get("data")

    def land_detail(self, land_id):
        """土地详情（公开接口），返回 data dict。"""
        return self.get(f"/land/{land_id}").json().get("data")

    # ── 订单 ──────────────────────────────────────────────
    def add_order(self, pid, num, uid=None):
        """下单。客户路径 uid 由服务端强制为本人；员工/管理员需显式传 uid。"""
        order = {"pid": pid, "num": num}
        if uid is not None:
            order["uid"] = uid
        return self.post("/order", json=order)

    def my_orders(self, uid=None):
        """查询订单列表（POST /order/order）。返回订单 dict 列表（键为小写 pid/uid）。"""
        body = {"id": uid if uid is not None else 0}
        resp = self.post("/order/order", json=body)
        return resp.json().get("data", [])

    def find_order_by_pid(self, pid, uid=None):
        """按商品 id 在本人订单中查找，返回订单 dict 或 None。"""
        return next((o for o in self.my_orders(uid) if o.get("pid") == pid), None)

    def delete_order(self, order_id):
        return self.delete(f"/order/{order_id}")

    def mark_order_paid(self, order_ids, status=1, pay_time=True):
        """后台批量更新订单状态（PUT /order，query 参数，admin 专用）。"""
        ids = order_ids if isinstance(order_ids, list) else [order_ids]
        query = "&".join(f"ids={i}" for i in ids)
        return self.put(f"/order?{query}&status={status}&time={str(pay_time).lower()}")

    # ── 支付 ──────────────────────────────────────────────
    def pay(self, order_ids, total_amount=None):
        """发起支付（POST /alipay/pay，form 参数）。totalAmount 为客户端金额（应被忽略）。"""
        ids = order_ids if isinstance(order_ids, list) else [order_ids]
        data = [("orderIds", i) for i in ids]
        if total_amount is not None:
            data.append(("totalAmount", total_amount))
        return self.post("/alipay/pay", data=data)
