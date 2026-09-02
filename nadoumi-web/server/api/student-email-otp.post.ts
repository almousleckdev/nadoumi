// Anonymous passthrough: request an email one-time code. The backend always
// answers 200 { sent: true } regardless of whether the address is registered.
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  try {
    return await $fetch<unknown>(`${backendBaseUrl(event)}/api/student/email-otp`, { method: 'POST', body })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 502)
    return e.data ?? { detail: 'Could not send the code' }
  }
})
