"""码表契约测试：后端 Code.java 与前端 code.js 是两份手工镜像，漂移即事故。

为什么值得独立一层：新增/修改状态码时只要有一端忘改，前端就会把成功当失败
（或反之），而编译期与单端单测都发现不了。这里纯源码正则解析，**不需要起服务**，
0.0x 秒跑完，天然适合放在 CI 的最快反馈位。

解析约定（与源码格式绑定，格式大改时需同步更新正则）：
- Java 常量行：``public static final Integer NAME = 数字;``
- JS 常量行：两空格缩进的 ``NAME: 数字,``（排除 ``[Code.X]: '文案'`` 映射行）
- 白名单：Java ``code.equals(NAME)`` / JS ``code === Code.NAME``
- 文件用 utf-8-sig 读取，兼容 BOM
"""
import re
from pathlib import Path

import allure
import pytest

pytestmark = pytest.mark.smoke

ROOT = Path(__file__).resolve().parents[3]
JAVA_CODE = ROOT / "src" / "main" / "java" / "com" / "cloudland" / "controller" / "result" / "Code.java"
JS_CODE = ROOT / "vue" / "src" / "constants" / "code.js"


def _read(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def _java_codes() -> dict:
    return {m.group(1): int(m.group(2))
            for m in re.finditer(r"public static final Integer (\w+) = (-?\d+);", _read(JAVA_CODE))}


def _js_codes() -> dict:
    return {m.group(1): int(m.group(2))
            for m in re.finditer(r"^  (\w+): (-?\d+),", _read(JS_CODE), re.M)}


def _java_whitelist() -> set:
    return set(re.findall(r"code\.equals\((\w+)\)", _read(JAVA_CODE)))


def _js_whitelist() -> set:
    return set(re.findall(r"code === Code\.(\w+)", _read(JS_CODE)))


def _js_message_keys() -> set:
    return set(re.findall(r"\[Code\.(\w+)\]:", _read(JS_CODE)))


@allure.feature("契约")
class TestCodeTableContract:
    @allure.title("两端码表键集合一致：无缺失、无多余")
    def test_key_sets_match(self):
        java, js = _java_codes(), _js_codes()
        assert java, "未解析到任何 Java 常量，正则或源码格式已变"
        assert js, "未解析到任何 JS 常量，正则或源码格式已变"
        assert set(java) == set(js), (
            f"码表键漂移：仅后端有 {set(java) - set(js)}，仅前端有 {set(js) - set(java)}"
        )

    @allure.title("同名状态码逐键取值一致")
    def test_values_match_per_key(self):
        java, js = _java_codes(), _js_codes()
        diff = {k: (java[k], js[k]) for k in set(java) & set(js) if java[k] != js[k]}
        assert not diff, f"同名码取值不一致（后端, 前端）：{diff}"

    @allure.title("isSuccess 白名单双端一致且引用合法")
    def test_success_whitelist_matches_both_ends(self):
        java_codes, js_codes = _java_codes(), _js_codes()
        java_wl, js_wl = _java_whitelist(), _js_whitelist()
        assert java_wl <= set(java_codes), f"Java 白名单引用了不存在的常量：{java_wl - set(java_codes)}"
        assert js_wl <= set(js_codes), f"JS 白名单引用了不存在的常量：{js_wl - set(js_codes)}"
        assert java_wl == js_wl, (
            f"成功白名单漂移：仅后端 {java_wl - js_wl}，仅前端 {js_wl - java_wl}"
        )
        assert {java_codes[n] for n in java_wl} == {js_codes[n] for n in js_wl}

    @allure.title("前端 getCodeMessage 覆盖全部状态码")
    def test_message_map_covers_all_codes(self):
        missing = set(_js_codes()) - _js_message_keys()
        assert not missing, f"这些码没有前端文案映射，调试时会显示未知状态码：{missing}"

    @allure.title("码表无重值：一个数值只对应一个语义")
    def test_no_duplicate_values(self):
        for name, codes in (("Java", _java_codes()), ("JS", _js_codes())):
            dup = {v for v in codes.values() if list(codes.values()).count(v) > 1}
            assert not dup, f"{name} 码表存在重值：{dup}"

    @allure.title("锚点常量锁定：SUCCESS=0 / FAILURE=-1")
    def test_anchor_constants(self):
        for name, codes in (("Java", _java_codes()), ("JS", _js_codes())):
            assert codes["SUCCESS"] == 0, f"{name} SUCCESS 锚点被改动"
            assert codes["FAILURE"] == -1, f"{name} FAILURE 锚点被改动"