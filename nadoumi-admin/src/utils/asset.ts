const API_BASE = import.meta.env.VITE_APP_BASE_API || '/dev-api'

/**
 * Resolve a stored asset reference to a URL the admin can load.
 * New uploads store a path only (`/profile/upload/...`); older rows may still
 * carry an absolute `http(s)://…` URL — pass those through unchanged.
 */
export function assetUrl(ref: string | null | undefined): string {
  if (!ref) return ''
  if (/^https?:\/\//i.test(ref) || ref.startsWith('data:')) return ref
  return `${API_BASE}${ref.startsWith('/') ? '' : '/'}${ref}`
}
