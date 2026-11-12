# -*- coding: utf-8 -*-
"""支付业务层：发起支付。"""


class PayService:
    def __init__(self, http):
        self.http = http

    def pay(self, order_ids, total_amount=None):
        """发起支付（POST /alipay/pay，form 参数）。totalAmount 为客户端金额（应被忽略）。"""
        ids = order_ids if isinstance(order_ids, list) else [order_ids]
        data = [("orderIds", i) for i in ids]
        if total_amount is not None:
            data.append(("totalAmount", total_amount))
        return self.http.post("/alipay/pay", data=data)
