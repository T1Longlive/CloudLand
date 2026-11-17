/**
 * 下单链路：加购物车 → 购物车勾选 → 去结算 → 订单列表可见。
 *
 * 这是全站唯一"写库 + 跨三个页面 + 有硬编码等待"的链路，也是 E2E 真正无可替代的地方：
 * 接口测试能证明 /order 会写一行记录，但证明不了"用户点完这一串按钮之后，那行记录会出现在他眼前"。
 *
 * 数据隔离：下单用一条用例专属的临时账号 + 一个用例专属的产品（fixtures 里造、跑完删）。
 * 绝不拿 seed 客户下单——它一有历史订单，"订单列表里有这一行"就不再是有效断言。
 */
const { test, expect } = require('../fixtures');
const { MASTER_CODE } = require('../fixtures/env');
const { HomePage } = require('../pages/home.page');
const { LoginFormPage } = require('../pages/login-form.page');
const { ProductPage } = require('../pages/product.page');
const { TrolleyPage } = require('../pages/trolley.page');
const { OrderPage } = require('../pages/order.page');

test.describe('下单', () => {
  test('加购物车 → 结算 → 订单出现在我的订单', async ({ page, tempCustomer, stockedProduct }) => {
    const home = new HomePage(page);
    const loginForm = new LoginFormPage(page);
    const product = new ProductPage(page);
    const trolley = new TrolleyPage(page);
    const order = new OrderPage(page);

    // 1. 登录（客户账号，必须走真实 UI —— 登录态、sessionStorage.userID 都是加购的前置）
    await home.open();
    await home.openLoginForm();
    await loginForm.login({
      phone: tempCustomer.phone,
      password: tempCustomer.password,
      code: MASTER_CODE,
    });
    await expect(home.userGreeting).toContainText(tempCustomer.username);

    // 2. 进产品详情，确认加载的是造出来的那个产品（防"打开了但 404"的假通过）
    await product.goto(stockedProduct.id);
    expect(await product.productName()).toContain(stockedProduct.productName);

    // 3. 加入购物车 → 弹「是否前往购物车页面?」→ 确定
    await product.addToCart();
    await product.confirmGoToTrolley();

    // 4. 购物车：勾选这一行，然后去结算
    await trolley.waitLoaded();
    await expect(trolley.rowByProductName(stockedProduct.productName)).toBeVisible();
    await trolley.selectRow(stockedProduct.productName);
    await trolley.checkout();

    // 5. 订单页：这一行必须出现，且是"未支付"（说明订单真的落库了，不是前端假数据）
    await order.waitLoaded();
    const orderRow = order.rowByProductName(stockedProduct.productName);
    await expect(orderRow).toBeVisible();
    await expect(orderRow).toContainText('未支付');
  });
});
