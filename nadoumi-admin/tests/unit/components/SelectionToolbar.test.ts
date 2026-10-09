import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { Editor } from '@tiptap/core'
import SelectionToolbar from '@/views/news/editor/SelectionToolbar.vue'
import { createNewsExtensions } from '@/views/news/editor/extensions'
import { mountOpts } from '../../helpers'

// Layout does not exist in happy-dom; the toolbar only needs *a* position to place itself.
const BOX = { left: 100, right: 120, top: 50, bottom: 70 }
const FIRST = 'First paragraph of text'
const SECOND = 'Second paragraph here'

let editor: Editor
let host: HTMLElement
let wrapper: VueWrapper

/** Start offset of a paragraph's text inside the ProseMirror document. */
function rangeOf(paragraph: 0 | 1, from = 0, to?: number) {
  const first = editor.state.doc.child(0).nodeSize
  const start = paragraph === 0 ? 1 : first + 1
  const length = (paragraph === 0 ? FIRST : SECOND).length
  return { from: start + from, to: start + (to ?? length) }
}

async function select(paragraph: 0 | 1) {
  editor.view.dom.focus()
  editor.commands.setTextSelection(rangeOf(paragraph))
  await flushPromises()
}

const toolbar = () => wrapper.find('[role="toolbar"]')
const buttons = () => toolbar().findAll('.tb__btn')
const linkInput = () => toolbar().find('.tb__input')
const isShown = () => toolbar().exists() && (toolbar().element as HTMLElement).style.display !== 'none'
const linkButton = () => buttons().find(b => b.attributes('aria-label') === 'Link')!

beforeEach(async () => {
  host = document.createElement('div')
  host.style.position = 'relative'
  document.body.appendChild(host)
  editor = new Editor({
    element: host,
    extensions: createNewsExtensions({ placeholder: '' }),
    content: `${FIRST}\n\n${SECOND}`,
  })
  vi.spyOn(editor.view, 'coordsAtPos').mockReturnValue({ ...BOX })
  vi.spyOn(Range.prototype, 'getBoundingClientRect').mockReturnValue({ ...BOX, x: 0, y: 0, width: 20, height: 20, toJSON: () => ({}) })
  wrapper = mount(SelectionToolbar, { props: { editor }, attachTo: host, ...mountOpts() })
  await flushPromises()
})

afterEach(() => {
  wrapper.unmount()
  editor.destroy()
  host.remove()
  vi.restoreAllMocks()
})

describe('SelectionToolbar', () => {
  it('offers the formatting buttons, not the link field, when text is selected', async () => {
    await select(0)

    expect(isShown()).toBe(true)
    expect(buttons().length).toBeGreaterThanOrEqual(7)
    expect(linkInput().exists()).toBe(false)
  })

  it('is hidden while nothing is selected', async () => {
    editor.view.dom.focus()
    editor.commands.setTextSelection(3)
    await flushPromises()

    expect(isShown()).toBe(false)
  })

  it('opens the link field only when the Link button is pressed', async () => {
    await select(0)

    await linkButton().trigger('click')

    expect(linkInput().exists()).toBe(true)
    expect(buttons()).toHaveLength(0)
  })

  it('returns to the formatting buttons when the writer selects other text instead of finishing the link', async () => {
    await select(0)
    await linkButton().trigger('click')
    expect(linkInput().exists()).toBe(true)

    await select(1) // the writer clicks back into the article and selects another passage

    expect(isShown()).toBe(true)
    expect(linkInput().exists()).toBe(false)
    expect(buttons().length).toBeGreaterThanOrEqual(7)
  })

  it('does not reopen the link field on a later selection after the selection was cleared', async () => {
    await select(0)
    await linkButton().trigger('click')

    editor.commands.setTextSelection(rangeOf(0).from) // caret only: the selection is gone
    await flushPromises()
    expect(isShown()).toBe(false)

    await select(1)
    expect(linkInput().exists()).toBe(false)
    expect(buttons().length).toBeGreaterThanOrEqual(7)
  })

  it('closes the link field on Escape and keeps the selection', async () => {
    await select(0)
    await linkButton().trigger('click')

    await linkInput().trigger('keydown', { key: 'Escape' })

    expect(linkInput().exists()).toBe(false)
    expect(buttons().length).toBeGreaterThanOrEqual(7)
    expect(editor.state.selection.empty).toBe(false)
  })

  it('applies a valid link to the selection and goes back to formatting', async () => {
    await select(0)
    await linkButton().trigger('click')
    await linkInput().setValue('example.com/guide')

    await toolbar().find('form').trigger('submit')

    expect(editor.getHTML()).toContain('href="https://example.com/guide"')
    expect(linkInput().exists()).toBe(false)
  })

  it('refuses a javascript: link and tells the writer', async () => {
    await select(0)
    await linkButton().trigger('click')
    await linkInput().setValue('javascript:alert(1)')

    await toolbar().find('form').trigger('submit')

    expect(editor.getHTML()).not.toContain('javascript:')
    expect(toolbar().find('[role="alert"]').exists()).toBe(true)
    expect(linkInput().exists()).toBe(true)
  })

  it('applies bold to the selection', async () => {
    await select(0)

    await buttons().find(b => b.attributes('aria-label') === 'Bold')!.trigger('click')

    expect(editor.getHTML()).toContain('<strong>First paragraph of text</strong>')
  })
})
