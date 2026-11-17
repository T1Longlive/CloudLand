"""支付宝 RSA2 签名工具：在测试侧构造"签名正确"的回调报文。

为什么必须自己签名
------------------
`POST /alipay/notify` 是全站**唯一不经过 JWT 鉴权**的写接口（见 WebMvcConfig 的
免认证白名单），因为支付宝服务器不可能带我们的 token。它的安全性 100% 建立在
`AlipaySignature.rsaCheckV1` 验签上——所以：

- 不构造合法签名 → 只能测出"非法请求被拒"，测不出"合法请求真的会改单"；
- 构造了合法签名 → 才能断言「验签通过 → 订单状态翻转 → 映射被消费」这条主链路。

⚠️ 密钥为什么会"有时不可用"
--------------------------
后端用 `alipay.publicKey` 校验回调，测试必须用**与之配对的私钥**签名：

- CI：`ci.yml` 用 openssl 现场生成一对再注入，私钥/公钥同源，必然配对；
- 本地 `.env`：是真实沙箱凭据，`ALIPAY_PRIVATE_KEY` 是**应用私钥**，
  `ALIPAY_PUBLIC_KEY` 是**支付宝平台公钥**——它们本来就不是一对。
  拿应用私钥签名，后端用平台公钥验，必然失败。

所以这里提供 `signing_pair_usable()`：签一段探针再用公钥验回来。
不配对就由用例 skip 并说明原因——绝不能让它变成一条"本地必红"的假故障。

本文件只做协议计算，不发请求、不做断言（分层约定同其它 common/ 模块）。
"""
import base64
from datetime import datetime

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding

from common.config import settings

# 参与签名的字段集合里必须排除的两个：sign 本身、以及 sign_type（支付宝约定）
_SIGN_EXCLUDED = {"sign", "sign_type"}


def _load_private_key(raw: str):
    """兼容两种存法：PEM 全文，或「PKCS8 DER 的 base64」（SDK 文档里的写法）。"""
    if not raw:
        raise ValueError("缺少 ALIPAY_PRIVATE_KEY")
    raw = raw.strip()
    if "BEGIN" in raw:
        return serialization.load_pem_private_key(raw.encode("utf-8"), password=None)
    return serialization.load_der_private_key(base64.b64decode(raw), password=None)


def _load_public_key(raw: str):
    if not raw:
        raise ValueError("缺少 ALIPAY_PUBLIC_KEY")
    raw = raw.strip()
    if "BEGIN" in raw:
        return serialization.load_pem_public_key(raw.encode("utf-8"))
    return serialization.load_der_public_key(base64.b64decode(raw))


def sign_content(params: dict) -> str:
    """待签串：按 key 升序、剔除 sign/sign_type 与空值，再拼 `k=v&k=v`。

    与 Java 侧 `AlipaySignature.getSignContent` 的规则逐条对齐：
    一旦这里差一个空值或排序规则，验签就永远 false，而且日志只会说"验签失败"。
    """
    items = sorted(
        (k, v)
        for k, v in params.items()
        if k not in _SIGN_EXCLUDED and v is not None and str(v) != ""
    )
    return "&".join(f"{k}={v}" for k, v in items)


def sign_params(params: dict) -> dict:
    """给参数集补上 sign（RSA2 = SHA256withRSA + base64），返回新字典。"""
    private_key = _load_private_key(settings.alipay_private_key)
    content = sign_content(params)
    signature = private_key.sign(
        content.encode("utf-8"), padding.PKCS1v15(), hashes.SHA256()
    )
    signed = dict(params)
    signed.setdefault("sign_type", "RSA2")
    signed["sign"] = base64.b64encode(signature).decode("ascii")
    return signed


def signing_pair_usable() -> bool:
    """私钥签、公钥验，签得回来才说明这对密钥可用。

    用途：本地 `.env` 的应用私钥 / 平台公钥不是一对，用例据此 skip 而不是红。
    """
    if not settings.alipay_private_key or not settings.alipay_public_key:
        return False
    try:
        probe = {"probe": "cloudland-notify", "sign_type": "RSA2"}
        signed = sign_params(probe)
        public_key = _load_public_key(settings.alipay_public_key)
        public_key.verify(
            base64.b64decode(signed["sign"]),
            sign_content(probe).encode("utf-8"),
            padding.PKCS1v15(),
            hashes.SHA256(),
        )
        return True
    except (InvalidSignature, ValueError, TypeError):
        return False


def _now() -> str:
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")


def build_notify_params(
    out_trade_no: str,
    *,
    trade_status: str = "TRADE_SUCCESS",
    total_amount: str = "0.01",
    **overrides,
) -> dict:
    """造一份贴近真实的异步通知参数（未签名）。

    字段按支付宝 trade_status_sync 通知摘取常用项，不含 app_id/seller_id 之类
    本项目并不校验的字段——多写只会让"哪些字段真的参与验签"变得看不清。
    """
    params = {
        "notify_type": "trade_status_sync",
        "notify_id": f"TEST{int(datetime.now().timestamp() * 1000)}",
        "notify_time": _now(),
        "charset": "UTF-8",
        "version": "1.0",
        "sign_type": "RSA2",
        "out_trade_no": out_trade_no,
        "trade_no": f"2026{int(datetime.now().timestamp() * 1000)}",
        "trade_status": trade_status,
        "total_amount": total_amount,
        "receipt_amount": total_amount,
        "buyer_pay_amount": total_amount,
        "subject": "云用地订单支付",
        "gmt_create": _now(),
        "gmt_payment": _now(),
    }
    params.update(overrides)
    return params
