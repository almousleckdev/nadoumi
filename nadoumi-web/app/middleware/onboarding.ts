/**
 * Keeps onboarding and the dashboard separate:
 *  - a student with an incomplete profile is sent from /dashboard/** to /onboarding
 *  - a student who has finished onboarding is sent from /onboarding to /dashboard
 * Runs after `auth`, so the caller is already an authenticated student here.
 */
export default defineNuxtRouteMiddleware(async (to) => {
  const localePath = useLocalePath()
  const { ensure } = useOnboarding()
  const status = await ensure()

  const path = to.path
  const onOnboarding = path === localePath('/onboarding') || path.endsWith('/onboarding')

  if (status === 'incomplete' && !onOnboarding) {
    return navigateTo(localePath('/onboarding'))
  }
  if (status === 'complete' && onOnboarding) {
    return navigateTo(localePath('/dashboard'))
  }
})
