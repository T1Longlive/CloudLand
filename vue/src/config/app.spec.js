import { beforeEach, describe, expect, it, vi } from 'vitest'

/**
 * app.js 在模块加载时就把 env 固化成常量了，所以每条用例都要
 * `vi.resetModules()` + 动态 import，否则测的都是第一份缓存。
 */
async function loadConfig(env = {}) {
  vi.resetModules()
  delete process.env.VUE_APP_API_BASE_URL
  delete process.env.VUE_APP_FRONTEND_BASE_URL
  Object.assign(process.env, env)
  const mod = await import('./app')
  return mod.APP_CONFIG
}

describe('APP_CONFIG.apiBaseUrl', () => {
  beforeEach(() => {
    delete process.env.VUE_APP_API_BASE_URL
    delete process.env.VUE_APP_FRONTEND_BASE_URL
  })

  it('没有注入环境变量时，兜底值必须带 /api', async () => {
    // 这条守的是一个真实事故：兜底值少写 /api，本地靠未跟踪的 .env.local 顶着，
    // 一进 CI 全站接口 404，而页面看起来完全正常。
    const config = await loadConfig()
    expect(config.apiBaseUrl).toBe('http://localhost:9090/api')
  })

  it('去掉尾部斜杠，避免拼出 // 路径', async () => {
    const config = await loadConfig({ VUE_APP_API_BASE_URL: 'http://api.test:9090/api///' })
    expect(config.apiBaseUrl).toBe('http://api.test:9090/api')
  })

  it('资源前缀全部基于 apiBaseUrl，且以 / 结尾', async () => {
    const config = await loadConfig({ VUE_APP_API_BASE_URL: 'http://api.test/api' })
    expect(config.resourceUrls.userFile).toBe('http://api.test/api/resource/userFile/')
    expect(config.resourceUrls.landFile).toBe('http://api.test/api/resource/landFile/')
    expect(config.resourceUrls.productFile).toBe('http://api.test/api/resource/productFile/')
  })

  it('frontendBaseUrl 默认取当前站点 origin，并去掉尾斜杠', async () => {
    const config = await loadConfig()
    expect(config.frontendBaseUrl).toBe(window.location.origin.replace(/\/+$/, ''))
    expect(config.forgetPasswordPath.endsWith('/ForgetPassword/')).toBe(true)
  })

  it('同时挂到 window 上，供非模块化脚本读取', async () => {
    const config = await loadConfig()
    expect(window.__APP_CONFIG__).toBe(config)
  })
})
