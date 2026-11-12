# -*- coding: utf-8 -*-
"""商品/土地业务层：分页查询、按名称查找、详情。"""
import json


class CatalogService:
    def __init__(self, http):
        self.http = http

    def product_page(self, name=None, page_num=1, page_size=100):
        """产品分页查询（公开接口；按名称模糊过滤）。"""
        cond = {"productName": name} if name else {}
        body = f"pageNum={page_num}&pageSize={page_size}&product={json.dumps(cond)}"
        return self.http.post(
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
        return self.http.get(f"/product/{product_id}").json().get("data")

    def land_detail(self, land_id):
        """土地详情（公开接口），返回 data dict。"""
        return self.http.get(f"/land/{land_id}").json().get("data")
