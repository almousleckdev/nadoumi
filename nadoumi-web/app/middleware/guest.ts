export default defineNuxtRouteMiddleware(async () => {
  const { status, refresh } = useSession()
  const localePath = useLocalePath()
  if (status.value === 'unknown') await refresh().catch(() => undefined)
  if (status.value === 'authed') return navigateTo(localePath('/dashboard'))
})
