import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vitest/config'

/**
 * 前端单测配置（Vitest + jsdom）。
 *
 * 为什么只测 src/utils 与 src/config：
 * 这两处是**纯逻辑**（token 生命周期、登录态缓存、URL 归一化），
 * 出错方式极其隐蔽——缓存没失效会让被禁用的账号还能用 5 分钟，
 * baseUrl 少了 /api 会让全站接口 404（这个坑真踩过）。它们值得单测。
 *
 * 为什么不测 .vue 组件：
 * 组件行为（渲染、交互、路由守卫）已有 7 条 Playwright E2E 覆盖，
 * 再用 @vue/test-utils 重写一遍，等于用两套机制测同一件事，
 * 改一次 UI 要修两处，收益远低于成本。覆盖率口径也只统计上面两个目录，
 * 不拿组件去稀释分母——那会让阈值变成自欺欺人的数字。
 */
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.spec.js'],
    // 每个用例前重置 mock，避免调用记录互相污染
    restoreMocks: true,
    coverage: {
      provider: 'v8',
      include: ['src/utils/**', 'src/config/**'],
      reporter: ['text', 'json-summary'],
      thresholds: {
        lines: 90,
        functions: 90,
        statements: 90,
        branches: 80,
      },
    },
  },
})
