/**
 * 注册链路。
 *
 * 这条链路的价值不在"能注册成功"，而在两件事：
 * 1. 前端校验是最后一道闸——后端对缺字段的请求会直接 500（已固化为接口层 xfail），
 *    所以"前端拦住、请求没发出去"必须被验证，而不是靠人盯着。
 * 2. 三级地址联动（el-cascader）是这套 UI 里最容易写崩的交互，
 *    它不通过，注册用例就永远只能停在"填了一半"。
 */
const { test, expect } = require('../fixtures');
const { MASTER_CODE } = require('../fixtures/env');
const { HomePage } = require('../pages/home.page');
const { LoginFormPage } = require('../pages/login-form.page');
const { RegisterFormPage } = require('../pages/register-form.page');

// 131/132 段是 E2E 专用临时号段（见 testing/api/common/data_factory.py 的约定）
function newIdentity(prefix = '132') {
  const suffix = String(Date.now()).slice(-8);
  const phone = `${prefix}${suffix}`;
  return {
    phone,
    username: `E2E注册${suffix.slice(-4)}`,
    password: 'Test@123456',
    age: 28,
    // 前端 QQ 邮箱正则为 ^[1-9]\d{3,13}@qq\.com$：必须以 1-9 开头，所以显式补一个前导 1
    mail: `1${String(Date.now()).slice(-8)}@qq.com`,
    // 前端正则 /^\S{5,}$/ 不允许空格：地址里带空格会被判成"最低填五个字"
    detailedAddress: '成都市武侯区天府大道1号E2E',
    region: ['四川省', '成都市', '武侯区'],
  };
}

const createdPhones = [];
// 无论用例成败都清账号：失败时不清理，下次跑就会撞"手机号已注册"，红得莫名其妙
test.afterEach(async ({ backend }) => {
  while (createdPhones.length) {
    const phone = createdPhones.pop();
    try {
      await backend.deleteUserByPhone(phone);
    } catch (error) {
      console.warn(`[cleanup] 注册用例账号 ${phone} 清理失败：${error.message}`);
    }
  }
});

test.describe('注册', () => {
  test('必填项为空 → 前端拦截并提示，不发出注册请求', async ({ page }) => {
    const home = new HomePage(page);
    const loginForm = new LoginFormPage(page);
    const registerForm = new RegisterFormPage(page);

    const registerRequests = [];
    page.on('request', (request) => {
      if (request.url().includes('/user/register')) registerRequests.push(request.url());
    });

    await home.open();
    await home.openLoginForm();
    await loginForm.goToRegister();
    await registerForm.waitReady();

    await registerForm.submitRegister();

    // 校验顺序是先手机号后其它（LoginForm.checkFrom → validatePhone）
    expect(await registerForm.notificationText()).toContain('请填写手机号');
    expect(registerRequests, '前端校验没拦住，请求已经打到后端了').toHaveLength(0);
  });

  test('完整填写（含三级地址）→ 注册成功，且新账号能登录', async ({ page }) => {
    const identity = newIdentity();
    createdPhones.push(identity.phone);

    const home = new HomePage(page);
    const loginForm = new LoginFormPage(page);
    const registerForm = new RegisterFormPage(page);

    await home.open();
    await home.openLoginForm();
    await loginForm.goToRegister();

    await registerForm.fillAll({ ...identity, code: MASTER_CODE });

    // 断言"后端确实返回了注册成功"，而不是只看页面变化：
    // 提示浮层 1 秒就消失，拿它当唯一判据既脆弱又说不清失败原因（code/msg 才是证据）。
    const [response] = await Promise.all([
      page.waitForResponse(
        (r) => r.url().includes('/user/register') && r.request().method() === 'POST'
      ),
      registerForm.submitRegister(),
    ]);
    const body = await response.json();
    expect(body.code, `注册接口返回：${JSON.stringify(body)}`).toBe(20006);

    // 成功后组件自己切回登录表单（不是跳页），这是 UI 层的信号
    await expect(registerForm.root).toBeHidden();
    await expect(loginForm.root).toBeVisible();

    // 注册成功的账号必须是 power=0 的客户：能登录 = 没有因为提权修复被误伤
    await loginForm.login({
      phone: identity.phone,
      password: identity.password,
      code: MASTER_CODE,
    });
    await expect(home.userGreeting).toContainText(identity.username);
  });
});
