"""订单业务层：对应 OrderController。

订单是本项目最容易出越权问题的地方（"我的订单"按 userId 查），
所以这里的注释重点写清"哪个参数代表身份、谁来决定它"。
"""
import json


class OrderService:
    def __init__(self, client):
        self.client = client

    def my_orders(self, user_id: int):
        """查某人的订单（前台共享接口 POST /order/order）。

        注意入参形式：后端直接 @RequestBody User，只用到里面的 id。
        这意味着 userId 完全由调用方给，归属校验必须靠 Service 层，是重点测点。
        """
        return self.client.post("/order/order", json={"id": user_id})

    def create(self, p_id: int, num: int, user_id: int = None):
        """下单（POST /order，@RequestBody Order2）。num=-1 表示土地订单。

        不传 user_id 时由服务端按登录态回填（客户）；员工/管理员可代录。
        注意：下单会锁地/扣库存，调用方必须负责删单回滚或清理。
        键名用全小写 pid/uid：Lombok 的 pId/uId 经 Jackson 绑定后就是小写。
        """
        body = {"pid": p_id, "num": num}
        if user_id is not None:
            body["uid"] = user_id
        return self.client.post("/order", json=body)

    def page(self, order: dict = None, page_num: int = 1, page_size: int = 5):
        """后台订单分页（ADMIN_RULES，需 power>=1）。"""
        return self.client.post(
            "/order/page",
            data={
                "pageNum": page_num,
                "pageSize": page_size,
                "order": json.dumps(order or {}, ensure_ascii=False),
            },
        )

    def update_status(self, ids, status: int, time: bool = False):
        """批量改订单状态（ADMIN_RULES）。"""
        return self.client.put(
            "/order",
            data=[("ids", i) for i in list(ids)] + [("status", status), ("time", str(time).lower())],
        )

    def delete(self, order_id: int):
        """删除订单（前后台共享接口 DELETE /order/{id}）。"""
        return self.client.delete(f"/order/{order_id}")
