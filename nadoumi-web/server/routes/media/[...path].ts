// Same-origin passthrough for catalog imagery served by the backend under
// `/profile/**` (RuoYi's upload dir). Stored refs are path-only, so the web
// renders `<img src="/media/profile/upload/…">` and this route forwards it —
// works in dev and prod without baking the backend host into the DB.
//
// The forwarded path is constrained to the `profile/` upload tree: without this
// guard `/media/<anything>` is an open reverse proxy to every backend route
// (`/druid/**`, `/swagger-ui.html`, `/api/dev/mail/latest`, …).
export default defineEventHandler((event) => {
  const path = getRouterParam(event, 'path') ?? ''
  if (!path.startsWith('profile/') || path.includes('..')) {
    throw createError({ statusCode: 404, statusMessage: 'Not Found' })
  }
  return proxyRequest(event, `${backendBaseUrl(event)}/${path}`)
})
