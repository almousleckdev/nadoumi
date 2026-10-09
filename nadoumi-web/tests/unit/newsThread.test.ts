import { describe, it, expect } from 'vitest'
import { commentLikeCounts } from '~/utils/newsThread'
import type { CommentNode } from '~/types/news'

function node(id: number, likeCount: number, replies: CommentNode[] = []): CommentNode {
  return { id, parentId: null, authorName: 'A', body: 'b', deleted: false, likeCount, createTime: '2026-10-01T10:00:00', replies }
}

describe('commentLikeCounts', () => {
  it('collects the like total of top-level comments and of every nested reply', () => {
    const thread = [node(1, 3, [node(2, 0, [node(3, 7)])]), node(4, 1)]

    expect(commentLikeCounts(thread)).toEqual({ 1: 3, 2: 0, 3: 7, 4: 1 })
  })

  it('is empty for a thread with no comments', () => {
    expect(commentLikeCounts([])).toEqual({})
  })
})
