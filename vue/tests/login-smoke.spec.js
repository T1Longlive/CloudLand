// @ts-check
import { test, expect, request } from '@playwright/test';
import { setVerifyCode, closeRedis } from './helpers/redis.js';

const TEST_PHONE = '18140213287';
const TEST_EMAIL = '676104035@qq.com';
const TEST_PASSWORD = '123456';
const TEST_CODE = '888888';

const SEL = {
  loginBtn: '#login-btn',
  loginForm: '.login-form',
  phone: '.login-form input[placeholder="请输入你的手机号"]',
  password: '.login-form input[placeholder="请输入你的密码"]',
  code: '.login-form input[placeholder="请输入你的验证码"]',
  submit: '.login-form button.link-btn',
  sendCode: '.login-form .link-btn-mail',
  userMsg: '#user-msg',
  notification: '.el-notification',
};

async function openLoginForm(page) {
  await page.goto('/');
  await page.locator(SEL.loginBtn).click();
  await expect(page.locator(SEL.loginForm)).toBeVisible();
}

test.describe('登录冒烟测试', () => {
  test.describe.configure({ mode: 'serial' });

  test.afterAll(async () => {
    await closeRedis();
  });

  test('密码登录成功 - 显示欢迎信息', async ({ page }) => {
    await openLoginForm(page);
    await setVerifyCode(TEST_EMAIL, TEST_CODE);

    await page.locator(SEL.phone).fill(TEST_PHONE);
    await page.locator(SEL.password).fill(TEST_PASSWORD);
    await page.locator(SEL.code).fill(TEST_CODE);
    await page.locator(SEL.submit).click();

    await expect(page.locator(SEL.userMsg)).toBeVisible({ timeout: 5000 });
    await expect(page.locator(SEL.userMsg)).toContainText('欢迎您');
    await expect(page.locator(SEL.loginForm)).toBeHidden();
  });

  test('错误密码登录失败 - 显示错误提示', async ({ page }) => {
    await openLoginForm(page);
    await setVerifyCode(TEST_EMAIL, TEST_CODE);

    await page.locator(SEL.phone).fill(TEST_PHONE);
    await page.locator(SEL.password).fill('wrong_password');
    await page.locator(SEL.code).fill(TEST_CODE);
    await page.locator(SEL.submit).click();

    await expect(page.locator(SEL.notification)).toBeVisible({ timeout: 5000 });
    await expect(page.locator(SEL.loginForm)).toBeVisible();
  });

  test('登录后 token 持久化 - 刷新页面仍保持登录态', async ({ page }) => {
    await openLoginForm(page);
    await setVerifyCode(TEST_EMAIL, TEST_CODE);

    await page.locator(SEL.phone).fill(TEST_PHONE);
    await page.locator(SEL.password).fill(TEST_PASSWORD);
    await page.locator(SEL.code).fill(TEST_CODE);
    await page.locator('.login-form #remember-me').check();
    await page.locator(SEL.submit).click();
    await expect(page.locator(SEL.userMsg)).toBeVisible({ timeout: 5000 });

    await page.reload();
    await expect(page.locator(SEL.userMsg)).toBeVisible({ timeout: 5000 });
    await expect(page.locator(SEL.loginBtn)).toBeHidden();
  });

  test('发送验证码 - 按钮进入倒计时', async ({ page }) => {
    await openLoginForm(page);
    await page.locator(SEL.phone).fill(TEST_PHONE);
    await page.locator(SEL.password).fill(TEST_PASSWORD);

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
