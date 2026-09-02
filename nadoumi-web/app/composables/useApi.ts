/**
 * Calls the Nadoumi backend **through the Nitro BFF** (`/api/public/**`,
 * `/api/student/**`). The browser never talks to the Spring API directly and never
 * holds the raw JWT — the student token lives in an httpOnly cookie the BFF attaches.
 */
export function useApi() {
  function publicGet<T>(path: string, query?: Record<string, unknown>) {
    return $fetch<T>(`/api/public/${stripLeadingSlash(path)}`, { query })
  }

  function studentFetch<T>(path: string, opts?: Parameters<typeof $fetch>[1]) {
    return $fetch<T>(`/api/student/${stripLeadingSlash(path)}`, opts)
  }

  return { publicGet, studentFetch }
}

function stripLeadingSlash(p: string) {
  return p.replace(/^\/+/, '')
}

interface ProblemLike { data?: { detail?: string; title?: string }; message?: string }

export function problemMessage(err: unknown, fallback: string): string {
  const e = err as ProblemLike
  return e?.data?.detail || e?.data?.title || e?.message || fallback
}
