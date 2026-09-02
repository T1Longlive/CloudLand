# -*- coding: utf-8 -*-
"""契约测试：前后端业务状态码码表一致性（纯源码解析，不依赖后端服务）。

- 后端：src/main/java/com/cloudland/controller/result/Code.java
- 前端：vue/src/constants/code.js

防漂移价值：两端码表靠人工同步，新增/改名状态码极易只改一端，
导致前端拦截器与组件的业务码判断失效。本文件固化四层契约：
键集合、同名取值、isSuccess 成功白名单、getCodeMessage 消息映射。
"""
import re
from pathlib import Path

import allure

# 仓库根目录（testing/api/ 向上两级）
ROOT = Path(__file__).resolve().parents[2]
JAVA_FILE = ROOT / "src" / "main" / "java" / "com" / "cloudland" / "controller" / "result" / "Code.java"
JS_FILE = ROOT / "vue" / "src" / "constants" / "code.js"


def _read(path: Path) -> str:
    assert path.is_file(), f"源码文件不存在: {path}"
    # utf-8-sig 兼容带 BOM 的源文件
    return path.read_text(encoding="utf-8-sig")


def parse_java_codes() -> dict:
    """解析 Code.java 的常量定义 → {名称: 数值}。"""
    text = _read(JAVA_FILE)
    pairs = re.findall(r"public\s+static\s+final\s+Integer\s+(\w+)\s*=\s*(-?\d+)\s*;", text)
    assert pairs, "未从 Code.java 解析到任何状态码常量（正则与源码结构不匹配？）"
    return {name: int(value) for name, value in pairs}


def parse_js_codes() -> dict:
    """解析 code.js 的 Code 对象属性 → {名称: 数值}。

    只匹配两空格缩进的 `NAME: 数值,` 属性行，
    排除 getCodeMessage 中 `[Code.X]: '文案'` 的映射行。
    """
    text = _read(JS_FILE)
    pairs = re.findall(r"^  (\w+):\s*(-?\d+),\s*$", text, flags=re.M)
    assert pairs, "未从 code.js 解析到任何状态码常量（正则与源码结构不匹配？）"
    return {name: int(value) for name, value in pairs}


def parse_java_success_whitelist() -> set:
    """解析 Code.java isSuccess() 中的成功白名单 → {名称}。"""
    text = _read(JAVA_FILE)
    names = set(re.findall(r"code\.equals\((\w+)\)", text))
    assert names, "未从 Code.java 解析到 isSuccess 白名单"
    return names


def parse_js_success_whitelist() -> set:
    """解析 code.js isSuccess() 中的成功白名单 → {名称}。"""
    text = _read(JS_FILE)
    names = set(re.findall(r"code\s*===\s*Code\.(\w+)", text))
    assert names, "未从 code.js 解析到 isSuccess 白名单"
    return names


def parse_js_message_map() -> set:
    """解析 code.js getCodeMessage() 码表映射的键 → {名称}。"""
    text = _read(JS_FILE)
    names = set(re.findall(r"\[Code\.(\w+)\]:", text))
    assert names, "未从 code.js 解析到 getCodeMessage 消息映射"
    return names


@allure.epic("API 自动化层")
@allure.feature("契约测试")
class TestCodeTableContract:
    """前后端状态码码表契约用例。"""

    @allure.story("码表结构")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("码表键集合一致：Java 与 JS 无缺失、无多余")
    def test_key_sets_match(self):
        java_codes = parse_java_codes()
        js_codes = parse_js_codes()
        java_only = set(java_codes) - set(js_codes)
        js_only = set(js_codes) - set(java_codes)
        assert not java_only and not js_only, (
            f"码表键集合漂移: 仅 Java 存在={sorted(java_only)}, "
            f"仅 JS 存在={sorted(js_only)}"
        )

    @allure.story("码表结构")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("同名状态码取值一致（逐键比对）")
    def test_values_match(self):
        java_codes = parse_java_codes()
        js_codes = parse_js_codes()
        diff = {
            name: (java_codes[name], js_codes[name])
            for name in set(java_codes) & set(js_codes)
            if java_codes[name] != js_codes[name]
        }
        assert not diff, f"同名状态码取值漂移: {diff}"

    @allure.story("行为语义")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("isSuccess 成功白名单两端一致")
    def test_success_whitelist_match(self):
        java_wl = parse_java_success_whitelist()
        js_wl = parse_js_success_whitelist()
        assert java_wl == js_wl, (
            f"isSuccess 白名单漂移: 仅 Java 存在={sorted(java_wl - js_wl)}, "
            f"仅 JS 存在={sorted(js_wl - java_wl)}"
        )

    @allure.story("行为语义")
    @allure.title("isSuccess 白名单中的名称均存在于码表")
    def test_success_whitelist_names_exist(self):
        java_codes = parse_java_codes()
        for whitelist in (parse_java_success_whitelist(), parse_js_success_whitelist()):
            unknown = whitelist - set(java_codes)
            assert not unknown, f"isSuccess 白名单引用了码表外的名称: {sorted(unknown)}"

    @allure.story("前端映射")
    @allure.title("getCodeMessage 消息映射覆盖全部状态码")
    def test_message_map_covers_all(self):
        js_codes = parse_js_codes()
        mapped = parse_js_message_map()
        unmapped = set(js_codes) - mapped
        surplus = mapped - set(js_codes)
        assert not unmapped and not surplus, (
            f"消息映射与码表不一致: 未映射={sorted(unmapped)}, "
            f"映射了不存在项={sorted(surplus)}"
        )

    @allure.story("码表结构")
    @allure.title("码表内部无重值（不同业务含义不得共用数值）")
    def test_no_duplicate_values(self):
        for source, codes in (("Code.java", parse_java_codes()), ("code.js", parse_js_codes())):
            value_to_names: dict = {}
            for name, value in codes.items():
                value_to_names.setdefault(value, []).append(name)
            duplicates = {v: names for v, names in value_to_names.items() if len(names) > 1}
            assert not duplicates, f"{source} 存在重复取值: {duplicates}"

    @allure.story("锚点常量")
    @allure.title("锚点常量取值锁定：SUCCESS=0 / FAILURE=-1")
    def test_anchor_constants(self):
        for source, codes in (("Code.java", parse_java_codes()), ("code.js", parse_js_codes())):
            assert codes.get("SUCCESS") == 0, f"{source} SUCCESS 应为 0"
            assert codes.get("FAILURE") == -1, f"{source} FAILURE 应为 -1"
