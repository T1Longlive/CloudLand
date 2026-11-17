"""消息业务层：对应 MsgController（留言 / 订阅 / 后台推送）。

⚠️ 危险接口：POST /msg/push 会给**所有订阅邮箱真实发信**。
这里不提供"正常调用"的方法，只提供 authz 探测入口，
避免有人顺手在用例里调它，把测试环境的邮箱全轰一遍。
"""
import json


class MsgService:
    def __init__(self, client):
        self.client = client

    # ---------- 公开接口（拦截器白名单） ----------
    def leave_message(self, name: str, mail: str, phone: str, content: str):
        """提交留言（公开）。同一手机号超过 10 条会被拒绝（ADD_ERR）。"""
        return self.client.post(
            "/msg",
            json={"name": name, "mail": mail, "phone": phone, "msg": content},
        )

    def subscribe(self, mail: str):
        """订阅邮件（公开）。重复订阅返回 ADD_ERR。"""
        return self.client.post("/msg/mail", json={"mail": mail})

    # ---------- 后台接口（ADMIN_RULES） ----------
    def page(self, msg: dict = None, page_num: int = 1, page_size: int = 5):
        """后台留言分页。"""
        return self.client.post(
            "/msg/page",
            data={
                "pageNum": page_num,
                "pageSize": page_size,
                "msg": json.dumps(msg or {}, ensure_ascii=False),
            },
        )

    def push_without_params(self):
        """只用于权限探测：故意不传 pushMsg，被参数校验挡在业务执行之前。

        绝不要补上 pushMsg —— 那会真的群发邮件。
        """
        return self.client.post("/msg/push")
