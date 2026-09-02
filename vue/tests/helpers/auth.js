import { setVerifyCode } from './redis.js';
import { TEST_ACCOUNTS, API_BASE_URL } from '../fixtures/accounts.js';

// 通用密码登录（绕过邮件验证码：Redis 直写 + code 头）
// account 结构：{ phone, password, email }
export async function loginByPassword(account) {
  await setVerifyCode(account.email, '888888');
  const res = await fetch(`${API_BASE_URL}/user/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', token: 'null', frond: 'true', code: '888888' },
    body: JSON.stringify({ phone: account.phone, password: account.password }),
  });
  const json = await res.json();
  return json.msg; // JWT token
}

// 管理员 token：使用 seed.sql 的专用测试管理员（power=2），
// 不再依赖 cloudland.sql 演示账号（其 power/status 可能被本地改动，曾导致 403）
export async function getAdminToken() {
  return loginByPassword(TEST_ACCOUNTS.admin);
}
