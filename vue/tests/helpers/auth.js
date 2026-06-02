import { setVerifyCode } from './redis.js';

const ADMIN = { phone: '18140213287', password: '123456', email: '676104035@qq.com' };
const API = 'http://localhost:9090/api';

export async function getAdminToken() {
  await setVerifyCode(ADMIN.email, '888888');
  const res = await fetch(`${API}/user/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', token: 'null', frond: 'true', code: '888888' },
    body: JSON.stringify({ phone: ADMIN.phone, password: ADMIN.password }),
  });
  const json = await res.json();
  return json.msg; // JWT token
}
