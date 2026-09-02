import router from '@/router'
import { useUserStore } from '@/stores/user'
import { getToken } from '@/utils/auth'

const WHITELIST = ['/login', '/404']

// All routes are static (src/router/index.ts, built from src/config/nav.ts).
// The guard only loads the signed-in user's roles/permissions once; screens and
// the sidebar gate their own content with userStore.hasPerm().
router.beforeEach(async (to) => {
  if (!getToken()) {
    return WHITELIST.includes(to.path) ? true : `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (to.path === '/login') return '/'

  const userStore = useUserStore()
  if (userStore.roles.length === 0) {
    try {
      await userStore.fetchInfo()
    }
    catch {
      userStore.reset()
      return `/login?redirect=${encodeURIComponent(to.fullPath)}`
    }
  }
  return true
})
