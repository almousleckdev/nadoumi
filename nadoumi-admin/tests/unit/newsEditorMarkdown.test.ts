import { afterEach, describe, expect, it } from 'vitest'
import { Editor } from '@tiptap/core'
import { createNewsExtensions, markdownOf } from '@/views/news/editor/extensions'

let editor: Editor | null = null

function load(markdown: string): Editor {
  editor = new Editor({ extensions: createNewsExtensions({ placeholder: '' }), content: markdown })
  return editor
}

/** Loads a body into the editor and reads it back: the stored form must survive an edit session. */
function roundTrip(markdown: string): string {
  return markdownOf(load(markdown))
}

afterEach(() => {
  editor?.destroy()
  editor = null
})

describe('article body round trip', () => {
  it.each([
    ['headings and paragraphs', '## Heading\n\n### Subheading\n\nA paragraph.'],
    ['bold, italic and links', 'Some **bold**, some *italic* and a [link](https://example.com/a).'],
    ['a quote', '> A quote worth keeping.'],
    ['lists', '- one\n- two\n\n1. first\n2. second'],
    ['a divider', 'Before\n\n---\n\nAfter'],
    ['a code block with a language', '```java\nint x = 1;\n```'],
    ['a code block without a language', '```\nplain\n```'],
  ])('keeps %s unchanged', (_label, markdown) => {
    expect(roundTrip(markdown)).toBe(markdown)
  })

  it('keeps an inline image with alt text and a caption', () => {
    const md = '![A shop front](https://cdn.example/a.jpg "A local shop")'
    expect(roundTrip(md)).toBe(md)
  })

  it('keeps the wide and full image width markers', () => {
    expect(roundTrip('![](https://cdn.example/a.jpg#wide)')).toBe('![](https://cdn.example/a.jpg#wide)')
    expect(roundTrip('![](https://cdn.example/a.jpg#full "Edge to edge")')).toBe('![](https://cdn.example/a.jpg#full "Edge to edge")')
  })

  it('exposes the image width and caption as node attributes, not as part of the url', () => {
    const e = load('![alt](https://cdn.example/a.jpg#wide "Cap")')
    const image = e.state.doc.firstChild!
    expect(image.type.name).toBe('image')
    expect(image.attrs).toMatchObject({ src: 'https://cdn.example/a.jpg', alt: 'alt', title: 'Cap', size: 'wide' })
  })

  it('turns a paragraph holding only a video link into an embed and writes it back as that link', () => {
    const md = 'Intro\n\n<https://www.youtube.com/watch?v=aX5DXP9DJ_c>\n\nOutro'
    const e = load(md)
    expect(e.state.doc.child(1).type.name).toBe('embed')
    expect(e.state.doc.child(1).attrs.url).toBe('https://www.youtube.com/watch?v=aX5DXP9DJ_c')
    expect(markdownOf(e)).toBe(md)
  })

  it('normalises a short youtube link to the canonical embed url', () => {
    const e = load('<https://youtu.be/aX5DXP9DJ_c>')
    expect(e.state.doc.firstChild!.attrs.url).toBe('https://www.youtube.com/watch?v=aX5DXP9DJ_c')
  })

  it('leaves a video link inside a sentence as an ordinary link', () => {
    const e = load('Watch <https://youtu.be/aX5DXP9DJ_c> now')
    expect(e.state.doc.firstChild!.type.name).toBe('paragraph')
  })

  it('leaves a link to another site as an ordinary link', () => {
    const e = load('<https://example.com/watch?v=aX5DXP9DJ_c>')
    expect(e.state.doc.firstChild!.type.name).toBe('paragraph')
  })

  it('keeps raw html in the stored body as inert text, never as elements', () => {
    const e = load('Hello\n\n<script>alert(1)</script>\n\n<img src=x onerror=alert(1)>')
    expect(e.view.dom.querySelector('script, img')).toBeNull()
    expect(e.getHTML()).not.toMatch(/<(script|img)\b/)
    expect(e.getText()).toContain('<script>alert(1)</script>')
  })

  it('keeps the title-less body empty when nothing was written', () => {
    expect(roundTrip('')).toBe('')
  })
})

describe('inserting blocks from the + menu', () => {
  it('replaces the empty line with the image and leaves a fresh line below it', () => {
    const e = load('Intro')
    e.chain().focus('end').splitBlock().run() // the writer pressed Enter: an empty line under "Intro"
    e.chain().focus().insertContent([{ type: 'image', attrs: { src: 'https://cdn.example/a.jpg', alt: '' } }, { type: 'paragraph' }]).run()

    expect(e.state.doc.children.map(n => n.type.name)).toEqual(['paragraph', 'image', 'paragraph'])
    expect(markdownOf(e)).toBe('Intro\n\n![](https://cdn.example/a.jpg)')
  })

  it('inserts a video embed followed by a fresh line', () => {
    const e = load('')
    e.chain().focus().insertContent([{ type: 'embed', attrs: { url: 'https://www.youtube.com/watch?v=aX5DXP9DJ_c' } }, { type: 'paragraph' }]).run()

    expect(e.state.doc.children.map(n => n.type.name)).toEqual(['embed', 'paragraph'])
  })
})
