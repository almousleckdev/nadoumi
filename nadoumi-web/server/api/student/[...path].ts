// BFF passthrough for the authenticated student API. Attaches the bearer token
// from the httpOnly cookie; the browser never sees the raw JWT.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }
  const target = `${backendBaseUrl(event)}/api/student/${path}`
  return proxyRequest(event, target, {
    headers: { authorization: `Bearer ${token}` },
  })
})
