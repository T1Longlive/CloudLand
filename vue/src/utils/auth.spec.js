import { beforeEach, describe, expect, it, vi } from 'vitest'

// 只 mock 请求层：被测的是 token 生命周期与缓存策略，不是 axios。
vi.mock('@/request/axiosInstance', () => ({
  default: { post: vi.fn() },
}))

import axiosInstance from '@/request/axiosInstance'
import {
  clearAuthState,
  createEmptyUser,
  fetchCurrentUser,
  getCachedLoginState,
  getStoredToken,
  hasPersistentToken,
  invalidateLoginState,
  loginWithPassword,
  persistToken,
  setCachedLoginState,
} from './auth'

const post = axiosInstance.post

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
  post.mockReset()
  invalidateLoginState()
  vi.useRealTimers()
})

describe('token 存取', () => {
  it('没有任何 token 时 hasPersistentToken 为 false', () => {
    expect(hasPersistentToken()).toBe(false)
  })

  it('字面量 "null" 不算有效 token', () => {
    // 历史遗留：某些路径曾把 null 写成字符串存进去，这里必须挡掉，
    // 否则前端会带着 "null" 去请求，后端一律 401。
    localStorage.setItem('token', 'null')
    expect(hasPersistentToken()).toBe(false)
    expect(getStoredToken()).toBeNull()
  })

  it('getStoredToken 优先取 sessionStorage', () => {
    localStorage.setItem('token', 'local-token')
    sessionStorage.setItem('token', 'session-token')
    expect(getStoredToken()).toBe('session-token')
  })

  it('session 没有时回退到 localStorage', () => {
    localStorage.setItem('token', 'local-token')
    expect(getStoredToken()).toBe('local-token')
  })
})

describe('persistToken', () => {
  it('记住我：local 与 session 都写', () => {
    persistToken('t1', true)
    expect(localStorage.getItem('token')).toBe('t1')
    expect(sessionStorage.getItem('token')).toBe('t1')
  })

  it('不记住：只写 session，并清掉可能残留的 local', () => {
    localStorage.setItem('token', 'old')
    persistToken('t2', false)
    expect(localStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('token')).toBe('t2')
  })

  it('非法 token 不写入，也不破坏已有登录态', () => {
    localStorage.setItem('token', 'keep-me')
    persistToken(null, true)
    persistToken('null', true)
    expect(localStorage.getItem('token')).toBe('keep-me')
  })
})

describe('clearAuthState', () => {
  it('清掉 token/uid/userID 并让登录态缓存失效', () => {
    localStorage.setItem('token', 't')
    sessionStorage.setItem('token', 't')
    sessionStorage.setItem('uid', '1')
    sessionStorage.setItem('userID', '1')
    setCachedLoginState(true)

    clearAuthState()

    expect(localStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('uid')).toBeNull()
    expect(sessionStorage.getItem('userID')).toBeNull()
    expect(getCachedLoginState()).toBeNull()
  })
})

describe('登录态缓存', () => {
  it('false 也是有效结果，必须能命中', () => {
    // 这是最容易写错的一处：用 if (result) 判断的话，
    // "未登录"这个结果永远不会被缓存，路由守卫每次都打后端。
    setCachedLoginState(false)
    expect(getCachedLoginState()).toBe(false)
  })

  it('超过 TTL（5 分钟）后失效', () => {
    vi.useFakeTimers()
    setCachedLoginState(true)

    vi.advanceTimersByTime(5 * 60 * 1000 - 1)
    expect(getCachedLoginState()).toBe(true)

    vi.advanceTimersByTime(2)
    expect(getCachedLoginState()).toBeNull()
  })
})

describe('fetchCurrentUser', () => {
  it('没有 token 时不发请求', async () => {
    const result = await fetchCurrentUser()
    expect(result).toEqual({ user: null, res: null })
    expect(post).not.toHaveBeenCalled()
  })

  it('token 有效时返回用户并续期 token', async () => {
    sessionStorage.setItem('token', 'old-token')
    post.mockResolvedValue({
      data: { code: 20005, msg: 'new-token', data: { id: 1, username: '张三' } },
    })

    const { user } = await fetchCurrentUser()

    expect(user).toEqual({ id: 1, username: '张三' })
    expect(sessionStorage.getItem('token')).toBe('new-token')
    expect(post.mock.calls[0][1]).toEqual(createEmptyUser())
    expect(post.mock.calls[0][2].headers.token).toBe('old-token')
    expect(post.mock.calls[0][2].headers.frond).toBe('true')
  })

  it('token 过期（20003）时清空登录态', async () => {
    sessionStorage.setItem('token', 'expired')
    post.mockResolvedValue({ data: { code: 20003, msg: 'token 过期' } })

    const { user } = await fetchCurrentUser()

    expect(user).toBeNull()
    expect(sessionStorage.getItem('token')).toBeNull()
  })
})

describe('loginWithPassword', () => {
  it('成功时持久化 token 并写入登录态缓存', async () => {
    post.mockResolvedValue({
      data: { code: 20005, msg: 'jwt', data: { id: 2, username: '李四' } },
    })

    const { user } = await loginWithPassword(
      { phone: '19900000001', password: 'Test@123456' },
      { remember: true, code: '000000' }
    )

    expect(user.id).toBe(2)
    expect(localStorage.getItem('token')).toBe('jwt')
    expect(getCachedLoginState()).toBe(true)
    expect(post.mock.calls[0][2].headers.code).toBe('000000')
  })

  it('失败时不写 token、不污染缓存', async () => {
    post.mockResolvedValue({ data: { code: 20002, msg: '密码错误' } })

    const { user, res } = await loginWithPassword({ phone: 'x', password: 'y' })

    expect(user).toBeNull()
    expect(res.code).toBe(20002)
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(getCachedLoginState()).toBeNull()
  })

  it('未传 code 时请求头里不应出现该字段', async () => {
    post.mockResolvedValue({ data: { code: 20002, msg: '密码错误' } })
    await loginWithPassword({ phone: 'x', password: 'y' })
    expect(post.mock.calls[0][2].headers).not.toHaveProperty('code')
  })
})
