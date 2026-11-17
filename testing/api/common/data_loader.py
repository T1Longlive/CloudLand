"""数据层：把用例输入与期望从代码里剥离。

好处：
- 加一条登录场景只改 YAML，不动 Python。
- 产品/测试非开发也能补数据。
- 数据文件进代码评审，改了什么一目了然。
"""
from pathlib import Path

import yaml

DATA_DIR = Path(__file__).resolve().parents[1] / "data"


def load_yaml(relative_path: str):
    """从 testing/api/data/ 下加载 YAML。"""
    path = DATA_DIR / relative_path
    if not path.exists():
        raise FileNotFoundError(f"数据文件不存在：{path}")
    with path.open(encoding="utf-8") as f:
        return yaml.safe_load(f)
