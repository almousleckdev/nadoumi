/**
 * Shim over an image reference coming from a public API response.
 *
 * Public responses now carry resolved `*Url` fields that are absolute
 * (Cloudinary / fake CDN) whenever a media id is set — those pass through
 * unchanged. During the deprecation window the legacy `*ImageUrl` columns still
 * exist, so an un-migrated row can resolve to a relative `/profile/...` path;
 * that is routed through the same-origin `/media/**` Nitro proxy.
 *
 * Remove this shim and the `server/routes/media/**` proxy in the same release
 * that drops the legacy `*ImageUrl` fields.
 */
export function mediaUrl(ref?: string | null): string {
  if (!ref) return ''
  if (/^https?:\/\//i.test(ref) || ref.startsWith('data:')) return ref
  return `/media${ref.startsWith('/') ? '' : '/'}${ref}`
}
