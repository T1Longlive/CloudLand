# -*- coding: utf-8 -*-
"""驱动层：只负责发请求、附加 token、处理滑动续期、记录 Allure 附件。

这一层不写业务。业务动作（登录/下单/支付）放到 services/ 下。
"""
import allure
import requests


class HttpClient:
    def __init__(self, base_url, timeout=10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.token = None
        self.user_id = None

    def request(self, method, path, *, frond=False, code=None, **kwargs):
        """发请求并处理 token 续期。

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

        self._attach(method, path, resp)
        return resp

    @staticmethod
    def _attach(method, path, resp):
        body = resp.text or ""
        if len(body) > 800:
            body = body[:800] + "...(truncated)"
        allure.attach(
            f"{method} {path}\nstatus={resp.status_code}\n{body}",
            name="http-exchange",
            attachment_type=allure.attachment_type.TEXT,
        )

    def get(self, path, **kw):
        return self.request("GET", path, **kw)

    def post(self, path, **kw):
        return self.request("POST", path, **kw)

    def put(self, path, **kw):
        return self.request("PUT", path, **kw)

    def delete(self, path, **kw):
        return self.request("DELETE", path, **kw)
