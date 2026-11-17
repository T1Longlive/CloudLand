"""门禁脚本：确认支付宝回调用例真的**执行**了，而不是被 skip 掉。

为什么需要这段代码
------------------
pytest 里 skip 不改变退出码：三条回调用例全 skip，CI 一样是绿的。
而这套用例的 skip 条件（密钥是否配对）恰好取决于环境变量——
"环境没配好"和"功能没问题"在 CI 结论上长得一模一样，这是最典型的假绿。

规则：
- 有 failed/error → 退出 1；
- 有 skipped → 退出 1（skip 等于没覆盖）；
- 用例数少于预期 → 退出 1（防止文件被误删/改名后静默失去覆盖）。

用法：
    pytest --junitxml=junit.xml
    python scripts/check_notify_coverage.py junit.xml [期望条数=3]
"""
import sys
import xml.etree.ElementTree as ET

TARGET = "test_alipay_notify"


def main() -> int:
    if len(sys.argv) < 2:
        print("用法: python scripts/check_notify_coverage.py <junit.xml> [期望条数]")
        return 2
    xml_path = sys.argv[1]
    expected = int(sys.argv[2]) if len(sys.argv) > 2 else 3

    tree = ET.parse(xml_path)
    cases = [
        case
        for case in tree.iter("testcase")
        if TARGET in (case.get("classname") or "")
    ]

    failed = [c.get("name") for c in cases if c.find("failure") is not None or c.find("error") is not None]
    skipped = [c.get("name") for c in cases if c.find("skipped") is not None]

    print(f"回调用例：共 {len(cases)} 条 / 跳过 {len(skipped)} / 失败 {len(failed)}")
    for name in failed:
        print(f"  FAILED  {name}")
    for name in skipped:
        print(f"  SKIPPED {name}")

    if failed:
        print("::error::回调用例失败")
        return 1
    if skipped:
        print("::error::回调用例被 skip —— 等于这条链路没有覆盖（通常是 ALIPAY 密钥没注入或不成对）")
        return 1
    if len(cases) < expected:
        print(f"::error::回调用例只有 {len(cases)} 条，少于预期 {expected} 条")
        return 1
    print("回调用例全部真实执行并通过")
    return 0


if __name__ == "__main__":
    sys.exit(main())
