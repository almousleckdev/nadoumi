/**
 * Resolve an image reference from an API response to a loadable URL.
 *
 * - Absolute (`https://…` Cloudinary secure_url / signed URL, `data:`, `blob:`)
 *   → returned unchanged.
 * - Relative (`/profile/…` legacy RuoYi uploads, other server-served paths)
 *   → prefixed with the API origin (`VITE_APP_BASE_API`), because the admin is
 *   served from a different origin than the backend.
 */
export function assetUrl(ref: string | null | undefined): string {
  if (!ref) return ''
  if (/^(https?:|data:|blob:)/i.test(ref)) return ref
  const base = (import.meta.env.VITE_APP_BASE_API || '/dev-api').replace(/\/$/, '')
  return base + (ref.startsWith('/') ? ref : '/' + ref)
}
