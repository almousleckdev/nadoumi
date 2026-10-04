import { describe, it, expect } from 'vitest'
import type { ArticleComment } from '@/api/news'
import { imageMarkdown, insertAtSelection, renderPreview, threadRows } from '@/views/news/markdown'

function comment(id: number, parentId: number | null): ArticleComment {
  return {
    id, parentId, articleId: 1, authorId: 5, authorName: `user${id}`, body: `body${id}`,
    status: 'VISIBLE', createTime: '2026-09-01T10:00:00', deletedTime: null,
  }
}

describe('insertAtSelection', () => {
  it('puts an image on its own paragraph in the middle of text', () => {
    const r = insertAtSelection('Intro text.Outro', 11, 11, imageMarkdown('https://x/y.jpg', 'pic'))
    expect(r.value).toBe('Intro text.\n\n![pic](https://x/y.jpg)\n\nOutro')
    expect(r.value.slice(0, r.caret).endsWith('![pic](https://x/y.jpg)\n\n')).toBe(true)
  })

  it('adds no extra blank lines into an empty body', () => {
    expect(insertAtSelection('', 0, 0, '![a](u)').value).toBe('![a](u)')
  })

  it('replaces the selected text', () => {
    expect(insertAtSelection('A\n\nOLD\n\nB', 3, 6, '![a](u)').value).toBe('A\n\n![a](u)\n\nB')
  })
})

describe('renderPreview', () => {
  it('renders inline images and escapes raw HTML', () => {
    const html = renderPreview('![pic](https://x/y.jpg)\n\n<script>alert(1)</script>')
    expect(html).toContain('<img src="https://x/y.jpg" alt="pic"')
    expect(html).not.toContain('<script>')
  })
})

describe('threadRows', () => {
  it('orders replies directly after their parent with increasing depth', () => {
    const rows = threadRows([comment(1, null), comment(2, null), comment(3, 1), comment(4, 3)])
    expect(rows.map(r => [r.comment.id, r.depth])).toEqual([[1, 0], [3, 1], [4, 2], [2, 0]])
  })
})
