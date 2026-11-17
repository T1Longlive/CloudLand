/**
 * 购物车（/user/myTrolley）。
 *
 * 表格是 el-table，"行"没有业务 id 可挂，官方结构类 .el-table__row 属于组件的稳定契约
 * （Element UI 2.x 的大版本内不会变），比我们自己数 div 靠谱。
 */
class TrolleyPage {
  constructor(page) {
    this.page = page;
    this.rows = page.locator('.el-table__row');
    this.emptyText = page.locator('.el-table__empty-text');
    this.totalPrice = page.locator('.link-btn3');
    this.checkoutButton = page.getByRole('button', { name: '去结算' });
  }

  async waitLoaded() {
    await this.page.waitForURL('**/user/myTrolley');
    // 表格数据是 mounted 里发请求拿的，出现行或出现空态都算加载完成
    await this.page
      .locator('.el-table__row, .el-table__empty-text')
      .first()
      .waitFor({ state: 'visible' });
  }

  rowByProductName(name) {
    return this.rows.filter({ hasText: name }).first();
  }

  /** 勾选某一行：点 label 而不是隐藏的原生 input（原生 input 不可见，点不动） */
  async selectRow(name) {
    const row = this.rowByProductName(name);
    await row.waitFor({ state: 'visible' });
    await row.locator('.el-checkbox').click();
  }

  async checkout() {
    await this.checkoutButton.click();
  }
}

module.exports = { TrolleyPage };
