/**
 * Resolve a stored image reference to a URL the browser can load.
 * New uploads store a path only (`/profile/upload/…`) → serve it through the
 * same-origin `/media/**` Nitro proxy. Older rows may carry an absolute
 * `http(s)://…` URL — pass those through unchanged.
 */
export function mediaUrl(ref?: string | null): string {
  if (!ref) return ''
  if (/^https?:\/\//i.test(ref) || ref.startsWith('data:')) return ref
  return `/media${ref.startsWith('/') ? '' : '/'}${ref}`
}
