/**
 * 注册表单（LoginForm.vue 的前台注册模式）。
 *
 * 这里有个必须写清的历史坑：地址是 el-cascader 三级联动，
 * 而"选地址"这件事在 DOM 里不是一次点击——要点省→市→区三列。
 * 很多 E2E 写不下去就退化成"跳过注册用例"，本质是没把交互拆开。
 */
class RegisterFormPage {
  constructor(page) {
    this.page = page;
    this.root = page.getByTestId('register-form');

    this.username = this.root.getByPlaceholder('请输入用户名(2-10个字符)');
    this.phone = this.root.getByPlaceholder('请输入手机号', { exact: true });
    this.password = this.root.getByPlaceholder('请设置你的密码(6-20位)');
    this.age = this.root.getByPlaceholder('请输入你的年龄');
    this.mail = this.root.getByPlaceholder('请输入QQ邮箱(@qq.com结尾)');
    this.detailedAddress = this.root.getByPlaceholder('请输入你的详细地址');
    this.code = this.root.getByPlaceholder('请输入你的验证码');

    // 级联选择器的可点区域是内部 input，不是外层 div
    this.regionInput = this.root.locator('.el-cascader .el-input__inner');
    /**
     * 面板**不能**用 this.root 作用域去找。
     * el-popper 展开时会把弹层节点移动到 document.body（`body > .el-cascader__dropdown`），
     * 于是 `[data-testid="register-form"] .el-cascader__dropdown` 在打开后变成 0 个元素——
     * 这是 el-popper 的固定行为，不是偶发。用应用显式声明的 popper-class 定位，全页唯一。
     */
    this.regionPanel = page.locator('.el-cascader__dropdown.region-cascader-popper');

    this.submit = this.root.getByRole('button', { name: '确认注册' });
    this.notification = page.locator('.el-notification');
  }

  async waitReady() {
    await this.root.waitFor({ state: 'visible' });
  }

  /** 文本类字段（不含地址和验证码），便于"缺某一项"的负向用例按需覆盖 */
  async fillTextFields({ username, phone, password, age, mail, detailedAddress }) {
    if (username !== undefined) await this.username.fill(username);
    if (phone !== undefined) await this.phone.fill(phone);
    if (password !== undefined) await this.password.fill(password);
    if (age !== undefined) await this.age.fill(String(age));
    if (mail !== undefined) await this.mail.fill(mail);
    if (detailedAddress !== undefined) await this.detailedAddress.fill(detailedAddress);
  }

  /**
   * 三级联动选地址，例如 ['四川省', '成都市', '武侯区']。
   *
   * 实现要点：每选一级，Element UI 会往面板里追加一列（.el-cascader-menu），
   * 所以第 n 级就去第 n 列点。用 role=menuitem 定位而不是 class，
   * 是因为 li 上带 role，属于无障碍契约，改名改样式都不会失效。
   */
  async selectRegion([province, city, county]) {
    await this.regionInput.click();
    await this.regionPanel.waitFor({ state: 'visible' });

    const columns = this.regionPanel.locator('.el-cascader-menu');
    // 这里刻意不传 exact：节点的可访问名里混进了后置箭头图标带出的空白，
    // 整串匹配会失配（实测 exact:true → 0 个，去掉 → 1 个）。用子串匹配反而更贴近用户视角。
    await columns.nth(0).getByRole('menuitem', { name: province }).click();
    await columns.nth(1).getByRole('menuitem', { name: city }).click();
    await columns.nth(2).getByRole('menuitem', { name: county }).click();

    // 选完叶子节点后面板收起，这同时是"地址已生效"的信号
    await this.regionPanel.waitFor({ state: 'hidden' });
  }

  async fillCode(code) {
    await this.code.fill(code);
  }

  async submitRegister() {
    await this.submit.click();
  }

  /** 一次性填完整套（含地址和验证码），用于正向用例 */
  async fillAll({ region, code, ...fields }) {
    await this.waitReady();
    await this.fillTextFields(fields);
    await this.selectRegion(region);
    await this.fillCode(code);
  }

  async notificationText() {
    await this.notification.first().waitFor({ state: 'visible' });
    return (await this.notification.first().innerText()).replace(/\s+/g, ' ').trim();
  }
}

module.exports = { RegisterFormPage };
