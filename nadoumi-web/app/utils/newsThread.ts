import type { CommentNode } from '~/types/news'

/** Like totals of every comment in a nested thread (replies included), keyed by comment id. */
export function commentLikeCounts(nodes: CommentNode[]): Record<number, number> {
  const counts: Record<number, number> = {}
  const walk = (list: CommentNode[]) => {
    for (const c of list) {
      counts[c.id] = c.likeCount
      walk(c.replies)
    }
  }
  walk(nodes)
  return counts
}
