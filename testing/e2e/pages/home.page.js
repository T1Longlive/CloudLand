/**
 * 首页。
 *
 * 选择器策略（整个 pages/ 目录统一遵守）：
 * 1. 优先用"用户看得见的东西"——文字、placeholder、role。它们不会因为改样式而失效。
 * 2. 其次用 id / data-testid 这类**契约性锚点**。
 * 3. 禁止：class 链、nth-child、`div > div > span` 这种结构选择器。
 *    结构选择器今天能跑，改一次布局就全红，是最贵的"省事"。
 */
class HomePage {
  constructor(page) {
    this.page = page;
    // 首页右上角的人像按钮（未登录时显示），点它弹出登录表单
    this.loginTrigger = page.locator('#login-btn');
    // 登录后同一位置变成"欢迎您，xxx"
    this.userGreeting = page.locator('#user-msg');
  }

  async goto() {
    await this.page.goto('/');
    await this.page.waitForLoadState('domcontentloaded');
  }

  /** 打开首页并等导航栏渲染完成（首页有滚动动画，等锚点按钮可见更稳） */
  async open() {
    await this.goto();
    await this.loginTrigger.waitFor({ state: 'visible' });
  }

  async openLoginForm() {
    await this.loginTrigger.click();
    await this.page.locator('.login-form').waitFor({ state: 'visible' });
  }
}

module.exports = { HomePage };
