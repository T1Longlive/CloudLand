/**
 * 登录链路。
 *
 * 为什么登录值得单独一组：它是所有其它链路的前置，一旦它脆，整套 E2E 都不可信。
 * 而这条链路上真实踩过两个坑，都用例固化下来：
 * 1. 客户账号（power=0）登录必须带 `frond` 头，历史上漏传导致全部客户登录不上；
 * 2. 登录表单里验证码是必填项（前端 checkFrom 强制），只填密码根本发不出请求。
 */
const { test, expect } = require('../fixtures');
const { MASTER_CODE } = require('../fixtures/env');
const { HomePage } = require('../pages/home.page');
const { LoginFormPage } = require('../pages/login-form.page');

test.describe('登录', () => {
  test('客户账号密码正确 → 首页右上角显示用户名', async ({ page, seedCustomer }) => {
    const home = new HomePage(page);
    const form = new LoginFormPage(page);

    await home.open();
    await home.openLoginForm();

    await form.login({
      phone: seedCustomer.phone,
      password: seedCustomer.password,
      code: MASTER_CODE,
    });

    // 登录成功的判据放在"登录态变成可见"上，而不是等浮层——浮层 1 秒就消失，
    // 拿它当断言条件会变成竞态。
    await expect(home.userGreeting).toContainText(seedCustomer.username);
    await expect(form.root).toBeHidden();
  });

  test('密码错误 → 提示密码错误且不进入登录态', async ({ page, seedCustomer }) => {
    const home = new HomePage(page);
    const form = new LoginFormPage(page);

    await home.open();
    await home.openLoginForm();
    await form.login({
      phone: seedCustomer.phone,
      password: 'Wrong@123456',
      code: MASTER_CODE,
    });

    // 后端 PASSWORD_ERR 的文案原样透出到提示里
    expect(await form.notificationText()).toContain('密码错误');
    // 表单仍在，说明没有跳进登录态
    await expect(form.root).toBeVisible();
  });

  test('登录后可进入个人中心（frond 请求头的回归点）', async ({ page, seedCustomer }) => {
    const home = new HomePage(page);
    const form = new LoginFormPage(page);

    await home.open();
    await home.openLoginForm();
    await form.login({
      phone: seedCustomer.phone,
      password: seedCustomer.password,
      code: MASTER_CODE,
    });
    await expect(home.userGreeting).toContainText(seedCustomer.username);

    await page.goto('/user');
    await expect(page).toHaveURL(/\/user$/);
    await expect(page.getByText('个人中心', { exact: true })).toBeVisible();
  });

  test('未登录访问个人中心 → 被路由守卫弹回首页', async ({ page }) => {
    // 这条不测登录，测的是"守卫真的在拦"。守卫失效时上面的用例全绿，用户却能裸奔。
    await page.goto('/user');

    await expect(page).toHaveURL(/\/$/);
    await expect(page.getByText('请先在首页右上方登录!')).toBeVisible();
  });
});
