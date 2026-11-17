"""支付业务层：对应 AlipayController。

只封装 /alipay/pay（本地签名构造表单，不联网、不碰回调）。
notify/return 依赖公网可达与真实沙箱付款，不在自动化范围内。
"""


class AlipayService:
    def __init__(self, client):
        self.client = client

    def pay(self, order_ids, *, total_amount=None, user_id=None):
        """发起页面支付。orderIds 是表单数组参数（@RequestParam Integer[]）。

        total_amount/user_id 故意可传：用于"篡改金额/身份"的防篡改用例，
        服务端应忽略它们并重算/取登录态。
        """
        fields = [("orderIds", i) for i in list(order_ids)]
        if total_amount is not None:
            fields.append(("totalAmount", str(total_amount)))
        if user_id is not None:
            fields.append(("userId", str(user_id)))
        return self.client.post("/alipay/pay", data=fields)