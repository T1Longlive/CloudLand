"""边界/鉴权探测服务：专门用于验证"拦截器"这一层，而不是业务逻辑。

为什么单独一个 service 而不是让用例直接用 client：
用例层直连驱动层会破坏 `tests → services → common` 的依赖方向，
下次换驱动（比如 requests 换 httpx）就要改用例。

为什么可以不传参数就探测：
拦截器 preHandle 在 Spring 参数绑定**之前**执行。所以缺 token 一定是 401、
权限不足一定是 403，压根走不到参数校验。这也让权限矩阵测试可以零数据写入、毫秒级完成 —— 
不用真的造出一个"合法的删除土地请求"才能验证它被拦住。
"""
import allure


class AuthzService:
    def __init__(self, client):
        self.client = client

    def probe(self, method: str, path: str, *, token: str = None):
        """按给定方法/路径发一个"无参数"请求，只用于观察拦截器返回什么。

        :param token: 需要临时替换成另一个 token 时传入（不传则用 client 自带的）
        """
        headers = {}
        if token is not None:
            headers["token"] = token
        with allure.step(f"探测 {method} {path}"):
            return self.client.request(method, path, headers=headers)
