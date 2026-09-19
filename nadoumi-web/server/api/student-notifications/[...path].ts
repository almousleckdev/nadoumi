// BFF passthrough for the authenticated student notifications API. Attaches the bearer token
// from the httpOnly cookie; the browser never sees the raw JWT.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }
  const search = getRequestURL(event).search
  const target = `${backendBaseUrl(event)}/api/notifications/${path}${search}`
  return proxyRequest(event, target, {
    headers: { authorization: `Bearer ${token}` },
  })
})
