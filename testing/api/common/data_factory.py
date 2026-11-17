"""数据工厂：造"不撞车"的测试数据。

为什么必须有这一层：
- 注册接口要求手机号/邮箱唯一，写死 13400000001 第二次跑就报"已注册"，
  测试变成"只能跑一次"的废物。
- 用例之间互相污染，是最常见的"顺序一变就红"的来源。

本项目的手机号分段约定（跟 seed 脚本、E2E 脚本共享，别乱改）：
- 199 段：固定 seed 账号，只读，测试不新增不修改
- 134 段：接口测试临时账号
- 131/132 段：E2E 临时账号

Stage 4 会把"自动清理"补上；现阶段用时间戳后缀保证唯一，不会撞库。
"""
import time

# 接口测试临时账号段
API_TEMP_PREFIX = "134"

# 造数据用的统一密码，长度/复杂度满足后端无强校验，但保持与 seed 一致便于排查
DEFAULT_PASSWORD = "Test@123456"


def unique_suffix() -> str:
    """返回 8 位数字后缀。用毫秒时间戳截断，同一秒内多次调用也不会重复。"""
    return str(int(time.time() * 1000))[-8:]


def temp_phone(prefix: str = API_TEMP_PREFIX) -> str:
    """生成一个未被占用的 11 位手机号，形如 134 + 8 位后缀。"""
    return f"{prefix}{unique_suffix()}"


def temp_user(phone: str = None) -> dict:
    """生成一份可直接提交给注册接口的用户数据（后端字段名，不是前端字段名）。

    `detailedAddress` 必须带：数据库里该列是 NOT NULL 且没有默认值，
    少传会直接抛 DataIntegrityViolationException 变成 HTTP 500。
    详见 tests/test_auth.py 里记录该缺陷的 xfail 用例。
    """
    phone = phone or temp_phone()
    return {
        "username": f"测试用户{phone[-4:]}",
        "phone": phone,
        "password": DEFAULT_PASSWORD,
        "mail": f"tmp-{phone}@cloudland.test",
        "age": 20,
        "address": "测试地址",
        "detailedAddress": "某省某市某区某路 1 号",
    }
