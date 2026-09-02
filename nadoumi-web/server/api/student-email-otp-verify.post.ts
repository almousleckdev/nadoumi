// Anonymous passthrough: exchange an email one-time code for a single-use ticket.
export default defineEventHandler(async (event) => {
  const body = await readBody(event)
  try {
    return await $fetch<unknown>(`${backendBaseUrl(event)}/api/student/email-otp/verify`, { method: 'POST', body })
  }
  catch (err) {
    const e = err as { statusCode?: number; data?: unknown }
    setResponseStatus(event, e.statusCode ?? 502)
    return e.data ?? { detail: 'Verification failed' }
  }
})
