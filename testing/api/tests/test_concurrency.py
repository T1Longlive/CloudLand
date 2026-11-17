"""并发用例：库存扣减的竞态。

为什么必须单独一组
------------------
`addOrder` 的扣库存是教科书级的「读 - 判断 - 写」：

    product = productMapper.selectById(pid)          // 读 num
    if (product.num < order.num) return 拒绝
    product.setNum(product.num - order.num)          // 用"读到的值"算新值
    productMapper.updateById(product)                // 写回绝对值

两个并发事务都读到 10、都算出 9、都写 9 —— 卖了 2 件只扣了 1 件（丢失更新）；
库存只有 1 件时两个请求都通过判断 —— 超卖。单线程用例 100% 漏，
人工点页面也点不出来，只有并发能暴露。

设计要点（这几条决定了它是"稳定复现"还是"偶尔抽风"）：

1. **屏障同步起跑**：线程是排队创建的，不设 Barrier 根本撞不上；
2. **多轮重复**：单轮可能刚好串行执行、看起来一切正常。连续 N 轮把
   "撞上竞态窗口"的概率叠上去，也让结论更接近真实并发；
3. **每轮独立产品**：不复用库存，避免上一轮的脏数据影响下一轮判断；
4. **断言不变量，不断言"必须失败"**：
   成功数 ≤ 初始库存、最终库存 == 初始库存 - 成功数、最终库存 ≥ 0。
   这三条任何一条破了都是缺陷，与调度顺序无关。
"""
import threading
import uuid
from concurrent.futures import ThreadPoolExecutor

import allure
import pytest

from common.config import settings
from common.http_client import HttpClient

# 并发轮次与线程数：线程数要明显大于库存，才容易在同一窗口读到同一个值
OVERSELL_ROUNDS = 5
OVERSELL_THREADS = 10
LOST_UPDATE_ROUNDS = 3
LOST_UPDATE_THREADS = 8


def _owner_account_id(anon) -> int:
    """产品的 a_id 是 NOT NULL 外键，取一个已存在的合作商 id。

    Jackson 把 Lombok 的 aId 序列化成全小写 aid（和订单的 pid/uid 同一规律），
    两个都接一下，避免后端命名微调就把造数打死。
    """
    records = anon.product.page(page_size=1).json()["data"]["records"]
    assert records, "库里没有任何产品，无法推导 a_id"
    record = records[0]
    return record.get("aId") or record.get("aid")


def _find_product_id(anon, name: str) -> int:
    records = anon.product.page({"productName": name}, page_size=10).json()["data"]["records"]
    hit = next((r for r in records if r["productName"] == name), None)
    assert hit, f"产品已创建但回查不到：{name}"
    return hit["id"]


@pytest.fixture
def product_factory(admin, anon):
    """造一个指定库存的可售产品，用例结束删除（服务端图片文件一并清理）。"""
    created = []
    owner_id = _owner_account_id(anon)

    def _make(stock: int):
        name = f"并发测试-{uuid.uuid4().hex[:8]}"
        resp = admin.product.create(
            {
                "productName": name,
                "description": "并发用例造数，跑完即删",
                "ordered": 0,
                "price": 1.0,
                "status": 1,
                "aId": owner_id,
                "num": stock,
                "img": "",
            }
        )
        assert resp.json()["code"] == 10001, f"造产品失败：{resp.text[:300]}"
        pid = _find_product_id(anon, name)
        created.append(pid)
        return pid, name

    yield _make

    for pid in created:
        try:
            admin.product.delete([pid])
        except Exception as exc:  # 清理异常不能吃掉测试结果
            print(f"[cleanup] 产品 {pid} 删除失败：{exc}")


def _fire_concurrent_orders(token: str, pid: int, threads: int):
    """N 条线程在同一刻向同一个产品下单，每单 1 件。

    每条线程独立 HttpClient：requests.Session 不保证线程安全，
    共享 session 在并发下会出现连接池竞争，测出来的失败可能来自测试自己。
    """
    barrier = threading.Barrier(threads)
    results = []

    def fire():
        client = HttpClient(settings.base_url, settings.timeout)
        client.token = token
        try:
            barrier.wait(timeout=15)  # 一起起跑，最大化撞车概率
            return client.post("/order", json={"pid": pid, "num": 1}).json().get("code")
        finally:
            client.session.close()

    with ThreadPoolExecutor(max_workers=threads) as pool:
        futures = [pool.submit(fire) for _ in range(threads)]
        results = [f.result() for f in futures]
    return results


