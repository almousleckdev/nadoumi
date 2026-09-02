// Student sign-in: exchange credentials at the Spring API, then keep the JWT in an
// httpOnly cookie. The token is never returned to the browser. Identity is
// email-first (spec Revision 2, D-R2-2).
export default defineEventHandler(async (event) => {
  const body = await readBody<{ email: string; password: string; code?: string; uuid?: string }>(event)
  try {
    const res = await $fetch<{ token: string }>(`${backendBaseUrl(event)}/api/student/login`, {
      method: 'POST',
      body: { email: body.email, password: body.password, code: body.code, uuid: body.uuid },
    })
    setStudentToken(event, res.token)
    return { signedIn: true }
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 400)
    return e.data ?? { detail: 'Sign-in failed' }
  }
})
