/**
 * 自定义夹具：把"造数 + 清数"从用例里赶出去。
 *
 * 用例里应该只剩三件事：打开页面、操作、断言。
 * 一旦出现"先注册个账号""先建个产品"，用例就从"验证行为"退化成"搭环境"。
 *
 * 清数策略：用完即删，且**删失败只告警不失败**。
 * 理由——清理失败是环境问题，不该把一条真实通过的用例染红（那是假失败）；
 * 但也绝不能静默，所以打印出来，让人看见"库开始脏了"。
 */
const { test: base, expect, request } = require('@playwright/test');
const { Backend } = require('./backend');
const { API_BASE_URL } = require('./env');
const { SEED_ACCOUNTS } = require('./accounts');

const test = base.extend({
  /** 直连后端的请求上下文（独立于浏览器，不共享 cookie/storage） */
  backend: async ({}, use) => {
    // 不设默认 Content-Type：后端同一个 Controller 里既有 JSON body、也有表单参数，
    // 由每个请求自己声明（data → application/json，form → x-www-form-urlencoded）。
    //
    // baseURL 尾部必须带 "/"：Playwright 按 URL 规则解析相对路径，
    // "http://host/api" + "user/page" 会被解析成 "http://host/user/page"（丢掉 /api）。
    // 带斜杠后才是 "http://host/api/user/page"。
    const context = await request.newContext({ baseURL: `${API_BASE_URL}/` });
    await use(new Backend(context));
    await context.dispose();
  },

  /** 一条用例专属的临时客户账号：互不干扰，跑完删除 */
  tempCustomer: async ({ backend }, use) => {
    const account = await backend.createTempCustomer();
    await use(account);
    try {
      await backend.deleteUserByPhone(account.phone);
    } catch (error) {
      console.warn(`[cleanup] 临时账号 ${account.phone} 清理失败：${error.message}`);
    }
  },

  /** 一条用例专属的、有库存的上架产品：跑完删除 */
  stockedProduct: async ({ backend }, use) => {
    const product = await backend.createStockedProduct();
    await use(product);
    try {
      await backend.deleteProduct(product.id);
    } catch (error) {
      console.warn(`[cleanup] 产品 ${product.id} 清理失败：${error.message}`);
    }
  },

  /** seed 客户账号（只读：不改资料、不禁用，否则下一条用例连坐） */
  seedCustomer: async ({}, use) => {
    await use(SEED_ACCOUNTS.customer);
  },
});

module.exports = { test, expect, SEED_ACCOUNTS };
