import type { H3Event } from 'h3'

/**
 * Shared by student-notifications/index.ts and .../[...path].ts. Nitro's
 * `[...path]` catch-all only matches requests with at least one segment after
 * the directory — a bare `GET /api/student-notifications` (the list endpoint,
 * no sub-path) 404s at the Nitro routing layer before this file is ever
 * reached. The sibling `index.ts` covers exactly that missing case; both files
 * delegate here so the proxying logic exists once.
 */
export function studentNotificationsProxy(event: H3Event) {
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
}
