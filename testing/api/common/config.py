"""配置层：所有环境差异收敛在这一个文件。

为什么单独一层：
- 用例里出现 `http://localhost:9090` 就是硬编码，换环境要全局搜索替换。
- 这里只读一次环境变量，其它层统一 `from common.config import settings`。

优先级：环境变量 > 代码默认值。CI 通过 env 注入即可切换环境，不改代码。
"""
import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    base_url: str = os.environ.get("TEST_API_BASE_URL", "http://localhost:9090/api")
    # 本项目测试环境的万能验证码；生产环境必须关闭（CODE_MASTER_ENABLED=false）
    master_code: str = os.environ.get("TEST_MASTER_CODE", "000000")
    timeout: int = int(os.environ.get("TEST_HTTP_TIMEOUT", "10"))
    # 支付交易映射断言/清理用；与后端 spring.redis 默认值一致
    redis_host: str = os.environ.get("TEST_REDIS_HOST", "localhost")
    redis_port: int = int(os.environ.get("TEST_REDIS_PORT", "6379"))
    redis_password: str = os.environ.get("TEST_REDIS_PASSWORD", "123456")
    redis_db: int = int(os.environ.get("TEST_REDIS_DB", "0"))
    # 支付宝密钥：notify 用例要用私钥自签回调报文，详见 common/alipay_signer.py
    # CI 由 ci.yml 现场生成一对；本地 .env 是真实沙箱凭据（两把不一定配对）
    alipay_private_key: str = os.environ.get("ALIPAY_PRIVATE_KEY", "")
    alipay_public_key: str = os.environ.get("ALIPAY_PUBLIC_KEY", "")


settings = Settings()

# seed 账号：与数据库 199 段账号一致，密码统一 Test@123456
# power: 0=客户 / 1=员工 / 2=管理员（后端没有角色表，权限靠这个字段）
SEED_ACCOUNTS = {
    "customer": {"phone": "19900000001", "password": "Test@123456", "power": 0},
    "employee": {"phone": "19900000002", "password": "Test@123456", "power": 1},
    "admin": {"phone": "19900000003", "password": "Test@123456", "power": 2},
}
