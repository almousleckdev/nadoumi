// BFF passthrough for the anonymous catalog API. No credentials attached.
export default defineEventHandler(async (event) => {
  const path = getRouterParam(event, 'path') ?? ''
  const target = `${backendBaseUrl(event)}/api/public/${path}`
  return proxyRequest(event, target)
})
