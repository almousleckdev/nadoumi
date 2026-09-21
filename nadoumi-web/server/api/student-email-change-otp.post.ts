// Cookie-authed passthrough: signed-in student requesting a code to prove they
// control a new sign-in email, before anything changes.
export default defineEventHandler(async (event) => {
  const token = studentToken(event)
  if (!token) {
    setResponseStatus(event, 401)
    return { detail: 'Not signed in' }
  }
  const body = await readBody(event)
  try {
    return await $fetch<unknown>(`${backendBaseUrl(event)}/api/student/email/change/otp`, {
      method: 'POST',
      body,
      headers: { authorization: `Bearer ${token}` },
    })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 502)
    return e.data ?? { detail: 'Could not send the code' }
  }
})
