# -*- coding: utf-8 -*-
"""订单业务层：下单、查询、删除、后台改状态。"""


class OrderService:
    def __init__(self, http):
        self.http = http

    def add_order(self, pid, num, uid=None):
        """下单。客户路径 uid 由服务端强制为本人；员工/管理员需显式传 uid。"""
        order = {"pid": pid, "num": num}
        if uid is not None:
            order["uid"] = uid
        return self.http.post("/order", json=order)

    def my_orders(self, uid=None):
        """查询订单列表（POST /order/order）。返回订单 dict 列表（键为小写 pid/uid）。"""
        body = {"id": uid if uid is not None else 0}
        resp = self.http.post("/order/order", json=body)
        return resp.json().get("data", [])

    def find_order_by_pid(self, pid, uid=None):
        """按商品 id 在本人订单中查找，返回订单 dict 或 None。"""
        return next((o for o in self.my_orders(uid) if o.get("pid") == pid), None)

    def delete_order(self, order_id):
        return self.http.delete(f"/order/{order_id}")

    def mark_order_paid(self, order_ids, status=1, pay_time=True):
        """后台批量更新订单状态（PUT /order，query 参数，admin 专用）。"""
        ids = order_ids if isinstance(order_ids, list) else [order_ids]
        query = "&".join(f"ids={i}" for i in ids)
        return self.http.put(f"/order?{query}&status={status}&time={str(pay_time).lower()}")
