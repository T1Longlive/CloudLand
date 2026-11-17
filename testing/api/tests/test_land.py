"""土地接口用例（LandController）。

土地是本项目唯一带"两套文件"（landFiles 合同附件 + imageFiles 展示图）的实体，
新增/修改都要 multipart 上传，且会把文件写到本地磁盘目录。
所以这里只覆盖：
- 公开查询（前台首页、土地列表、详情）—— 用户最常走的路径；
- 分页契约；
- 不存在 id 的异常行为（发现了一个真实缺陷）；
写接口留给 Stage 3/4 用独立临时存储目录再测，避免把本地磁盘写脏。
"""
import allure
import pytest


@allure.feature("土地")
class TestLandPublicQuery:
    @allure.story("土地分页")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_land_page_structure(self, anon):
        """土地分页返回结构满足前端列表页契约。"""
        resp = anon.land.page(page_num=1, page_size=5)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004

        page = body["data"]
        for field in ("records", "total", "size", "current"):
            assert field in page, f"分页响应缺少字段 {field}"

        if page["records"]:
            record = page["records"][0]
            # 列表页要渲染这些字段，少一个就是空白卡片
            for field in ("id", "landName", "price", "address"):
                assert field in record, f"土地列表记录缺少字段 {field}"

    @allure.story("分页参数生效")
    @pytest.mark.p1
    def test_land_page_size_respected(self, anon):
        page = anon.land.page(page_num=1, page_size=2).json()["data"]
        assert page["size"] == 2
        assert len(page["records"]) <= 2

    @allure.story("土地详情与列表一致")
    @pytest.mark.p0
    def test_land_detail_matches_list(self, anon, first_land_id):
        """详情接口返回的字段必须与列表里同一条记录一致。

        这类"同一份数据两个接口"的一致性检查很值钱：
        后端常常在 VO 层给列表和详情写了两套 SQL，改了一边忘了另一边，
        前端就会"列表显示名字、点进去空白"。
        """
        listed = next(
            r for r in anon.land.page(page_size=10).json()["data"]["records"]
            if r["id"] == first_land_id
        )
        detail = anon.land.get_by_id(first_land_id).json()["data"]

        assert detail["id"] == listed["id"]
        assert detail["landName"] == listed["landName"], (
            f"详情与列表的土地名称不一致：{detail['landName']!r} vs {listed['landName']!r}"
        )

    @allure.story("详情：不存在的 id")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：GET /land/{id} 传入不存在的 id 返回 HTTP 500。"
               "根因是 LandServiceImpl.selectById 里 selectPage(1,1,land).getRecords().get(0)，"
               "空列表取下标抛 IndexOutOfBoundsException。"
               "期望返回 404 或业务码，修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_land_detail_not_found_should_not_500(self, anon):
        """不存在的资源应明确返回"没有"，而不是 5xx。

        为什么这算缺陷而不是小事：
        1. 5xx 意味着服务端异常，监控会当成故障告警，真实故障被噪音淹没；
        2. 任何人用 /land/1 扫一遍就能触发大量异常日志（放大攻击面）；
        3. 前端拿不到 404 就无法区分"加载失败"和"这条数据被删了"。
        """
        resp = anon.land.get_by_id(99_999_999)
        assert resp.status_code != 500, (
            f"查询不存在的土地返回了 500，应改为 404 或业务错误码：{resp.text[:200]}"
        )
