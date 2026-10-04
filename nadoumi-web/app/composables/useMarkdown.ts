import MarkdownIt from 'markdown-it'

// Raw HTML in the source is escaped (html: false) and markdown-it rejects javascript:/data: links,
// so the rendered string is safe to bind with v-html.
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

export function renderMarkdown(source: string): string {
  return md.render(source)
}
