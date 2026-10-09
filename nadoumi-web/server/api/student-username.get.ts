// Live username availability for the register form. Anonymous (nobody is signed in yet) and rate limited by the
// backend per IP; only the handle is forwarded.
export default defineEventHandler(async (event) => {
  const username = String(getQuery(event).username ?? '').slice(0, 64)
  try {
    return await $fetch(`${backendBaseUrl(event)}/api/student/username-available`, { query: { username } })
  }
  catch (err) {
    const e = err as { statusCode?: number }
    throw createError({ statusCode: e.statusCode ?? 502, statusMessage: 'Username check unavailable' })
  }
})
