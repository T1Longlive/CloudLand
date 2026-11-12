# -*- coding: utf-8 -*-
"""用户业务层：分页查询、按手机号查找、删除。"""
import json


class UserService:
    def __init__(self, http):
        self.http = http

    def user_page(self, phone=None, page_num=1, page_size=100):
        """后台用户分页查询（power>=1 专用接口）。"""
        cond = {"phone": phone} if phone else {}
        body = f"pageNum={page_num}&pageSize={page_size}&user={json.dumps(cond)}"
        return self.http.post(
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
        return self.http.delete("/user", json=[user_id])
