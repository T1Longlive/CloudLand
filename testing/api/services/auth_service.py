"""认证业务层：对应后端 UserController 的认证相关接口。

三层的分工，在这个文件里体现得最清楚：
- 这一层：知道"登录要 POST /user/login、body 是 phone+password、power=0 要带 frond 头"。
- 不写断言：同一个 login() 既能被"登录成功"用例调用，也能被"密码错误"用例调用。
  如果这里写死了 assert code == 20005，反向用例就没法复用了 —— 这是新手最常见的框架设计错误。

后端契约要点（来自 UserController / UserServiceImpl 源码，不是猜的）：
1. 登录成功 code=20005，**JWT 放在 msg 字段**里（不是 data，也不是响应头）。
2. 登录的三种入参通道，靠请求头开关：
   - 带 `token` 头且 body 无 password → token 续期通道
   - 带 `code` 头 → 邮箱验证码通道（按 phone 反查邮箱校验验证码）
   - 都不带 → 普通账号密码通道
3. `frond` 头 = 前台登录标记。power=0（客户）不带这个头会被拦成 20008 权限不足，
   power=1/2 不受影响。所以客户账号登录必须带 `frond: true`。
4. 注册不是 JSON body，而是 `@RequestParam user`（表单字段里塞一段 JSON 字符串）。
"""
import json

from common.config import SEED_ACCOUNTS


class AuthService:
    def __init__(self, client):
        self.client = client

    # ---------- 登录 ----------
    def login(self, phone=None, password=None, *, code=None, frond=True, remember=None):
        """调用登录接口，返回原始 Response（断言由用例层做）。

        :param phone: 手机号
        :param password: 密码；验证码登录时传 None
        :param code: 邮箱验证码；传了才会走验证码通道
        :param frond: 是否前台登录。power=0 的账号必须为 True
        :param remember: 是否记住登录（影响后端 JWT 有效期）
        """
        body = {}
        if phone is not None:
            body["phone"] = phone
        if password is not None:
            body["password"] = password

        headers = {}
        # 请求头只能是字符串，"true"/"false" 后端用 Boolean.parseBoolean 解析
        if frond is not None:
            headers["frond"] = "true" if frond else "false"
        if remember is not None:
            headers["remember"] = "true" if remember else "false"
        if code is not None:
            headers["code"] = code

        return self.client.post("/user/login", json=body, headers=headers)

    def login_by_token(self, token):
        """token 续期通道：只带 token 头、body 不带密码。

        frond 必须带：power=0 的客户在 token 通道同样受前台标记门禁，
        不带会被拦成 20008（与密码通道一致）。
        """
        return self.client.post("/user/login", json={}, headers={"token": token, "frond": "true"})

    # ---------- 注册 ----------
    def register(self, user: dict, *, code=None):
        """注册新用户。user 用后端字段名（username/phone/password/mail/age/address）。

        注意这里是表单提交，不是 json=：后端用 @RequestParam 接收 user 字符串。
        写成 json= 会直接 400，这是本项目最容易踩的坑之一。
        """
        headers = {}
        if code is not None:
            headers["code"] = code
        return self.client.post(
            "/user/register",
            data={"user": json.dumps(user, ensure_ascii=False)},
            headers=headers,
        )

    # ---------- 便捷方法：给其它用例准备"已登录客户端" ----------
    def login_as(self, role: str):
        """用 seed 账号登录，把 token 写回 client 并返回 client 自身。

        role 取 customer / employee / admin。登录失败直接抛断言错误，
        因为"准备数据失败"属于阻塞性故障，不该让下游用例产生一堆迷惑的假失败。
        """
        account = SEED_ACCOUNTS[role]
        resp = self.login(phone=account["phone"], password=account["password"], frond=True)
        body = resp.json()
        if body.get("code") != 20005:
            raise AssertionError(
                f"seed 账号 {role} 登录失败，无法继续后续用例："
                f"HTTP {resp.status_code} code={body.get('code')} msg={body.get('msg')}"
            )
        self.client.token = body["msg"]
        return self.client
