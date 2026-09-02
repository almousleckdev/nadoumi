export default defineNuxtRouteMiddleware(async (to) => {
  const { status, refresh } = useSession()
  const localePath = useLocalePath()
  if (status.value === 'unknown') await refresh().catch(() => undefined)
  if (status.value !== 'authed') {
    return navigateTo({ path: localePath('/login'), query: { redirect: to.fullPath } })
  }
})
