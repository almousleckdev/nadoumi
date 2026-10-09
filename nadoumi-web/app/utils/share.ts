/** Social share links for an article. Every value is URL-encoded so a title can never alter the query. */
export type ShareNetwork = 'x' | 'facebook' | 'linkedin' | 'whatsapp'

export interface ShareTarget {
  key: ShareNetwork
  href: string
}

/** Only real web pages can be shared; anything else (javascript:, data:, relative paths) is refused. */
export function isShareableUrl(url: string): boolean {
  try {
    const { protocol } = new URL(url)
    return protocol === 'https:' || protocol === 'http:'
  }
  catch {
    return false
  }
}

export function shareTargets(url: string, title: string): ShareTarget[] {
  if (!isShareableUrl(url)) return []
  const u = encodeURIComponent(url)
  const text = encodeURIComponent(title)
  return [
    { key: 'x', href: `https://twitter.com/intent/tweet?url=${u}&text=${text}` },
    { key: 'facebook', href: `https://www.facebook.com/sharer/sharer.php?u=${u}` },
    { key: 'linkedin', href: `https://www.linkedin.com/sharing/share-offsite/?url=${u}` },
    { key: 'whatsapp', href: `https://wa.me/?text=${encodeURIComponent(`${title} ${url}`)}` },
  ]
}
