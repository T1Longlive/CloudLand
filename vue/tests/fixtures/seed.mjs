// E2E 测试种子账号准备脚本（跨平台，Node 直连 MySQL，不依赖 mysql CLI）
// 读取 seed.sql（INSERT IGNORE 幂等）执行，并校验 3 个账号就绪后退出。
// 连接参数支持环境变量：TEST_DB_HOST / TEST_DB_PORT / TEST_DB_USER / TEST_DB_PASSWORD / TEST_DB_NAME
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import mysql from 'mysql2/promise';

const __dirname = dirname(fileURLToPath(import.meta.url));

const config = {
  host: process.env.TEST_DB_HOST || 'localhost',
  port: Number(process.env.TEST_DB_PORT || 3306),
  user: process.env.TEST_DB_USER || 'root',
  password: process.env.TEST_DB_PASSWORD || '123456',
  database: process.env.TEST_DB_NAME || 'cloudland',
  multipleStatements: true, // seed.sql 含注释与多行 INSERT，需整体执行
};

const EXPECTED_PHONES = ['19900000001', '19900000002', '19900000003'];

async function main() {
  const sql = readFileSync(join(__dirname, 'seed.sql'), 'utf8');
  const conn = await mysql.createConnection(config);

  try {
    await conn.query(sql);
    const [rows] = await conn.query(
      'SELECT phone, power, status FROM user WHERE phone IN (?)',
      [EXPECTED_PHONES],
    );
    const got = rows.map(r => `${r.phone}(power=${r.power},status=${r.status})`).sort();
    if (rows.length !== EXPECTED_PHONES.length) {
      throw new Error(`种子账号不完整：期望 ${EXPECTED_PHONES.length} 个，实际 ${rows.length} 个 [${got.join(', ')}]`);
    }
    console.log(`[seed] 测试账号就绪: ${got.join(', ')}`);
  } finally {
    await conn.end();
  }
}

main().catch(err => {
  console.error('[seed] 失败:', err.message);
  process.exit(1);
});
