"""产品接口用例（ProductController）。与土地同构，但只有一个图片字段。"""
import allure
import pytest


@allure.feature("产品")
class TestProductPublicQuery:
    @allure.story("产品分页")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_product_page_structure(self, anon):
        resp = anon.product.page(page_num=1, page_size=5)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004

        page = body["data"]
        for field in ("records", "total", "size", "current"):
            assert field in page, f"分页响应缺少字段 {field}"

        if page["records"]:
            record = page["records"][0]
            for field in ("id", "productName", "price"):
                assert field in record, f"产品列表记录缺少字段 {field}"

    @allure.story("分页参数生效")
    @pytest.mark.p1
    def test_product_page_size_respected(self, anon):
        page = anon.product.page(page_num=1, page_size=2).json()["data"]
        assert page["size"] == 2
        assert len(page["records"]) <= 2

    @allure.story("产品详情与列表一致")
    @pytest.mark.p0
    def test_product_detail_matches_list(self, anon, first_product_id):
        listed = next(
            r for r in anon.product.page(page_size=10).json()["data"]["records"]
            if r["id"] == first_product_id
        )
        detail = anon.product.get_by_id(first_product_id).json()["data"]

        assert detail["id"] == listed["id"]
        assert detail["productName"] == listed["productName"], (
            f"详情与列表的产品名称不一致：{detail['productName']!r} vs {listed['productName']!r}"
        )

    @allure.story("详情：不存在的 id")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：GET /product/{id} 传入不存在的 id 返回 HTTP 500，"
               "与 /land/{id} 同源（空列表取 get(0) 抛 IndexOutOfBoundsException）。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_product_detail_not_found_should_not_500(self, anon):
        resp = anon.product.get_by_id(99_999_999)
        assert resp.status_code != 500, (
            f"查询不存在的产品返回了 500，应改为 404 或业务错误码：{resp.text[:200]}"
        )
