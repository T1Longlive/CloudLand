// @ts-check
import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:8080';

const PHONE = '.login-form input[placeholder="请输入你的手机号"]';
const PASSWORD = '.login-form input[placeholder="请输入你的密码"]';
const CODE = '.login-form input[placeholder="请输入你的验证码"]';

test.beforeEach(async ({ page }) => {
  await page.goto(BASE_URL);
  await page.locator('#login-btn').click();
  await expect(page.locator('.login-form')).toBeVisible();
});

test('登录表单正常显示和关闭', async ({ page }) => {
  await expect(page.locator(PHONE)).toBeVisible();
  await expect(page.locator(PASSWORD)).toBeVisible();
  await expect(page.locator(CODE)).toBeVisible();

  await page.locator('#close-login-form').click();
  await expect(page.locator('.login-form')).toBeHidden();
});

test('手机号为空时提示错误', async ({ page }) => {
  await page.locator('.login-form button.link-btn').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('手机号');
});

test('手机号格式错误时提示错误', async ({ page }) => {
  await page.locator(PHONE).fill('12345');
  await page.locator('.login-form button.link-btn').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('手机号格式有误');
});

test('密码为空时提示错误', async ({ page }) => {
  await page.locator(PHONE).fill('13800138000');
  await page.locator('.login-form button.link-btn').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('密码');
});

test('验证码为空时提示错误', async ({ page }) => {
  await page.locator(PHONE).fill('13800138000');
  await page.locator(PASSWORD).fill('password123');
  await page.locator('.login-form button.link-btn').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('验证码');
});

test('验证码格式错误时提示错误', async ({ page }) => {
  await page.locator(PHONE).fill('13800138000');
  await page.locator(PASSWORD).fill('password123');
  await page.locator(CODE).fill('123');
  await page.locator('.login-form button.link-btn').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('验证码有误');
});

test('点击注册链接切换到注册表单', async ({ page }) => {
  await page.locator('.login-form .account a').click();
  await expect(page.locator('.login-form')).toBeHidden();
  await expect(page.locator('.register-form')).toBeVisible();
});

test('发送验证码按钮在手机号为空时不发送', async ({ page }) => {
  await page.locator('.login-form .link-btn-mail').click();
  await expect(page.locator('.el-notification')).toBeVisible();
  await expect(page.locator('.el-notification')).toContainText('手机号');
});

test('发送验证码后按钮进入倒计时状态', async ({ page }) => {
  await page.route('**/user/code', route => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 30001, msg: '发送成功' }),
    });
  });

  page.once('dialog', dialog => dialog.accept());

  await page.locator(PHONE).fill('13800138000');
  await page.locator(PASSWORD).fill('password123');
  await page.locator('.login-form .link-btn-mail').click();

  await expect(page.locator('.login-form .link-btn-mail')).toBeDisabled();
  await expect(page.locator('.login-form .link-btn-mail')).toContainText('秒后重试');
});
