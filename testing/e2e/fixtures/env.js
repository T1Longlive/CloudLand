/**
 * E2E 环境配置的唯一入口。
 *
 * 为什么集中在这里：用例和 Page Object 都不读 process.env。
 * 环境差异（本地 8080 / CI 里的服务地址 / 换一台机器）只改这一个文件或这几个变量，
 * 用例本身永远只写"相对路径"和"业务语义"。
 */
const BASE_URL = (process.env.E2E_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '');

// 后端 API 前缀（application.yml 的 server.servlet.context-path=/api）
const API_BASE_URL = (process.env.E2E_API_BASE_URL || 'http://localhost:9090/api').replace(/\/+$/, '');

// 万能验证码：后端 code.master-enabled=true 时登录/注册都能用它通过。
// 生产必须关闭这个开关，所以 E2E 只跑在测试环境。
const MASTER_CODE = process.env.E2E_MASTER_CODE || '000000';

/**
 * 前端由谁提供：
 * - 默认让 Playwright 起 vue dev server（本地 `npx playwright test` 一条命令就能跑）
 * - 已在跑的话自动复用（reuseExistingServer）
 * - CI 里可以传 E2E_WEB_SERVER_COMMAND 指向静态构建产物，和线上形态更接近
 */
const WEB_SERVER_COMMAND = process.env.E2E_WEB_SERVER_COMMAND || 'npm --prefix ../../vue run serve';

module.exports = {
  BASE_URL,
  API_BASE_URL,
  MASTER_CODE,
  WEB_SERVER_COMMAND,
};
