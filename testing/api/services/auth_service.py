# -*- coding: utf-8 -*-
"""认证业务层：登录、注册。只关心业务动作，不关心 HTTP 细节。"""
import json


class AuthService:
    def __init__(self, http):
        self.http = http

    def login(self, phone, password, master_code="000000"):
        """密码登录（frond 前台通道 + 万能码）。成功后 token/user_id 写入 HttpClient。"""
        resp = self.http.post(
            "/user/login",
            frond=True,
            code=master_code,
            json={"phone": phone, "password": password},
        )
        body = resp.json() if resp.status_code == 200 else {}
        if body.get("code") == 20005 and body.get("msg"):
            self.http.token = body["msg"]  # 后端契约：token 位于 msg 字段
            self.http.user_id = body["data"]["id"]
        return resp

    def register(self, user: dict, master_code="000000"):
        """注册（FormData user 字段 + frond/code 头，与前端真实流程一致）。"""
        return self.http.post(
            "/user/register",
            frond=True,
            code=master_code,
            data={"user": json.dumps(user, ensure_ascii=False)},
        )