def _stock_of(anon, pid: int) -> float:
    return float(anon.product.get_by_id(pid).json()["data"]["num"])


def _cleanup_orders(customer, pid: int):
    """删掉本轮产生的订单（未支付 → 硬删，库存回滚）。"""
    for record in customer.order.my_orders(0).json()["data"]:
        if record.get("pId") == pid:
            customer.order.delete(record["id"])


@allure.feature("并发")
class TestStockRace:
    """这两个缺陷为什么不修，而是记 xfail —— 想清楚再动手

    测试的职责是**证明缺陷存在并把它变成可回归的信号**，不是顺手改业务代码。
    改了三行代码把用例弄绿，等于把"这里曾经有并发漏洞"这件事从历史里抹掉，
    下一个接手的人只会看到一段"本来就对"的代码。

    修复方向（给研发的输入，不是测试自己动手）：
    把"读-算-写回绝对值"换成原子操作，二选一：
      UPDATE product SET num = num - #{n} WHERE id = #{pid} AND num >= #{n}
      然后看 affectedRows：0 就是库存不足，直接拒绝；
    或者 SELECT ... FOR UPDATE 把行锁住再判断。
    两者都要求"判断"和"扣减"发生在同一条 SQL / 同一个锁里。
    """

    @allure.story("库存只有 1 件时并发下单不能超卖")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：并发下单超卖。库存 1 件时 10 路并发有 8 单成功。"
               "根因是 Order2ServiceImpl.addOrder 的『读 num → 判断 → 写回 num-1』非原子，"
               "并发的 SELECT 都不加锁，读到同一个旧值就都通过判断。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_concurrent_orders_must_not_oversell(
        self, customer_client, anon, customer, product_factory
    ):
        initial_stock = 1
        violations = []

        for round_no in range(1, OVERSELL_ROUNDS + 1):
            pid, _ = product_factory(stock=initial_stock)
            codes = _fire_concurrent_orders(customer_client.token, pid, OVERSELL_THREADS)
            succeeded = sum(1 for code in codes if code == 10001)
            final_stock = _stock_of(anon, pid)

            with allure.step(f"第 {round_no} 轮：成功 {succeeded} 单，剩余库存 {final_stock}"):
                if succeeded > initial_stock:
                    violations.append(
                        f"第{round_no}轮超卖：库存 {initial_stock} 件，成功下单 {succeeded} 单"
                    )
                if final_stock < 0:
                    violations.append(f"第{round_no}轮库存为负：{final_stock}")
                if final_stock > initial_stock - succeeded + 1e-9:
                    violations.append(
                        f"第{round_no}轮库存未按成功数扣减：初始 {initial_stock}、"
                        f"成功 {succeeded}、剩余 {final_stock}"
                        f"（应剩 {initial_stock - succeeded}）"
                    )

            _cleanup_orders(customer, pid)

        assert not violations, "并发下单破坏了库存不变量：\n  " + "\n  ".join(violations)

    @allure.story("库存充足时并发下单不能丢失扣减")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：并发下单丢失更新。库存 20、8 路并发全部成交，库存却只扣了 1~2。"
               "同样是写回绝对值（num = 读到的值 - n）导致的 lost update，"
               "与超卖同源，只是库存充足时表现为少扣而不是多卖。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_concurrent_orders_must_not_lose_stock_updates(
        self, customer_client, anon, customer, product_factory
    ):
        initial_stock = 20
        violations = []

        for round_no in range(1, LOST_UPDATE_ROUNDS + 1):
            pid, _ = product_factory(stock=initial_stock)
            codes = _fire_concurrent_orders(customer_client.token, pid, LOST_UPDATE_THREADS)
            succeeded = sum(1 for code in codes if code == 10001)
            final_stock = _stock_of(anon, pid)

            with allure.step(f"第 {round_no} 轮：成功 {succeeded} 单，剩余库存 {final_stock}"):
                if succeeded != LOST_UPDATE_THREADS:
                    violations.append(
                        f"第{round_no}轮有 {LOST_UPDATE_THREADS - succeeded} 单被误拒"
                        f"（库存 {initial_stock} 件、仅 {LOST_UPDATE_THREADS} 单）"
                    )
                if abs((initial_stock - succeeded) - final_stock) > 1e-9:
                    violations.append(
                        f"第{round_no}轮丢失更新：初始 {initial_stock}、成功 {succeeded}、"
                        f"剩余 {final_stock}（应剩 {initial_stock - succeeded}）"
                    )

            _cleanup_orders(customer, pid)

        assert not violations, "并发下单破坏了库存扣减的原子性：\n  " + "\n  ".join(violations)
