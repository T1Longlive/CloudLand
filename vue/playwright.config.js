// @ts-check
import './tests/env.js'; // 加载 .env.test 中的 TEST_* 变量（worker 进程经 config 继承）
import { defineConfig, devices } from '@playwright/test';

// 测试环境网络隔离（勿删，删了 webkit 登录/注册会稳定失败）
// 宿主若注入 HTTP_PROXY/HTTPS_PROXY（企业代理、AI IDE 沙箱等），WebKit 会把
// localhost:8080 → localhost:9090 的 CORS 预检也交给代理：代理对 OPTIONS 返回
// 204 且不带 Access-Control-Allow-Origin，请求根本到不了后端，登录/注册用例
// 稳定失败，而 Edge/Chromium 与 curl 不受影响 —— 表现成「只有 webkit 挂」的假 flaky。
// 证据：Playwright trace 报 "Origin ... is not allowed by Access-Control-Allow-Origin.
// Status code: 204"；后端 Tomcat access log 无任何 OPTIONS 记录；清除代理变量后
// POST 恢复 200 且 access log 出现 "OPTIONS /api/user/login 200"。
// CI runner 不注入代理，故此前线上 62/62 全绿而本地稳定 60/62。
// 测试只访问本机服务，显式清除代理变量，保证本地与 CI 网络路径一致。
const PROXY_ENV_KEYS = [
  'HTTP_PROXY', 'HTTPS_PROXY', 'http_proxy', 'https_proxy', 'ALL_PROXY', 'all_proxy',
];
for (const key of PROXY_ENV_KEYS) {
  delete process.env[key];
}

/**
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: './tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code. */
  forbidOnly: !!process.env.CI,
  /* Retry on CI only */
  retries: process.env.CI ? 2 : 1,
  /* Opt out of parallel tests on CI. */
  workers: 1,
  /* Reporter to use. More info: https://playwright.dev/docs/test-reporters */
  reporter: [
    ['html'],
    // CI 汇总用的 JUnit XML（test-results/ 已被 .gitignore 忽略）
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  /* Shared settings for all the projects below. See https://playwright.dev/docs/api/class-testoptions. */
  use: {
    baseURL: 'http://localhost:8080',
    // 桌面站大视口：保证 el-cascader 等下拉面板不超出可视区域（headless 下曾致 V08 点击失败）
    viewport: { width: 1920, height: 1080 },
    trace: 'on-first-retry',
  },

  /* Configure projects for major browsers */
  projects: [
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },

    /* CI 用 Edge：无头、无慢放，配合 --project=edge-ci 运行 */
    {
      name: 'edge-ci',
      use: { ...devices['Desktop Edge'], channel: 'msedge', headless: true },
    },

    /* 本地调试用 Edge：有头 + 慢放 800ms，配合 --project=edge-debug 运行 */
    {
      name: 'edge-debug',
      use: { ...devices['Desktop Edge'], channel: 'msedge', headless: false, launchOptions: { slowMo: 800 } },
    }
  ],
});

