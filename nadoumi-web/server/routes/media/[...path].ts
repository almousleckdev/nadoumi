// Same-origin passthrough for catalog imagery served by the backend under
// `/profile/**` (RuoYi's upload dir). Stored refs are path-only, so the web
// renders `<img src="/media/profile/upload/…">` and this route forwards it —
// works in dev and prod without baking the backend host into the DB.
export default defineEventHandler((event) => {
  const path = getRouterParam(event, 'path') ?? ''
  return proxyRequest(event, `${backendBaseUrl(event)}/${path}`)
})
