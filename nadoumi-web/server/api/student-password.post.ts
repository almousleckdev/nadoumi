// Cookie-authed passthrough: signed-in student changing their own password.
// Attaches the bearer from the httpOnly cookie; the password crosses to Spring once.
export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (!token) {
    setResponseStatus(event, 401)
    return { detail: 'Not signed in' }
  }
  const body = await readBody(event)
  try {
    return await $fetch(`${backendBaseUrl(event)}/api/student/password`, {
      method: 'POST',
      body,
      headers: { authorization: `Bearer ${token}` },
    })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 502)
    return e.data ?? { detail: 'Password change failed' }
  }
})
