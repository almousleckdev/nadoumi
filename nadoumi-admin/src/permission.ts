import router from '@/router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import { getToken } from '@/utils/auth'

const WHITELIST = ['/login', '/404']

router.beforeEach(async (to) => {
  const hasToken = getToken()

  if (!hasToken) {
    return WHITELIST.includes(to.path) ? true : `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (to.path === '/login') return '/'

  const userStore = useUserStore()
  const permissionStore = usePermissionStore()

  if (userStore.roles.length === 0) {
    try {
      await userStore.fetchInfo()
      const routes = await permissionStore.generateRoutes()
      routes.forEach((r) => router.addRoute(r))
      return { ...to, replace: true }
    } catch {
      userStore.reset()
      return `/login?redirect=${encodeURIComponent(to.fullPath)}`
    }
  }

  return true
})
