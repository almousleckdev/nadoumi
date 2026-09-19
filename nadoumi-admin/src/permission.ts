import router from '@/router'
import { useUserStore } from '@/stores/user'

const WHITELIST = ['/login', '/404']

// All routes are static (src/router/index.ts, built from src/config/nav.ts).
// The guard only loads the signed-in user's roles/permissions once; screens and
// the sidebar gate their own content with userStore.hasPerm().
router.beforeEach(async (to) => {
  const userStore = useUserStore()
  await userStore.restore()

  if (!userStore.signedIn) {
    return WHITELIST.includes(to.path) ? true : `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }

  if (to.path === '/login') return '/'

  // A screen declares the permission it needs in its route meta. Without it the
  // page would just render and every API call would 403 — redirect to 404 so a
  // staff member never lands on a screen their role can't use.
  const perm = to.meta?.perm as string | undefined
  if (perm && !userStore.hasPerm(perm)) {
    return to.path === '/404' ? true : '/404'
  }
  return true
})
