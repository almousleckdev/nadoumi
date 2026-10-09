import { mergeAttributes, Node, type AnyExtension, type Editor, type NodeViewRenderer } from '@tiptap/core'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Placeholder from '@tiptap/extension-placeholder'
import StarterKit from '@tiptap/starter-kit'
import CodeBlock from '@tiptap/extension-code-block'
import { Markdown } from 'tiptap-markdown'
import { embedFromUrl, imageSrcWithSize, splitImageSize, type ImageSize } from './dialect'

/** Vue node views are attached by the editor page; the schema and Markdown rules below work without them. */
export interface NewsNodeViews {
  image?: () => NodeViewRenderer
  embed?: () => NodeViewRenderer
  codeBlock?: () => NodeViewRenderer
}

export interface NewsExtensionOptions {
  placeholder: string
  nodeViews?: NewsNodeViews
}

const LINK_REL = 'noopener noreferrer nofollow ugc'
const ALLOWED_LINK = /^(https?:|mailto:)/i
const SIZE_ATTR = 'data-size'
const EMBED_ATTR = 'data-nad-embed'

/**
 * Image block: `![alt](url#wide "caption")`. The width lives in the url fragment while stored and in
 * the `size` node attribute while editing, so the editor never shows the marker as part of the url.
 */
const NadImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      size: {
        default: 'inline' as ImageSize,
        parseHTML: (el: HTMLElement) => el.getAttribute(SIZE_ATTR) ?? 'inline',
        renderHTML: (attrs: { size: ImageSize }) => ({ [SIZE_ATTR]: attrs.size }),
      },
    }
  },
  addStorage() {
    return {
      markdown: {
        serialize(state: MarkdownWriter, node: ImageNode) {
          const src = imageSrcWithSize(node.attrs.src, node.attrs.size).replace(/[()]/g, '\\$&')
          const title = node.attrs.title ? ` "${node.attrs.title.replace(/"/g, '\\"')}"` : ''
          state.write(`![${state.esc(node.attrs.alt ?? '')}](${src}${title})`)
          state.closeBlock(node)
        },
        parse: {
          updateDOM(element: HTMLElement) {
            element.querySelectorAll('img').forEach((img) => {
              const { src, size } = splitImageSize(img.getAttribute('src') ?? '')
              img.setAttribute('src', src)
              img.setAttribute(SIZE_ATTR, size)
            })
          },
        },
      },
    }
  },
})

/** A video on its own line. Stored as the bare autolink `<https://www.youtube.com/watch?v=ID>`. */
const NadEmbed = Node.create({
  name: 'embed',
  group: 'block',
  atom: true,
  draggable: true,
  addAttributes() {
    return {
      url: {
        default: null,
        parseHTML: (el: HTMLElement) => el.getAttribute(EMBED_ATTR),
        renderHTML: (attrs: { url: string | null }) => (attrs.url ? { [EMBED_ATTR]: attrs.url } : {}),
      },
    }
  },
  parseHTML() {
    return [{ tag: `div[${EMBED_ATTR}]` }]
  },
  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes)]
  },
  addStorage() {
    return {
      markdown: {
        serialize(state: MarkdownWriter, node: ImageNode) {
          state.write(`<${node.attrs.url}>`)
          state.closeBlock(node)
        },
        parse: {
          // a paragraph that is only a video autolink is an embed; anything else stays a normal link
          updateDOM(element: HTMLElement) {
            element.querySelectorAll('p').forEach((p) => {
              const link = p.childNodes.length === 1 ? p.firstElementChild : null
              const href = link?.tagName === 'A' ? link.getAttribute('href') : null
              if (!href || href !== link?.textContent) return
              const embed = embedFromUrl(href)
              if (!embed) return
              const block = element.ownerDocument.createElement('div')
              block.setAttribute(EMBED_ATTR, embed.url)
              p.replaceWith(block)
            })
          },
        },
      },
    }
  },
})

function withView<T extends AnyExtension & { extend: (config: object) => T }>(base: T, view?: () => NodeViewRenderer): T {
  return view ? base.extend({ addNodeView: view }) : base
}

export function createNewsExtensions({ placeholder, nodeViews = {} }: NewsExtensionOptions): AnyExtension[] {
  return [
    StarterKit.configure({
      heading: { levels: [1, 2, 3] },
      codeBlock: false,
      dropcursor: { color: '#1a8917', width: 2 },
    }),
    withView(CodeBlock, nodeViews.codeBlock),
    withView(NadImage.configure({ inline: false, allowBase64: false }), nodeViews.image),
    withView(NadEmbed, nodeViews.embed),
    Link.configure({
      openOnClick: false,
      autolink: true,
      linkOnPaste: true,
      HTMLAttributes: { rel: LINK_REL, target: '_blank' },
      isAllowedUri: (url, ctx) => ctx.defaultValidate(url) && ALLOWED_LINK.test(url),
    }),
    Placeholder.configure({ placeholder }),
    // html: false keeps the parser identical to the public renderer: raw tags are text, never elements
    Markdown.configure({ html: false, tightLists: true, bulletListMarker: '-', linkify: false, breaks: false }),
  ]
}

/** The article body as stored: the editor's document serialised to Markdown. */
export function markdownOf(editor: Editor): string {
  return (editor.storage as unknown as { markdown: { getMarkdown: () => string } }).markdown.getMarkdown()
}

/** The slice of prosemirror-markdown's serializer state the specs above use. */
interface MarkdownWriter {
  write: (text: string) => void
  esc: (text: string) => string
  closeBlock: (node: unknown) => void
}

interface ImageNode {
  attrs: { src: string, alt: string | null, title: string | null, size: ImageSize, url: string }
}
