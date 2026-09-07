// BFF passthrough for the anonymous catalog API. No credentials attached.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  // proxyRequest uses `target` verbatim — the incoming query string (filters,
  // paging, sort) must be carried over explicitly or every filter is silently lost.
  const search = getRequestURL(event).search
  const target = `${backendBaseUrl(event)}/api/public/${path}${search}`
  return proxyRequest(event, target)
})
