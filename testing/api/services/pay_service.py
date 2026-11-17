"""支付业务层：对应 AlipayController。

覆盖 /alipay/pay 与 /alipay/notify：
- pay：本地签名构造表单，不联网；
- notify：**需要自己签一份合法报文**（见 common/alipay_signer.py），
  它是全站唯一免 JWT 的写接口，安全性全靠 RSA2 验签，必须有用例守着。
- /alipay/return 仍不在范围内：它依赖真实沙箱交易查询（要联网打支付宝）。
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

    def notify(self, params: dict):
        """投递一份异步通知（表单编码）。

        返回的是**纯文本** `success` / `failure`，不是统一 Result 结构：
        支付宝只认这两个字符串，返回 JSON 会一直重试。
        调用时不要带 token —— 支付宝也不可能带，用裸客户端（anon）才是真实条件。
        """
        return self.client.post("/alipay/notify", data=params)