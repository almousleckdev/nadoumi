/**
 * Passthrough shim. Media references are now absolute URLs — Cloudinary
 * `secure_url` for public catalog imagery, short-lived signed URLs for protected
 * assets — so there is nothing left to resolve. Kept as a one-liner so existing
 * imports keep working; remove once every caller is gone.
 */
export function assetUrl(ref: string | null | undefined): string {
  return ref ?? ''
}
