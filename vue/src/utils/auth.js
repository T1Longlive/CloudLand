import axiosInstance from "@/request/axiosInstance";

// ==================== 登录态缓存（供路由守卫使用） ====================
// 背景：路由守卫原先每次导航都 POST /user/login 校验 token，导航密集时请求放大。
// 策略：内存缓存校验结果 + TTL（5 分钟内复用），登录成功 / 清除登录态时主动失效，
// 保证服务端禁用账号等变更最迟 5 分钟（或下一次登录态变更）生效。
const LOGIN_CHECK_TTL = 5 * 60 * 1000;
const loginStateCache = {result: null, expiresAt: 0};

/** 失效缓存：登录态发生变化（登录成功 / 登出 / token 过期清除）时调用 */
export function invalidateLoginState() {
    loginStateCache.result = null;
    loginStateCache.expiresAt = 0;
}

/** 读取未过期的缓存结果；无缓存或已过期返回 null */
export function getCachedLoginState() {
    if (loginStateCache.result !== null && Date.now() < loginStateCache.expiresAt) {
        return loginStateCache.result;
    }
    return null;
}

/** 写入缓存结果 */
export function setCachedLoginState(loggedIn) {
    loginStateCache.result = loggedIn;
    loginStateCache.expiresAt = Date.now() + LOGIN_CHECK_TTL;
}

export function createEmptyUser() {
  return {
    id: null,
    username: null,
    password: null,
    phone: null,
    age: null,
    address: null,
    img: null,
    status: 1,
    detailedAddress: null,
    power: 0,
    mail: null
  };
}

export function hasPersistentToken() {
  const token = localStorage.getItem('token');
  return token !== undefined && token !== null && token !== 'null';
}

export function getStoredToken() {
  const sessionToken = sessionStorage.getItem('token');
  if (sessionToken !== undefined && sessionToken !== null && sessionToken !== 'null') {
    return sessionToken;
  }
  const localToken = localStorage.getItem('token');
  if (localToken !== undefined && localToken !== null && localToken !== 'null') {
    return localToken;
  }
  return null;
}

export function persistToken(token, remember = hasPersistentToken()) {
  if (token === undefined || token === null || token === 'null') {
    return;
  }
  if (remember) {
    localStorage.setItem('token', token);
  } else {
    localStorage.removeItem('token');
  }
  sessionStorage.setItem('token', token);
}

export function clearAuthState() {
  localStorage.removeItem('token');
  sessionStorage.removeItem('token');
  sessionStorage.removeItem('uid');
  sessionStorage.removeItem('userID');
  invalidateLoginState();
}

export async function fetchCurrentUser(frond = true) {
  const token = getStoredToken();
  if (!token) {
    return {user: null, res: null};
  }
  const remember = hasPersistentToken();
  const config = {
    headers: {
      token,
      remember: String(remember),
      frond: String(frond),
    }
  };
  const {data: res} = await axiosInstance.post('/user/login', createEmptyUser(), config);
  if (res.code === 20005) {
    persistToken(res.msg, remember);
    return {user: res.data, res};
  }
  if (res.code === 20003) {
    clearAuthState();
  }
  return {user: null, res};
}

export async function loginWithPassword(user, {frond = true, remember = false, code = null} = {}) {
  const headers = {
    token: null,
    remember: String(remember),
    frond: String(frond),
  };
  if (code !== null && code !== undefined) {
    headers.code = code;
  }
  const {data: res} = await axiosInstance.post('/user/login', user, {headers});
  if (res.code === 20005) {
    persistToken(res.msg, remember);
    setCachedLoginState(true);
    return {user: res.data, res};
  }
  return {user: null, res};
}
