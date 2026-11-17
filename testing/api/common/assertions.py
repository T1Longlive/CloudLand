"""断言层：把"HTTP 层"和"业务层"分开判。

这个项目有两套状态，只判一套就会漏：
1. HTTP 状态码：401 缺 token / 402 权限变更 / 403 账号禁用或权限不足，由拦截器返回；
   正常业务响应 HTTP 恒为 200。
2. 业务码 `code`：0 成功、10001 新增成功、20005 登录成功、30003 验证码错误……

只断言 HTTP 200 → 漏掉业务失败（密码错也是 200）。
只断言 code → 漏掉鉴权拦截（401 时没有 body）。

所以每个用例都该按需调用 assert_http + assert_code，两个维度都要有。
"""

# 后端 Code.isSuccess() 的白名单（与 Code.java 保持一致）
SUCCESS_CODES = {0, 10001, 10002, 10003, 10004, 20005, 20006, 30001}


def assert_http(resp, expected: int = 200, message: str = ""):
    """断言 HTTP 状态码。"""
    detail = message or f"HTTP 状态码不符，响应：{resp.text[:500]}"
    assert resp.status_code == expected, detail


def assert_code(resp, expected: int, message: str = ""):
    """断言响应体里的业务码。"""
    body = resp.json()
    detail = message or f"业务码应为 {expected}，实际 {body.get('code')}，msg={body.get('msg')}"
    assert body.get("code") == expected, detail


def assert_success(resp):
    """断言业务成功（业务码落在白名单内）。"""
    body = resp.json()
    assert body.get("code") in SUCCESS_CODES, (
        f"期望成功，实际 code={body.get('code')} msg={body.get('msg')}：{resp.text[:500]}"
    )
