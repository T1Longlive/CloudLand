// @ts-check
import { test, expect } from '@playwright/test';
import { setVerifyCode, closeRedis } from './helpers/redis.js';
import { getAdminToken } from './helpers/auth.js';
import { cleanupTestUsersByPrefix } from './helpers/cleanup.js';
import { TEST_ACCOUNTS, API_BASE_URL } from './fixtures/accounts.js';

// cleanupTestUsersByPrefix 的调用方用例（纯 API，不依赖前端页面）
// 132 段为本 spec 专用前缀：不与 register-smoke 的 131 段、seed 的 199 段冲突
const TEMP_USERS = [
  { username: '清理测试甲', phone: '13200000001', password: 'test123456', age: 25, mail: '13200000001@qq.com', address: '四川省,成都市,锦江区', detailedAddress: '自动化清理测试地址001' },
  { username: '清理测试乙', phone: '13200000002', password: 'test123456', age: 26, mail: '13200000002@qq.com', address: '四川省,成都市,锦江区', detailedAddress: '自动化清理测试地址002' },
];
const TEST_CODE = '888888';

// 走后端注册接口造号（与前端真实流程一致：FormData user + frond/code 头，Redis 直写验证码）
async function registerByApi(user) {
  await setVerifyCode(user.mail, TEST_CODE);
  const formData = new FormData();
  formData.append('user', JSON.stringify(user));
  const res = await fetch(`${API_BASE_URL}/user/register`, {
    method: 'POST',
    headers: { frond: 'true', code: TEST_CODE },
    body: formData,
  });
  return res.json();
}

// 精确查询单个手机号是否仍存在（user/page 支持按 phone 条件查询）
async function findUserByPhone(phone, token) {
  const res = await fetch(`${API_BASE_URL}/user/page`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', token },
    body: `pageNum=1&pageSize=100&user=${encodeURIComponent(JSON.stringify({ phone }))}`,
  });
  const json = await res.json();
  return (json.data?.records ?? []).find(u => u.phone === phone) ?? null;
}

test.describe('清理 helper 测试 - cleanupTestUsersByPrefix', () => {
  test.describe.configure({ mode: 'serial' });

  let adminToken;

  test.beforeAll(async () => {
    adminToken = await getAdminToken();
    // 保证起点干净：清掉历史遗留的 132 段账号
    await cleanupTestUsersByPrefix('132', adminToken);
  });

  test.afterAll(async () => {
    await closeRedis();
  });

  test('前置 - API 注册两个 132 段临时账号', async () => {
    for (const user of TEMP_USERS) {
      const json = await registerByApi(user);
      expect(json.code).toBe(20006); // REGISTER_OK
    }
  });

  test('按前缀清理 - 删除 132 段账号并返回删除数量', async () => {
    const removed = await cleanupTestUsersByPrefix('132', adminToken);
    expect(removed).toBeGreaterThanOrEqual(TEMP_USERS.length);
    for (const user of TEMP_USERS) {
      expect(await findUserByPhone(user.phone, adminToken)).toBeNull();
    }
  });

  test('幂等重跑 - 无残留可删时返回 0', async () => {
    expect(await cleanupTestUsersByPrefix('132', adminToken)).toBe(0);
  });

  test('安全性 - seed 账号（199 段）不受前缀清理影响', async () => {
    // helper 内部显式排除 199 段；账号仍可查询且管理员仍可登录，即证明未被误删
    expect(await findUserByPhone(TEST_ACCOUNTS.admin.phone, adminToken)).not.toBeNull();
    const token = await getAdminToken();
    expect(token).toBeTruthy();
  });
});
