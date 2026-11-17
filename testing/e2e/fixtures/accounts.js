/**
 * 种子账号：与 testing/api/data/seed/seed_test_accounts.sql 保持同一份事实。
 *
 * 为什么不各写各的：账号是接口测试和 E2E 的共同前置。
 * 两边各存一份硬编码，改密码时必然漏一边，然后出现"接口层绿、E2E 红"的假故障。
 * 唯一事实来源是那份 SQL，这里的常量只是它的可读镜像。
 */
const SEED_ACCOUNTS = {
  // power=0 客户：前台登录必须带 frond 头，历史上漏传导致全部客户登录不上
  customer: {
    phone: '19900000001',
    username: '测试客户',
    password: 'Test@123456',
    power: 0,
  },
  // power=1 员工
  employee: {
    phone: '19900000002',
    username: '测试员工',
    password: 'Test@123456',
    power: 1,
  },
  // power=2 管理员：造数/清理用的后台账号
  admin: {
    phone: '19900000003',
    username: '测试管理员',
    password: 'Test@123456',
    power: 2,
  },
};

module.exports = { SEED_ACCOUNTS };
