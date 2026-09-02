// Anonymous passthrough: complete a forgotten-password reset with a ticket.
// The backend returns 204 and no token; there is no auto-login.
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  try {
    return await $fetch(`${backendBaseUrl(event)}/api/student/password/reset`, { method: 'POST', body })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 502)
    return e.data ?? { detail: 'Password reset failed' }
  }
})
