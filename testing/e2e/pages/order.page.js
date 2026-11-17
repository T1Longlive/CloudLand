/**
 * 我的订单（/user/myOrder）。
 *
 * 注意这里的等待不是"可以加 sleep"的借口：
 * MyTrolley.addOrder() 用了两段写死的 setTimeout(2000)，再跳转再 loading 2000。
 * 也就是说从点「去结算」到订单页可用，硬成本就有 4 秒以上。
 * 所以我们用 waitForURL + 行可见作为**条件等待**，而不是 sleep 一个拍脑袋的数字——
 * 将来前端把 setTimeout 改成 Promise，用例不用改。
 */
class OrderPage {
  constructor(page) {
    this.page = page;
    this.rows = page.locator('.el-table__row');
    this.emptyText = page.locator('.el-table__empty-text');
  }

  async waitLoaded() {
    await this.page.waitForURL('**/user/myOrder', { timeout: 20_000 });
    await this.page
      .locator('.el-table__row, .el-table__empty-text')
      .first()
      .waitFor({ state: 'visible', timeout: 20_000 });
  }

  rowByProductName(name) {
    return this.rows.filter({ hasText: name }).first();
  }
}

module.exports = { OrderPage };
