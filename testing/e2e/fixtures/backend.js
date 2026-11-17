/**
 * 后端 API 驱动：E2E 只把它用来"造数 / 清数"，不用它做断言。
 *
 * 为什么 E2E 还要调 API：
 * 1. 速度——用 8 次点击注册出来的账号，只为了给下单用例当背景，属于用 UI 造数据，慢且脆。
 * 2. 边界——UI 只负责验证"用户能走通的路"。造一个库存 20 的产品走后台表单要点十几次，
 *    一旦后台改版，订单用例跟着红，故障定位被污染。
 * 3. 清理——用例跑完把账号删掉，库才不会被历次运行腌入味。
 *
 * 约定：这个文件里出现的一切期望值（code 20006/10001 之类）都是**前置断言**，
 * 造数失败就直接失败，不能让下游用例产生一堆看不懂的假失败。
 */
const { API_BASE_URL, MASTER_CODE } = require('./env');
const { SEED_ACCOUNTS } = require('./accounts');

// E2E 临时账号手机号段（与 interface 层 134 段、seed 199 段区分开，便于排查和批量清理）
const E2E_PHONE_PREFIX = '131';

// 1x1 透明 PNG：产品创建接口强制要一张图，用最小合法文件避免仓库里塞二进制
const TINY_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==',
  'base64'
);

function uniqueSuffix() {
  return String(Date.now()).slice(-8);
}

class Backend {
  /** @param {import('@playwright/test').APIRequestContext} context 已绑定 baseURL 的请求上下文 */
  constructor(context) {
    this.context = context;
    this._tokens = new Map();
  }

  // ---------- 登录态 ----------
  /** 用 seed 账号换 token（内部缓存，同一个 role 只登录一次） */
  async tokenFor(role) {
    if (this._tokens.has(role)) return this._tokens.get(role);

    const account = SEED_ACCOUNTS[role];
    if (!account) throw new Error(`未知角色：${role}`);

    const resp = await this.context.post('user/login', {
      data: { phone: account.phone, password: account.password },
      headers: { frond: 'true', code: MASTER_CODE },
    });
    const body = await resp.json();
    // 登录成功时后端把 JWT 放在 msg 字段（不是 data，也不是响应头）——项目历史契约
    if (body.code !== 20005) {
      throw new Error(`seed 账号 ${role} 登录失败，E2E 无法造数：${JSON.stringify(body)}`);
    }
    this._tokens.set(role, body.msg);
    return body.msg;
  }

  // ---------- 用户 ----------
  /**
   * 造一个临时客户账号（power=0），返回可直接用于 UI 登录的凭据。
   * 手机号段 131，邮箱带时间戳，第二次跑不会撞"已注册"。
   */
  async createTempCustomer(overrides = {}) {
    const phone = `${E2E_PHONE_PREFIX}${uniqueSuffix()}`;
    const user = {
      username: `E2E用户${phone.slice(-4)}`,
      phone,
      password: 'Test@123456',
      age: 25,
      // 与 seed 账号同样的四川省/成都市/锦江区，保证前端展示逻辑一致
      address: '510000,510100,510101',
      detailedAddress: '成都市武侯区天府大道 1 号 E2E',
      mail: `${phone}@qq.com`,
      ...overrides,
    };

    const resp = await this.context.post('user/register', {
      // 注册是 @RequestParam user（一段 JSON 字符串），不是 JSON body
      form: { user: JSON.stringify(user) },
      headers: { frond: 'true', code: MASTER_CODE },
    });
    const body = await resp.json();
    if (body.code !== 20006) {
      throw new Error(`临时账号注册失败：${JSON.stringify(body)}`);
    }
    return { ...user, code: MASTER_CODE };
  }

