const API = 'http://localhost:9090/api';

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
