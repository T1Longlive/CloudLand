import Redis from 'ioredis';

const redis = new Redis({ host: 'localhost', port: 6379, password: '123456' });

// 直接写入验证码到 Redis，key 为邮箱，TTL 5分钟
export async function setVerifyCode(email, code = '888888') {
  await redis.set(email, code, 'EX', 300);
  return code;
}

export async function getVerifyCode(email) {
  return redis.get(email);
}

export async function closeRedis() {
  await redis.quit();
}
