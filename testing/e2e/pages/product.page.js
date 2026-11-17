/**
 * 产品详情页（/productInfo/:id）。
 *
 * 加购逻辑在 ProductInfoView.addTrolley()：
 * - choose=0 加入购物车 → 成功后弹 $confirm「是否前往购物车页面?」
 * - choose=1 立即购买 → 直接跳购物车
 * 商品已在购物车里时只弹 warning，不重复加——这条分支以后要单独覆盖。
 */
class ProductPage {
  constructor(page) {
    this.page = page;
    this.title = page.locator('.productInfo-msg-1 p');
    this.addToCartButton = page.getByRole('button', { name: '加入购物车' });
    this.buyNowButton = page.getByRole('button', { name: '立即购买' });
    this.confirmBox = page.locator('.el-message-box');
  }

  async goto(productId) {
    await this.page.goto(`/productInfo/${productId}`);
    await this.title.waitFor({ state: 'visible' });
  }

  /** 产品名（断言页面确实加载了目标产品，避免"打开了但 404"的假通过） */
  async productName() {
    return (await this.title.innerText()).trim();
  }

  async addToCart() {
    await this.addToCartButton.click();
  }

  /** 「是否前往购物车页面?」→ 确定 */
  async confirmGoToTrolley() {
    await this.confirmBox.waitFor({ state: 'visible' });
    await this.confirmBox.getByRole('button', { name: '确定' }).click();
    await this.page.waitForURL('**/user/myTrolley');
  }
}

module.exports = { ProductPage };
