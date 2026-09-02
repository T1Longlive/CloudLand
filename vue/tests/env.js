// .env.test 加载器：将 vue/.env.test 中的 TEST_* 变量注入 process.env（副作用模块）
// 用法：playwright.config.js / seed.mjs 顶部 import 本文件
// CJS 语法：需同时兼容 Playwright 的 transform 管道与 seed.mjs 的纯 Node ESM import
const { existsSync } = require('node:fs');
const { join } = require('node:path');

const envPath = join(__dirname, '..', '.env.test');

if (existsSync(envPath)) {
  // 已存在的环境变量优先：CI 注入的 TEST_* 不被 .env.test 覆盖
  const preserved = {};
  for (const key of Object.keys(process.env)) {
    if (key.startsWith('TEST_')) preserved[key] = process.env[key];
  }
  process.loadEnvFile(envPath);
  Object.assign(process.env, preserved);
}

module.exports = {};
