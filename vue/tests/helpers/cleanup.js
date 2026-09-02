// 后端 API 地址支持环境变量覆盖（本地默认 localhost:9090，CI 由 workflow 注入）
const API = process.env.TEST_API_BASE_URL || 'http://localhost:9090/api';

export async function deleteUserByPhone(phone, token) {
  const res = await fetch(`${API}/user/page`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', token },
    body: `pageNum=1&pageSize=100&user=${encodeURIComponent(JSON.stringify({ phone }))}`,
  });
  const json = await res.json();
  const records = json.data?.records ?? [];
  const user = records.find(u => u.phone === phone);
  if (!user) return;
  await fetch(`${API}/user`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json', token },
    body: JSON.stringify([user.id]),
  });
}

// E2E 测试专用号段：seed.sql 中的固定账号（1990000000x）不清理，只清理用例运行中注册的临时账号
export async function cleanupTestUsersByPrefix(prefix, token) {
  const res = await fetch(`${API}/user/page`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded', token },
    body: `pageNum=1&pageSize=100&user=${encodeURIComponent(JSON.stringify({}))}`,
  });
  const json = await res.json();
  const records = json.data?.records ?? [];
  const victims = records.filter(u => u.phone.startsWith(prefix) && !u.phone.startsWith('1990000000'));
  for (const u of victims) {
    await fetch(`${API}/user`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json', token },
      body: JSON.stringify([u.id]),
    });
  }
  return victims.length;
}
