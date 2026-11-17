"""产品业务层：对应 ProductController。与 LandService 同构，只封装无文件上传的接口。"""
import json


class ProductService:
    def __init__(self, client):
        self.client = client

    def get_by_id(self, product_id: int):
        """产品详情（公开接口，白名单 GET /product/{id}）。"""
        return self.client.get(f"/product/{product_id}")

    def page(self, product: dict = None, page_num: int = 1, page_size: int = 5):
        """产品分页（公开接口，白名单 POST /product/page）。"""
        return self.client.post(
            "/product/page",
            data={
                "pageNum": page_num,
                "pageSize": page_size,
                "product": json.dumps(product or {}, ensure_ascii=False),
            },
        )

    def delete(self, ids):
        """批量删除产品（ADMIN_RULES）。"""
        return self.client.delete("/product", json=list(ids))
