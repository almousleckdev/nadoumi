import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'
import router from '@/router'

/**
 * One client for two response styles:
 *  - RuoYi console endpoints: HTTP 200 + `{ code, msg, data?, token?, rows?, total? }`
 *  - Nadoumi `/api/**`: real status codes, bare bodies, RFC 9457 problem+json on errors
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || '/dev-api',
  timeout: 15000,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

function toLogin() {
  removeToken()
  if (router.currentRoute.value.path !== '/login') {
    router.replace(`/login?redirect=${encodeURIComponent(router.currentRoute.value.fullPath)}`)
  }
}

// One toast per distinct message within a short window, so a single failed
// request never stacks two identical error toasts.
let lastMsg = ''
let lastAt = 0
function notifyError(msg: string) {
  const now = Date.now()
  if (msg === lastMsg && now - lastAt < 1500) return
  lastMsg = msg
  lastAt = now
  ElMessage.error(msg)
}

function friendly(status: number | undefined, raw: string): string {
  if (!status) return raw === 'Network Error' ? 'Cannot reach the server. Check your connection and try again.' : raw
  if (status >= 500) return 'Something went wrong on the server. Please try again shortly.'
  if (status === 429) return 'Too many attempts. Please wait a moment and try again.'
  return raw
}

service.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && typeof data === 'object' && typeof (data as any).code === 'number') {
      const code = (data as any).code
      if (code === 200) return data
      if (code === 401) {
        toLogin()
        return Promise.reject(new Error((data as any).msg || 'Not authenticated'))
      }
      const msg = (data as any).msg || 'Request failed'
      notifyError(msg)
      return Promise.reject(new Error(msg))
    }
    return data
  },
  (error) => {
    const res = error.response
    let msg = error.message as string
    if (res) {
      const body = res.data || {}
      msg = body.detail || body.title || body.msg || friendly(res.status, msg)
      if (res.status === 401) toLogin()
    }
    else {
      msg = friendly(undefined, msg)
    }
    notifyError(msg || 'Network error')
    return Promise.reject(error)
  },
)

export default service