  /**
   * 按手机号查账号。
   *
   * ⚠️ 这里不能用 `POST /user/page` 的 phone 条件——它根本没实现：
   * UserMapper.selectByCondition 只拼了 status / id / power / username 四个 <if>，
   * 传 phone 会被静默忽略，接口老老实实返回**全部用户**。
   * （与 msg/page 的 phone 过滤失效是同一类缺陷，接口层已用 xfail 记录。）
   *
   * 所以只能翻页拉回来自己匹配。分页大小取 100，最多翻 10 页（1000 个账号）——
   * 测试库规模到不了这个量级，翻满还没找到就是真的没有。
   */
  async findUserByPhone(phone) {
    const token = await this.tokenFor('admin');
    for (let pageNum = 1; pageNum <= 10; pageNum += 1) {
      const resp = await this.context.post('user/page', {
        form: { pageNum, pageSize: 100, user: '{}' },
        headers: { token, frond: 'true' },
      });
      const body = await resp.json();
      const records = body?.data?.records || [];
      const hit = records.find((u) => u.phone === phone);
      if (hit) return hit;
      if (records.length < 100) return null; // 最后一页
    }
    return null;
  }

  /** 按手机号删除账号。order2 / trolley 对 user 有 ON DELETE CASCADE，删账号即清掉其业务数据。 */
  async deleteUserByPhone(phone) {
    const user = await this.findUserByPhone(phone);
    if (!user) return false;
    const token = await this.tokenFor('admin');
    const resp = await this.context.delete('user', {
      data: [user.id],
      headers: { token, frond: 'true' },
    });
    const body = await resp.json();
    if (body.code !== 10002) {
      throw new Error(`临时账号清理失败：${JSON.stringify(body)}`);
    }
    return true;
  }

  // ---------- 产品 ----------
  /** 取一个已存在的合作商 id：product.a_id 是 NOT NULL 外键，造产品必须带上 */
  async firstAccountId() {
    const resp = await this.context.post('product/page', {
      form: { pageNum: 1, pageSize: 1, product: '{}' },
    });
    const body = await resp.json();
    const first = body?.data?.records?.[0];
    // Jackson 对 Lombok 的 aId 字段序列化成全小写 aid（与订单的 pid/uid 同一个规律），
    // 两个都接一下，避免后端命名风格微调就把造数打死。
    const accountId = first?.aId ?? first?.aid;
    if (accountId) return accountId;

    // 库里没有产品时的兜底：用管理员账号顶替
    const admin = await this.findUserByPhone(SEED_ACCOUNTS.admin.phone);
    if (admin?.id) return admin.id;
    throw new Error('库里既没有产品也没有管理员账号，无法造产品');
  }

  /** 造一个上架且有库存的产品，返回 { id, productName, price, num } */
  async createStockedProduct({ name, price = 12.5, num = 20 } = {}) {
    const token = await this.tokenFor('admin');
    const productName = name || `E2E产品-${uniqueSuffix()}`;
    const aId = await this.firstAccountId();

    const resp = await this.context.post('product', {
      multipart: {
        product: JSON.stringify({
          productName,
          description: 'E2E 自动化测试造数，跑完即删',
          ordered: 0,
          price,
          status: 1,
          aId,
          num,
          img: '',
        }),
        productImg: { name: 'e2e.png', mimeType: 'image/png', buffer: TINY_PNG },
      },
      headers: { token, frond: 'true' },
    });
    const body = await resp.json();
    if (body.code !== 10001) {
      throw new Error(`产品造数失败：${JSON.stringify(body)}`);
    }

    // 创建接口不返回主键，只能回查。按名称精确匹配，不接受"随便拿一条"。
    const found = await this.findProductByName(productName);
    if (!found) throw new Error(`产品已创建但回查不到：${productName}`);
    return { ...found, price: Number(found.price), num: Number(found.num) };
  }

  async findProductByName(productName) {
    const resp = await this.context.post('product/page', {
      form: { pageNum: 1, pageSize: 10, product: JSON.stringify({ productName }) },
    });
    const body = await resp.json();
    const records = body?.data?.records || [];
    return records.find((p) => p.productName === productName) || null;
  }

  async deleteProduct(id) {
    const token = await this.tokenFor('admin');
    const resp = await this.context.delete('product', {
      data: [id],
      headers: { token, frond: 'true' },
    });
    const body = await resp.json();
    if (body.code !== 10002) {
      throw new Error(`产品清理失败：${JSON.stringify(body)}`);
    }
  }
}

module.exports = { Backend, API_BASE_URL };
