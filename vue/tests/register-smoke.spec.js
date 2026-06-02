// @ts-check
import { test, expect } from '@playwright/test';
import { setVerifyCode, closeRedis } from './helpers/redis.js';
import { getAdminToken } from './helpers/auth.js';
import { deleteUserByPhone } from './helpers/cleanup.js';

const TEST = {
  username: '测试用户',
  phone: '13100000001',
  password: 'test123456',
  age: '25',
  email: '10000000001@qq.com',
  detailedAddress: '测试详细地址001',
  code: '888888',
};

const SEL = {
  registerForm: '.register-form',
  loginForm: '.login-form',
  loginBtn: '#login-btn',
  toRegister: '.login-form .account a',
  username: '.register-form input[placeholder="请输入用户名(2-10个字符)"]',
  phone: '.register-form input[placeholder="请输入手机号"]',
  password: '.register-form input[placeholder="请设置你的密码(6-20位)"]',
  age: '.register-form input[placeholder="请输入你的年龄"]',
  email: '.register-form input[placeholder="请输入QQ邮箱(@qq.com结尾)"]',
  address: '.register-form .el-cascader',
  detailedAddress: '.register-form input[placeholder="请输入你的详细地址"]',
  code: '.register-form input[placeholder="请输入你的验证码"]',
  submit: '.register-form button.link-btn:has-text("确认注册")',
  sendCode: '.register-form .link-btn-mail',
  notification: '.el-notification',
};

async function openRegisterForm(page) {
  await page.goto('/');
  await page.locator(SEL.loginBtn).click();
  await expect(page.locator(SEL.loginForm)).toBeVisible();
  await page.locator(SEL.toRegister).click();
  await expect(page.locator(SEL.registerForm)).toBeVisible();
}

async function selectRegion(page) {
  await page.locator(SEL.address).click();
  await page.locator('.el-cascader-panel .el-cascader-node__label', { hasText: '四川省' }).first().click();
  await page.locator('.el-cascader-panel .el-cascader-node__label', { hasText: '成都市' }).first().click();
  await page.locator('.el-cascader-panel .el-cascader-node__label', { hasText: '锦江区' }).first().click();
}

async function fillValidForm(page) {
  await setVerifyCode(TEST.email, TEST.code);
  await page.locator(SEL.username).fill(TEST.username);
  await page.locator(SEL.phone).fill(TEST.phone);
  await page.locator(SEL.password).fill(TEST.password);
  await page.locator(SEL.age).fill(TEST.age);
  await page.locator(SEL.email).fill(TEST.email);
  await selectRegion(page);
  await page.locator(SEL.detailedAddress).fill(TEST.detailedAddress);
  await page.locator(SEL.code).fill(TEST.code);
}

test.describe('注册冒烟测试', () => {
  test.describe.configure({ mode: 'serial' });

  let adminToken;

  test.beforeAll(async () => {
    adminToken = await getAdminToken();
    await deleteUserByPhone(TEST.phone, adminToken);
  });

  test.afterAll(async () => {
    await deleteUserByPhone(TEST.phone, adminToken);
    await closeRedis();
  });

  // ── 表单验证 ──────────────────────────────────────────────
  test('V01 用户名为空', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('用户名');
  });

  test('V02 用户名格式错误（1个字符）', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill('a');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('用户名填写有误');
  });

  test('V03 手机号格式错误', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill('12345');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('手机号格式有误');
  });

  test('V04 密码为空', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('密码');
  });

  test('V05 年龄不合法（17岁）', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.age).fill('17');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('年龄填写有误');
  });

  test('V06 邮箱格式错误', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.age).fill(TEST.age);
    await page.locator(SEL.email).fill('abc@gmail.com');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('邮箱填写有误');
  });

  test('V07 未选择地区', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.age).fill(TEST.age);
    await page.locator(SEL.email).fill(TEST.email);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('请选择地址');
  });

  test('V08 详细地址不足5字', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.age).fill(TEST.age);
    await page.locator(SEL.email).fill(TEST.email);
    await selectRegion(page);
    await page.locator(SEL.detailedAddress).fill('短');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('详细地址最低填写五个字');
  });

  test('V09 验证码格式错误', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.age).fill(TEST.age);
    await page.locator(SEL.email).fill(TEST.email);
    await selectRegion(page);
    await page.locator(SEL.detailedAddress).fill(TEST.detailedAddress);
    await page.locator(SEL.code).fill('123');
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toContainText('验证码有误');
  });

  // ── 完整注册流程 ──────────────────────────────────────────
  test('F01 注册成功', async ({ page }) => {
    await openRegisterForm(page);
    await fillValidForm(page);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.registerForm)).toBeHidden({ timeout: 8000 });
  });

  test('F02 注册后跳转登录表单', async ({ page }) => {
    // 先清理，再注册
    await deleteUserByPhone(TEST.phone, adminToken);
    await openRegisterForm(page);
    await fillValidForm(page);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.registerForm)).toBeHidden({ timeout: 8000 });
    await expect(page.locator(SEL.loginForm)).toBeVisible();
  });

  // ── 重复注册 ──────────────────────────────────────────────
  test('D01 手机号已存在', async ({ page }) => {
    // 此时 TEST.phone 已注册（F02 注册成功）
    await openRegisterForm(page);
    await fillValidForm(page);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toBeVisible({ timeout: 5000 });
    await expect(page.locator(SEL.registerForm)).toBeVisible();
  });

  test('D02 邮箱已存在', async ({ page }) => {
    await openRegisterForm(page);
    // 换手机号但用相同邮箱
    await setVerifyCode(TEST.email, TEST.code);
    await page.locator(SEL.username).fill(TEST.username);
    await page.locator(SEL.phone).fill('13100000002');
    await page.locator(SEL.password).fill(TEST.password);
    await page.locator(SEL.age).fill(TEST.age);
    await page.locator(SEL.email).fill(TEST.email);
    await selectRegion(page);
    await page.locator(SEL.detailedAddress).fill(TEST.detailedAddress);
    await page.locator(SEL.code).fill(TEST.code);
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.notification)).toBeVisible({ timeout: 5000 });
    await expect(page.locator(SEL.registerForm)).toBeVisible();
  });

  // ── 验证码倒计时 ──────────────────────────────────────────
  test('C01 发送验证码按钮倒计时', async ({ page }) => {
    await openRegisterForm(page);
    await page.locator(SEL.phone).fill(TEST.phone);
    await page.locator(SEL.email).fill(TEST.email);

    await page.route('**/user/code', route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ code: 30001, msg: '发送成功' }),
      })
    );

    await page.locator(SEL.sendCode).click();
    await expect(page.locator(SEL.sendCode)).toBeDisabled();
    await expect(page.locator(SEL.sendCode)).toContainText('秒后重试');
  });
});
