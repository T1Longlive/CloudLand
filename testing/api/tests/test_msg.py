"""留言/订阅接口用例（MsgController）。

这个模块有一条隐形业务规则容易漏：**同一个手机号最多留 10 条**，
超过返回 ADD_ERR 并带上"你的留言已超过10条"。
这条规则如果没测，前端就只会在用户点提交后一脸茫然 —— 因为 code 不是成功码但也没提示。

注意：本文件不测 POST /msg/push（群发邮件），也不新增留言数据。
留言没有删除接口，写进去就清不掉，所以在共享库上只做"只读 + 已被拒绝"的验证。
"""
import allure
import pytest

# 单手机号留言上限（与 MsgController.msg 里的 msgs.size() > 9 对应）
MESSAGE_LIMIT = 10


def _phones_with_message_count(employee, page_size: int = 100) -> dict:
    """从后台分页接口拉全量留言，按手机号聚合条数。

    为什么不直接查数据库：用例一旦直连 DB，就多了一份库连接配置要维护，
    而且换环境（Docker/CI 里的 MySQL）时最容易坏。
    能通过 API 拿到的数据，就不要用 DB 拿。
    """
    page = employee.msg.page(page_size=page_size).json()["data"]
    counts = {}
    for record in page["records"]:
        phone = record.get("phone")
        if phone:
            counts[phone] = counts.get(phone, 0) + 1
    return counts


@allure.feature("消息")
class TestMsgPage:
    @allure.story("后台留言分页")
    @pytest.mark.smoke
    @pytest.mark.p0
    def test_msg_page_structure(self, employee):
        resp = employee.msg.page(page_num=1, page_size=5)
        assert resp.status_code == 200
        body = resp.json()
        assert body["code"] == 10004

        page = body["data"]
        for field in ("records", "total", "size", "current"):
            assert field in page, f"留言分页响应缺少字段 {field}"
        assert len(page["records"]) <= 5

        if page["records"]:
            for field in ("id", "name", "phone", "msg", "sendTime"):
                assert field in page["records"][0], f"留言记录缺少字段 {field}"


@allure.feature("消息")
class TestMessageLimit:
    @allure.story("同一手机号超过 10 条留言被拒绝")
    @pytest.mark.p1
    def test_message_limit_enforced(self, employee, anon):
        """用一个"已经饱和"的手机号再提交一条，必须被拒绝。

        为什么用现有饱和数据而不是自己造 10 条：
        留言没有删除接口，自己造 10 条就永久留在库里，越跑越脏。
        借用已饱和的手机号，既验证了边界规则，又零写入。
        """
        counts = _phones_with_message_count(employee)
        saturated = [p for p, c in counts.items() if c >= MESSAGE_LIMIT]
        if not saturated:
            pytest.skip("库中没有留言数达到上限的手机号，无法验证限流规则")

        phone = saturated[0]
        resp = anon.msg.leave_message(
            name="自动化测试", mail=f"tmp-{phone}@cloudland.test", phone=phone,
            content="超过上限的留言，应被拒绝",
        )
        body = resp.json()

        assert body["code"] == 10005, (
            f"手机号 {phone} 已有 {counts[phone]} 条留言，"
            f"再提交应被拒绝（10005），实际 code={body['code']} msg={body.get('msg')}"
        )


@allure.feature("消息")
class TestMsgSearch:
    @allure.story("留言搜索：按姓名可用")
    @pytest.mark.p1
    def test_msg_page_filter_by_name_works(self, employee):
        """用库里真实存在的姓名做模糊搜索，结果必须全部命中。

        这条是"对照组"：先证明分页的过滤机制本身是通的，
        后面那条缺陷用例（按手机号搜不到）才有说服力 ——
        否则无法区分"过滤整体坏了"和"只有某个字段没实现"。
        """
        records = employee.msg.page(page_size=50).json()["data"]["records"]
        names = [r["name"] for r in records if r.get("name")]
        if not names:
            pytest.skip("库中没有带姓名的留言，无法验证搜索")

        needle = names[0]
        page = employee.msg.page({"name": needle}, page_size=50).json()["data"]

        assert page["records"], f"按姓名 {needle!r} 搜索无结果，但库里确实存在该姓名"
        assert all(needle in r["name"] for r in page["records"]), (
            f"按姓名 {needle!r} 搜索返回了不匹配的记录："
            f"{[r['name'] for r in page['records']]}"
        )

    @allure.story("留言搜索：按手机号失效")
    @pytest.mark.p1
    @pytest.mark.xfail(
        strict=True,
        reason="缺陷记录：POST /msg/page 的 phone 条件被静默忽略。"
               "MsgMapper.selectByCondition 只拼了 id 和 name 两个 <if>，"
               "phone/mail/msg 三个条件根本没实现，导致后台按手机号搜索返回全部数据。"
               "修复后本条 XPASS（strict 转红）提示更新用例。",
    )
    def test_msg_page_filter_by_phone_returns_nothing(self, employee):
        """用一个库里绝不存在的手机号搜索，必须返回 0 条。

        为什么这是缺陷而不是"没实现就算了"：
        返回全部数据比返回 0 条更危险 —— 后台运营以为"这个号码没留过言"，
        实际是搜索功能失效，会据此做出错误判断（比如重复回复、漏跟客户）。
        静默忽略查询条件，本质上是在给用户返回错误答案。
        """
        page = employee.msg.page({"phone": "10000000000"}, page_size=50).json()["data"]
        assert page["total"] == 0, (
            f"按不存在的手机号搜索，返回了 {page['total']} 条数据，"
            "说明 phone 查询条件被忽略了"
        )
