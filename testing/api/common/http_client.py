"""驱动层：唯一直接使用 requests 的地方。

职责边界（记牢这三条，框架就不会烂）：
1. 只负责"怎么发"：连接复用、token 注入、超时、日志、报告附件。
2. 不判断业务是否成功 —— 那是 assertions 的活。
3. 不拼业务参数 —— 那是 services 的活。

为什么用 Session 而不是 requests.get：
Session 复用 TCP 连接与 Cookie，几十条用例能省掉重复握手，跑得更快。
"""
import logging
import time

import requests

try:
    import allure
except ImportError:  # 报告库可选，缺失时不影响测试执行
    allure = None

logger = logging.getLogger(__name__)


class HttpClient:
    def __init__(self, base_url: str, timeout: int = 10):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        self.session = requests.Session()
        self.token = None  # 登录成功后由 services 层写入

    def request(self, method: str, path: str, *, headers: dict = None, **kwargs):
        final_headers = {"Accept": "application/json"}
        if headers:
            final_headers.update(headers)
        if self.token:
            # 本项目的认证头名是 token，不是 Authorization: Bearer
            final_headers["token"] = self.token

        url = f"{self.base_url}{path}"
        started = time.perf_counter()
        resp = self.session.request(
            method, url, headers=final_headers, timeout=self.timeout, **kwargs
        )
        elapsed_ms = (time.perf_counter() - started) * 1000

        logger.info("%s %s -> HTTP %s (%.1fms)", method, url, resp.status_code, elapsed_ms)
        self._attach(method, url, resp, elapsed_ms)
        return resp

    def get(self, path, **kwargs):
        return self.request("GET", path, **kwargs)

    def post(self, path, **kwargs):
        return self.request("POST", path, **kwargs)

    def put(self, path, **kwargs):
        return self.request("PUT", path, **kwargs)

    def delete(self, path, **kwargs):
        return self.request("DELETE", path, **kwargs)

    @staticmethod
    def _attach(method, url, resp, elapsed_ms):
        """把请求/响应挂到 Allure 报告里，失败时不用再翻日志。

        无用例上下文时 allure.attach 会抛错，所以要包住。
        """
        if allure is None:
            return
        try:
            allure.attach(
                f"{method} {url}\n耗时 {elapsed_ms:.1f}ms\nHTTP {resp.status_code}",
                name="请求摘要",
                attachment_type=allure.attachment_type.TEXT,
            )
            allure.attach(
                resp.text[:4000],
                name="响应体",
                attachment_type=allure.attachment_type.JSON,
            )
        except Exception:
            pass
