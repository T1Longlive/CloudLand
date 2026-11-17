"""产品业务层：对应 ProductController。与 LandService 同构，只封装无文件上传的接口。"""
import base64
import json

# 1x1 透明 PNG：新增产品接口强制要一张图（@RequestParam MultipartFile productImg），
# 用最小合法文件，不必在仓库里存二进制素材。
_TINY_PNG = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=="
)


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

    def create(self, product: dict):
        """新增产品（ADMIN_RULES，需 power>=1）。

        入参形式与后端签名对齐：product 是 **JSON 字符串的表单字段**，
        productImg 是文件字段。写成 json= 会直接 400 —— 和注册接口同一个坑。
        """
        return self.client.post(
            "/product",
            data={"product": json.dumps(product, ensure_ascii=False)},
            files={"productImg": ("test.png", _TINY_PNG, "image/png")},
        )

    def delete(self, ids):
        """批量删除产品（ADMIN_RULES）。"""
        return self.client.delete("/product", json=list(ids))
