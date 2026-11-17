/**
 * 登录弹窗（LoginForm.vue 的前台登录模式）。
 *
 * 一个组件里塞了登录和注册两套表单（v-show 切换），所以两个 Page Object 各自
 * 用自己的根节点 data-testid 圈定范围——不然 `getByPlaceholder('请输入你的验证码')`
 * 会同时命中两套表单，触发 strict 模式报错。
 */
class LoginFormPage {
  constructor(page) {
    this.page = page;
    this.root = page.getByTestId('login-form');
    this.phone = this.root.getByPlaceholder('请输入你的手机号');
    this.password = this.root.getByPlaceholder('请输入你的密码');
    this.code = this.root.getByPlaceholder('请输入你的验证码');
    this.submit = this.root.getByRole('button', { name: '登录' });
    this.registerLink = this.root.getByText('注册一个!');
    // Element UI 的 notify（右上角浮层）：登录结果都用它提示
    this.notification = page.locator('.el-notification');
  }

  async waitReady() {
    await this.root.waitFor({ state: 'visible' });
  }

  /**
   * 填登录表单。
   * 注意验证码是**必填**的：LoginUp() 里 checkFrom(false, true) 要求 code 非空 6 位，
   * 所以"只用密码登录"这条路在 UI 上不存在，必须配万能验证码（测试环境）。
   */
  async fill({ phone, password, code }) {
    await this.phone.fill(phone);
    await this.password.fill(password);
    if (code !== undefined) await this.code.fill(code);
  }

  async submitLogin() {
    await this.submit.click();
  }

  async login({ phone, password, code }) {
    await this.waitReady();
    await this.fill({ phone, password, code });
    await this.submitLogin();
  }

  /** 等待任意一条通知出现，返回其文本（用于断言"提示了但没登录成功"） */
  async notificationText() {
    await this.notification.first().waitFor({ state: 'visible' });
    return (await this.notification.first().innerText()).replace(/\s+/g, ' ').trim();
  }

  async goToRegister() {
    await this.registerLink.click();
  }
}

module.exports = { LoginFormPage };
