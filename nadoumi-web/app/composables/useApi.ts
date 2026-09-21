/**
 * Calls the Nadoumi backend **through the Nitro BFF** (`/api/public/**`,
 * `/api/student/**`). The browser never talks to the Spring API directly and never
 * holds the raw JWT — the student token lives in an httpOnly cookie the BFF attaches.
 */
export function useApi() {
  // Nuxt's global $fetch does not forward the incoming request's cookies when called
  // server-side (SSR/plugins/middleware) — useRequestFetch() is the request-bound
  // equivalent that does, and behaves exactly like $fetch on the client. Without this,
  // every SSR-time call to our own httpOnly-cookie-gated BFF routes looks unauthenticated.
  const requestFetch = useRequestFetch()

  function publicGet<T>(path: string, query?: Record<string, unknown>) {
    return requestFetch<T>(`/api/public/${stripLeadingSlash(path)}`, { query })
  }

  function publicPost<T>(path: string, body: Record<string, unknown>) {
    return requestFetch<T>(`/api/public/${stripLeadingSlash(path)}`, { method: 'POST', body })
  }

  function studentFetch<T>(path: string, opts?: Parameters<typeof $fetch>[1]) {
    return requestFetch<T>(`/api/student/${stripLeadingSlash(path)}`, opts)
  }

  return { publicGet, publicPost, studentFetch }
}

function stripLeadingSlash(p: string) {
  return p.replace(/^\/+/, '')
}
