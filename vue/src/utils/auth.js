import axiosInstance from "@/request/axiosInstance";

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
    return {user: res.data, res};
  }
  return {user: null, res};
}
