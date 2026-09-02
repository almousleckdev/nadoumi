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
      ElMessage.error((data as any).msg || 'Request failed')
      return Promise.reject(new Error((data as any).msg || 'Request failed'))
    }
    return data
  },
  (error) => {
    const res = error.response
    let msg = error.message
    if (res) {
      const body = res.data || {}
      msg = body.detail || body.title || body.msg || msg
      if (res.status === 401) toLogin()
    }
    ElMessage.error(msg || 'Network error')
    return Promise.reject(error)
  },
)

export default service
