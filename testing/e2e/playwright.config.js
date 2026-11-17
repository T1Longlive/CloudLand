// @ts-check
/**
 * Playwright 配置。
 *
 * 三个关键取舍，都是被现实打出来的：
 *
 * 1. 清代理变量（第 24 行起）：宿主注入的 HTTP_PROXY 会把 localhost 的 CORS 预检
 *    拐进代理，WebKit 尤其明显——表现为 "Origin not allowed" + 后端 access log 里
 *    零 OPTIONS 请求。曾把它误判为随机 flaky，加了 retries 也压不住。
 *    测试只访问本机服务，所以加载配置时直接删掉，让测试不依赖运行者的环境是否干净。
 *
 * 2. retries = 0：flaky 必须暴露。重试会把它变成"偶尔红一下"的噪音，
 *    三个月后没人敢信这套用例。真不稳就修根因。
 *
 * 3. 不做 fullyParallel 的暴力并行：订单链路共享同一份库存和同一批账号，
 *    并发只会制造"抢库存"的假失败。worker 数交给默认策略，用例本身保证数据隔离。
 */
const { defineConfig, devices } = require('@playwright/test');
const { BASE_URL, WEB_SERVER_COMMAND } = require('./fixtures/env');

for (const key of ['HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY', 'http_proxy', 'https_proxy', 'all_proxy']) {
  delete process.env[key];
}

const VIEWPORT = { width: 1920, height: 1080 };

module.exports = defineConfig({
  testDir: './tests',
  // 订单链路有前端写死的 4 秒等待，单条给到 45 秒足够
  timeout: 45_000,
  expect: { timeout: 8_000 },

  fullyParallel: false,
  retries: 0,
  forbidOnly: !!process.env.CI,
  workers: process.env.CI ? 1 : undefined,

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],

  use: {
    baseURL: BASE_URL,
    viewport: VIEWPORT,
    locale: 'zh-CN',
    timezoneId: 'Asia/Shanghai',
    // 失败才留证据：trace 能回放每一步 DOM 快照，是定位"假 flaky"的唯一靠谱手段
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
  },

  projects: [
    {
      // 默认只跑 WebKit：CI（Linux）与本地（Windows）都能装，且它是本项目历史上
      // 唯一暴露过代理问题的内核，留着等于持续回归那条修复。
      name: 'webkit',
      use: { ...devices['Desktop Safari'], viewport: VIEWPORT },
    },
    // 交叉验证可选：Windows 本机有 Edge 时用 E2E_BROWSER=edge 打开，多一个内核的样本
    ...(process.env.E2E_BROWSER === 'edge'
      ? [{ name: 'msedge', use: { ...devices['Desktop Edge'], channel: 'msedge', viewport: VIEWPORT } }]
      : []),
  ],

  /**
   * 前端由 Playwright 负责起停：`npx playwright test` 一条命令可跑，不用先手工 npm run serve。
   * 已在跑（比如本地正开着 dev server）就复用，不抢占端口。
   *
   * CI 里改成由流水线显式启动（E2E_SKIP_WEB_SERVER=1）：
   * Playwright 自己起的时候，超时只报一句 "Timed out waiting for http://..."，
   * 看不到 webpack 编译到哪一步、报了什么错。把启动过程放到独立步骤里，
   * 失败就有 frontend.log 可看，也能按需要把等待时间调到 300s（冷 runner 编译慢）。
   */
  webServer:
    process.env.E2E_SKIP_WEB_SERVER === '1'
      ? undefined
      : {
          command: WEB_SERVER_COMMAND,
          url: BASE_URL,
          reuseExistingServer: !process.env.CI,
          timeout: 180_000,
          stdout: 'ignore',
          stderr: 'pipe',
        },
});
