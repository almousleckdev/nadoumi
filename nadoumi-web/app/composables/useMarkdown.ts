import MarkdownIt, { type StateCore, type Token } from 'markdown-it'
import { embedFromUrl, splitImageSize } from '~/utils/articleDialect'

// Raw HTML in the source is escaped (html: false) and markdown-it rejects javascript:/data: links,
// so the rendered string is safe to bind with v-html. The two block conventions below (figure, embed)
// build their markup here from escaped values and an allow-listed iframe source, never from the input.
const md = new MarkdownIt({ html: false, linkify: true, typographer: true })

const defaultLinkOpen = md.renderer.rules.link_open
  ?? ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  const token = tokens[idx]!
  token.attrSet('target', '_blank')
  token.attrSet('rel', 'noopener noreferrer nofollow ugc')
  return defaultLinkOpen(tokens, idx, options, env, self)
}

md.renderer.rules.image = (tokens, idx, options, env, self) => {
  const token = tokens[idx]!
  token.attrSet('loading', 'lazy')
  token.attrSet('decoding', 'async')
  token.attrSet('alt', token.content)
  return self.renderToken(tokens, idx, options)
}

const esc = md.utils.escapeHtml
const EMBED_SANDBOX = 'allow-scripts allow-same-origin allow-presentation allow-popups'

function attrOf(token: Token, name: string): string | null {
  const value = token.attrGet(name)
  return value === null ? null : String(value)
}

/** `![alt](url#wide "caption")` alone in a paragraph becomes a <figure>. */
function figureOf(image: Token): string {
  const { src, size } = splitImageSize(attrOf(image, 'src') ?? '')
  const caption = attrOf(image, 'title')
  return `<figure class="nad-fig nad-fig--${size}">`
    + `<img src="${esc(src)}" alt="${esc(image.content)}" loading="lazy" decoding="async">`
    + (caption ? `<figcaption>${esc(caption)}</figcaption>` : '')
    + '</figure>\n'
}

/** `<https://youtu.be/ID>` alone in a paragraph becomes a sandboxed player on the provider's embed host. */
function embedOf(link: Token, text: Token): string | null {
  const href = attrOf(link, 'href')
  if (!href || href !== text.content) return null
  const embed = embedFromUrl(href)
  if (!embed) return null
  return `<div class="nad-embed"><iframe src="${esc(embed.src)}" title="${esc(embed.provider)}" loading="lazy" `
    + `referrerpolicy="strict-origin-when-cross-origin" sandbox="${EMBED_SANDBOX}" `
    + 'allow="fullscreen; picture-in-picture" allowfullscreen></iframe></div>\n'
}

function blockOfParagraph(inline: Token): string | null {
  const kids = inline.children ?? []
  if (kids.length === 1 && kids[0]!.type === 'image') return figureOf(kids[0]!)
  if (kids.length === 3 && kids[0]!.type === 'link_open' && kids[1]!.type === 'text' && kids[2]!.type === 'link_close') {
    return embedOf(kids[0]!, kids[1]!)
  }
  return null
}

md.core.ruler.after('inline', 'nad_blocks', (state: StateCore) => {
  const { tokens } = state
  for (let i = 0; i + 2 < tokens.length; i++) {
    if (tokens[i]!.type !== 'paragraph_open' || tokens[i + 1]!.type !== 'inline' || tokens[i + 2]!.type !== 'paragraph_close') continue
    const html = blockOfParagraph(tokens[i + 1]!)
    if (html === null) continue
    const block = new state.Token('html_block', '', 0)
    block.content = html
    block.block = true
    tokens.splice(i, 3, block)
  }
})

export function renderMarkdown(source: string): string {
  return md.render(source)
}
