/**
 * True when `path` is safe to pass to navigateTo() as a post-login redirect —
 * a same-site path only. Rejects protocol-relative (`//host`), backslash
 * (browsers treat `\` like `/` in a URL, so `/\evil.com` becomes `//evil.com`)
 * and scheme-separator (`:`, e.g. `javascript:`) forms that could otherwise
 * send the user off-site.
 */
export function isSafeRedirectPath(path: string | undefined | null): path is string {
  return !!path && path.startsWith('/') && !path.startsWith('//') && !path.includes('\\') && !path.includes(':')
}
