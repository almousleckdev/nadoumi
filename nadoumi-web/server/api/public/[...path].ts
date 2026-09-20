// BFF passthrough for the anonymous catalog API. No credentials attached.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  // proxyRequest uses `target` verbatim — the incoming query string (filters,
  // paging, sort) must be carried over explicitly or every filter is silently lost.
  const search = getRequestURL(event).search
  // Spring's default routing (trailing-slash matching disabled since Spring 6 /
  // Boot 3+) 404s on a bare `/api/public/` — only append the slash when there
  // is an actual sub-path.
  const target = `${backendBaseUrl(event)}/api/public${path ? `/${path}` : ''}${search}`
  return proxyRequest(event, target)
})
