import { describe, it, expect } from 'vitest'
import type { ArticleComment } from '@/api/news'
import { threadRows } from '@/views/news/commentThread'

function comment(id: number, parentId: number | null): ArticleComment {
  return {
    id, parentId, articleId: 1, authorId: 5, authorName: `user${id}`, body: `body${id}`,
    status: 'VISIBLE', createTime: '2026-09-01T10:00:00', deletedTime: null,
  }
}

describe('threadRows', () => {
  it('orders replies directly after their parent with increasing depth', () => {
    const rows = threadRows([comment(1, null), comment(2, null), comment(3, 1), comment(4, 3)])
    expect(rows.map(r => [r.comment.id, r.depth])).toEqual([[1, 0], [3, 1], [4, 2], [2, 0]])
  })
})
