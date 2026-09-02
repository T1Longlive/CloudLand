# -*- coding: utf-8 -*-
"""requests 封装：token 头自动附加 + updatedToken 滑动续期。

对应前端 src/request/interceptor.js 的行为契约：
- 请求头 `token` 携带 JWT
- 响应头 `updatedToken` 出现时自动替换本地 token（滑动续期）
- HTTP 401/402/403 语义由调用方结合 assertions 判断
"""
import json

import requests


class ApiClient:
    def __init__(self, base_url, timeout=10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.token = None

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

    # ── 业务语义封装 ──────────────────────────────────────
    def login(self, phone, password, master_code="000000"):
        """密码登录（frond 前台通道 + 万能码）。成功后 token 存入 self.token。"""
        resp = self.post(
            "/user/login",
            frond=True,
            code=master_code,
            json={"phone": phone, "password": password},
        )
        body = resp.json() if resp.status_code == 200 else {}
        if body.get("code") == 20005 and body.get("msg"):
            self.token = body["msg"]  # 后端契约：token 位于 msg 字段
        return resp

    def register(self, user: dict, master_code="000000"):
        """注册（FormData user 字段 + frond/code 头，与前端真实流程一致）。"""
        return self.post(
            "/user/register",
            frond=True,
            code=master_code,
            data={"user": json.dumps(user, ensure_ascii=False)},
        )

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
