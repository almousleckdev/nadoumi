/**
 * The Markdown dialect of a news article body (see docs/FRONTEND_ARCHITECTURE.md, "Article body format").
 *
 * Everything stays plain, portable Markdown; the editor only adds two conventions on top of it:
 *  - image width is a `#wide` / `#full` marker on the image url, the caption is the image title;
 *  - a paragraph holding nothing but a YouTube / Vimeo autolink is a video embed.
 *
 * nadoumi-web renders the same conventions (app/composables/useMarkdown.ts). Keep the two in step.
 */

export type ImageSize = 'inline' | 'wide' | 'full'

const SIZE_MARKER = /#(wide|full)$/

/** Splits an image url into the clean url and the width marker it carried. */
export function splitImageSize(src: string): { src: string, size: ImageSize } {
  const match = SIZE_MARKER.exec(src)
  if (!match) return { src, size: 'inline' }
  return { src: src.slice(0, match.index), size: match[1] as ImageSize }
}

/** The url with exactly one width marker for `size` (none for inline). */
export function imageSrcWithSize(src: string, size: ImageSize): string {
  const { src: clean } = splitImageSize(src)
  return size === 'inline' ? clean : `${clean}#${size}`
}

export type EmbedProvider = 'youtube' | 'vimeo'

export interface Embed {
  provider: EmbedProvider
  /** Canonical url, the form stored in the article body. */
  url: string
  /** Iframe source on the provider's embed host. */
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

/**
 * Resolves a pasted link to an embeddable video, or null. Only https links to the allow-listed hosts
 * with a well-formed id qualify, so the iframe source is always built here, never taken from the input.
 */
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
  if (yt && YOUTUBE_ID.test(yt)) {
    return {
      provider: 'youtube',
      url: `https://www.youtube.com/watch?v=${yt}`,
      src: `https://www.youtube-nocookie.com/embed/${yt}`,
    }
  }
  const vm = vimeoId(url)
  if (vm && VIMEO_ID.test(vm)) {
    return { provider: 'vimeo', url: `https://vimeo.com/${vm}`, src: `https://player.vimeo.com/video/${vm}` }
  }
  return null
}

const LINK_SCHEME = /^(https?:|mailto:)/i
const BARE_EMAIL = /^[^\s@/]+@[^\s@/]+\.[^\s@/]+$/
const BARE_DOMAIN = /^(?:www\.)?[a-z0-9-]+(?:\.[a-z0-9-]+)+(?:[/?#]\S*)?$/i

/**
 * Turns what an editor typed into the link box into a safe href, or null. Only http(s) and mailto
 * survive; a bare domain gets https and a bare address gets mailto. Anything else is not a link.
 */
export function normalizeLink(input: string): string | null {
  const value = input.trim()
  if (value === '') return null
  if (LINK_SCHEME.test(value)) {
    if (/^mailto:/i.test(value)) return value.length > 'mailto:'.length ? value : null
    try {
      return new URL(value).href === '' ? null : value
    }
    catch {
      return null
    }
  }
  if (BARE_EMAIL.test(value)) return `mailto:${value}`
  if (BARE_DOMAIN.test(value)) return `https://${value}`
  return null
}

export function wordCount(text: string): number {
  const trimmed = text.trim()
  return trimmed === '' ? 0 : trimmed.split(/\s+/).length
}
