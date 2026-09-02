// @ts-check
import { defineConfig, devices } from '@playwright/test';

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

