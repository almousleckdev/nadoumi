export default defineEventHandler(async (event) => {
  const body = await readBody<{
    fullName?: string; username: string; email?: string
    password: string; code?: string; uuid?: string
  }>(event)
  const base = backendBaseUrl(event)

  try {
    await $fetch(`${base}/api/student/register`, {
      method: 'POST',
      body: {
        username: body.username,
        password: body.password,
        nickName: body.fullName,
        email: body.email,
        code: body.code,
        uuid: body.uuid,
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
    body: { username: body.username, password: body.password, code: body.code, uuid: body.uuid },
  })
  setStudentToken(event, token)
  return { signedIn: true }
})
