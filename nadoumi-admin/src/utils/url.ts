/**
 * Returns `value` as an href only if it parses as an absolute http(s) URL —
 * `null` otherwise. Guards against a `javascript:` (or other executable
 * scheme) value stored in free-text fields like a university's website
 * ending up in a rendered `<a href>`.
 */
export function safeHref(value: unknown): string | null {
  if (typeof value !== 'string' || value.trim() === '') return null
  try {
    const url = new URL(value)
    return url.protocol === 'http:' || url.protocol === 'https:' ? value : null
  } catch {
    return null
  }
}
