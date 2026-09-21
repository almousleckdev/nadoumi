import axios, { type AxiosInstance } from 'axios'
import { ElMessage } from 'element-plus'
import { API_BASE } from './apiBase'
import { CLIENT_HEADERS } from './session'
import router from '@/router'
import { useUserStore } from '@/stores/user'

/**
 * One client for two response styles:
 *  - RuoYi console endpoints: HTTP 200 + `{ code, msg, data?, token?, rows?, total? }`
 *  - Nadoumi `/api/**`: real status codes, bare bodies, RFC 9457 problem+json on errors
 */
const service: AxiosInstance = axios.create({
  baseURL: API_BASE,
  timeout: 15000,
  // the session is an httpOnly cookie; the API is reached through a same-origin proxy
  withCredentials: true,
  headers: CLIENT_HEADERS,
})

function toLogin() {
  const userStore = useUserStore()
  userStore.reset()
  // The start-up session probe also lands here on a 401; the router guard owns that redirect.
  if (!userStore.loaded) return
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
    // `silent` requests probe an outcome the caller handles itself (e.g. "am I a participant?")
    if (!(error.config as { silent?: boolean } | undefined)?.silent) notifyError(msg || 'Network error')
    return Promise.reject(error)
  },
)

export default service
