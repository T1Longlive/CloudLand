import Redis from 'ioredis';

// 惰性单例：Playwright 会复用 worker 进程跑多个测试文件，
// 前一文件 afterAll 的 closeRedis() 会关闭模块级连接；
// 断线（status=end）时自动重建，避免 "Connection is closed"。
let redis = null;

function getRedis() {
  if (!redis || redis.status === 'end') {
    redis = new Redis({
      host: process.env.TEST_REDIS_HOST || 'localhost',
      port: Number(process.env.TEST_REDIS_PORT || 6379),
      password: process.env.TEST_REDIS_PASSWORD || '123456',
      maxRetriesPerRequest: 3,
    });
  }
  return redis;
}

// 直接写入验证码到 Redis，key 为邮箱，TTL 5分钟
export async function setVerifyCode(email, code = '888888') {
  await getRedis().set(email, code, 'EX', 300);
  return code;
}

export async function getVerifyCode(email) {
  return getRedis().get(email);
}

export async function closeRedis() {
  if (redis) {
    await redis.quit().catch(() => {});
    redis = null; // 置空，后续文件复用 worker 时可重建
  }
}
