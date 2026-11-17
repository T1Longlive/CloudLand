"""土地业务层：对应 LandController。

只封装**不需要上传文件**的接口（公开查询 + 分页）。
新增/修改土地要求 multipart 上传 landFiles/imageFiles，会把文件写到本地磁盘，
属于"有副作用、难清理"的用例，单独在 Stage 4 用临时存储目录来做，这里不做。
"""
import json


class LandService:
    def __init__(self, client):
        self.client = client

    def get_by_id(self, land_id: int):
        """土地详情（公开接口，拦截器白名单 GET /land/{id}）。"""
        return self.client.get(f"/land/{land_id}")

    def page(self, land: dict = None, page_num: int = 1, page_size: int = 5):
        """土地分页（公开接口，拦截器白名单 POST /land/page）。"""
        return self.client.post(
            "/land/page",
            data={
                "pageNum": page_num,
                "pageSize": page_size,
                "land": json.dumps(land or {}, ensure_ascii=False),
            },
        )

    def delete(self, ids):
        """批量删除土地（ADMIN_RULES）。"""
        return self.client.delete("/land", json=list(ids))
