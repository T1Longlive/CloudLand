const normalizeBaseUrl = (url) => url.replace(/\/+$/, '')

const apiBaseUrl = normalizeBaseUrl(process.env.VUE_APP_API_BASE_URL || 'http://localhost:9090')
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
