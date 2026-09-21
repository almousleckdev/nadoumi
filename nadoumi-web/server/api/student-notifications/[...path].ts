// BFF passthrough for the authenticated student notifications API. Attaches the bearer token
// from the httpOnly cookie; the browser never sees the raw JWT.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  const token = studentToken(event)
  if (!token) {
    throw createError({ statusCode: 401, statusMessage: 'Not signed in' })
  }
  const search = getRequestURL(event).search
  // Spring's default routing (trailing-slash matching disabled since Spring 6 /
  // Boot 3+) 404s on a bare `/api/notifications/` — only append the slash when
  // there is an actual sub-path, so the base list endpoint resolves correctly.
  const target = `${backendBaseUrl(event)}/api/notifications${path ? `/${path}` : ''}${search}`
  return proxyRequest(event, target, {
    headers: { authorization: `Bearer ${token}` },
  })
})
