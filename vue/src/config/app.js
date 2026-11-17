const normalizeBaseUrl = (url) => url.replace(/\/+$/, '')

// 兜底值必须带 /api：后端 context-path 是 /api，少了它所有接口都 404。
// 之前这里写的是 'http://localhost:9090'（少 /api），本地一直没暴露，
// 是因为仓库里有一份未跟踪的 vue/.env.local 覆盖了它——换台机器/进 CI 就全挂。
const apiBaseUrl = normalizeBaseUrl(process.env.VUE_APP_API_BASE_URL || 'http://localhost:9090/api')
const frontendBaseUrl = normalizeBaseUrl(process.env.VUE_APP_FRONTEND_BASE_URL || window.location.origin)

export const APP_CONFIG = {
  apiBaseUrl,
  frontendBaseUrl,
  resourceUrls: {
    userFile: `${apiBaseUrl}/resource/userFile/`,
    landFile: `${apiBaseUrl}/resource/landFile/`,
    productFile: `${apiBaseUrl}/resource/productFile/`,
  },
  forgetPasswordPath: `${frontendBaseUrl}/ForgetPassword/`,
}

window.__APP_CONFIG__ = APP_CONFIG
