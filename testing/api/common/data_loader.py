# -*- coding: utf-8 -*-
"""测试数据加载：YAML 用例数据与用例代码分离。"""
from pathlib import Path

import yaml

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def load_yaml(name):
    path = DATA_DIR / name
    with path.open("r", encoding="utf-8") as f:
        return yaml.safe_load(f)
