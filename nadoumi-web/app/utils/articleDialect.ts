/**
 * The Markdown conventions of a news article body, as written by the admin editor
 * (nadoumi-admin: src/views/news/editor/dialect.ts). Keep the two in step. See docs/FRONTEND_ARCHITECTURE.md.
 *
 *  - image width is a `#wide` / `#full` marker on the image url, the caption is the image title;
 *  - a paragraph holding nothing but a YouTube / Vimeo autolink is a video embed.
 */

export type ImageSize = 'inline' | 'wide' | 'full'

const SIZE_MARKER = /#(wide|full)$/

export function splitImageSize(src: string): { src: string, size: ImageSize } {
  const match = SIZE_MARKER.exec(src)
  if (!match) return { src, size: 'inline' }
  return { src: src.slice(0, match.index), size: match[1] as ImageSize }
}

export interface Embed {
  provider: 'youtube' | 'vimeo'
  /** Iframe source on the provider's embed host, always built here and never taken from the input. */
  src: string
}

const YOUTUBE_HOSTS = new Set(['www.youtube.com', 'youtube.com', 'm.youtube.com'])
const YOUTUBE_ID = /^[A-Za-z0-9_-]{11}$/
const VIMEO_HOSTS = new Set(['vimeo.com', 'www.vimeo.com', 'player.vimeo.com'])
const VIMEO_ID = /^\d{1,12}$/

function youtubeId(url: URL): string | null {
  if (url.hostname === 'youtu.be') return url.pathname.slice(1)
  if (!YOUTUBE_HOSTS.has(url.hostname)) return null
  if (url.pathname === '/watch') return url.searchParams.get('v')
  const embedded = /^\/embed\/([^/]+)$/.exec(url.pathname)
  return embedded ? embedded[1]! : null
}

function vimeoId(url: URL): string | null {
  if (!VIMEO_HOSTS.has(url.hostname)) return null
  const match = /^\/(?:video\/)?(\d+)$/.exec(url.pathname)
  return match ? match[1]! : null
}

/** Only https links to the allow-listed hosts with a well-formed id qualify. */
export function embedFromUrl(input: string): Embed | null {
  let url: URL
  try {
    url = new URL(input.trim())
  }
  catch {
    return null
  }
  if (url.protocol !== 'https:' || url.username || url.password) return null

  const yt = youtubeId(url)
  if (yt && YOUTUBE_ID.test(yt)) return { provider: 'youtube', src: `https://www.youtube-nocookie.com/embed/${yt}` }
  const vm = vimeoId(url)
  if (vm && VIMEO_ID.test(vm)) return { provider: 'vimeo', src: `https://player.vimeo.com/video/${vm}` }
  return null
}
