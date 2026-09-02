// Register + open a session entirely server-side (spec §8.1). The browser sends
// the password once and the verified-email `ticket`; it gets back only
// { signedIn: true } and never sees the JWT.
export default defineEventHandler(async (event) => {
  const body = await readBody<{
    firstName: string; lastName: string; email: string
    password: string; ticket: string
  }>(event)
  const base = backendBaseUrl(event)

  try {
    await $fetch(`${base}/api/student/register`, {
      method: 'POST',
      body: {
        firstName: body.firstName,
        lastName: body.lastName,
        email: body.email,
        password: body.password,
        ticket: body.ticket,
      },
    })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 400)
    return e.data ?? { detail: 'registration failed' }
  }

  const { token } = await $fetch<{ token: string }>(`${base}/api/student/login`, {
    method: 'POST',
    body: { email: body.email, password: body.password },
  })
  setStudentToken(event, token)
  return { signedIn: true }
})
