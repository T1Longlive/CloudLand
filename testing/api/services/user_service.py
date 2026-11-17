"""用户业务层：对应后端 UserController（除认证外的部分）。

同样是"只拼参数、不做断言"。注意几个容易踩的入参形式差异：
- POST /user/page      → 表单参数（@RequestParam user 是一段 JSON 字符串）
- POST /user/id        → JSON body，直接就是一个数字（@RequestBody Long）
- DELETE /user         → JSON body，是数字数组（@RequestBody Integer[]）
- PUT  /user           → 表单参数 user + 可选文件
把"哪个接口用什么形式传参"这一层知识关在这个文件里，用例层就不需要记这些。
"""
import json


class UserService:
    def __init__(self, client):
        self.client = client

    def page(self, user: dict = None, page_num: int = 1, page_size: int = 5):
        """后台用户分页查询（ADMIN_RULES，需 power>=1）。"""
        return self.client.post(
            "/user/page",
            data={
                "pageNum": page_num,
                "pageSize": page_size,
                # 不传条件时传空对象，避免后端 JSON.parseObject("") 抛异常
                "user": json.dumps(user or {}),
            },
        )

    def get_by_id(self, user_id: int):
        """按 id 查单个用户（ADMIN_RULES，响应含手机号/邮箱等敏感字段）。"""
        return self.client.post("/user/id", json=user_id)

    def employee_list(self):
        """取所有 power=2 的用户（历史命名，实际筛的是管理员）。"""
        return self.client.post("/user/employee")

    def update(self, user: dict, *, frond: bool = False, contact: bool = False):
        """修改用户资料。

        :param frond: 前台模式。开启后后端会强制把 id 改回登录者自己，
                      并要求 password 字段是"当前密码"用于身份确认。
        :param contact: 改绑手机/邮箱模式（仅在 frond 下有意义）。
        """
        headers = {}
        if frond:
            headers["frond"] = "true"
        if contact:
            headers["contact"] = "true"
        return self.client.put(
            "/user",
            data={"user": json.dumps(user, ensure_ascii=False)},
            headers=headers,
        )

    def update_password(self, old_password: str, new_password: str):
        """修改自己的密码。用户 id 取自登录态，请求体里传 id 也不会生效（防越权）。"""
        return self.client.put(
            "/user/password",
            json={"oldPassword": old_password, "newPassword": new_password},
        )

    def delete(self, ids):
        """批量删除用户（ADMIN_RULES，需 power>=1）。"""
        return self.client.delete("/user", json=list(ids))

    def my_trolley(self, user_id: int):
        """查看某人的购物车（前台共享接口，由 Service 层按归属校验）。"""
        return self.client.post("/user/trolley", json={"id": user_id})

    def add_trolley(self, product_id: int, user_id: int, number: int = 1):
        """加购物车。键名全小写 pid/uid/num（Jackson 对 Lombok pId/uId 的绑定契约）。"""
        return self.client.post(
            "/user/addTrolley",
            json={"pid": product_id, "uid": user_id, "num": number},
        )

    def delete_trolley(self, trolley_id: int):
        return self.client.delete(f"/user/trolley/{trolley_id}")
