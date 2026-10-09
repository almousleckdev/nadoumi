import type { ArticleComment } from '@/api/news'

export interface ThreadRow {
  comment: ArticleComment
  depth: number
}

/** Flattens the flat comment list into display order (parent, then its replies), with nesting depth. */
export function threadRows(flat: ArticleComment[]): ThreadRow[] {
  const byParent = new Map<number | null, ArticleComment[]>()
  for (const c of flat) {
    const list = byParent.get(c.parentId) ?? []
    list.push(c)
    byParent.set(c.parentId, list)
  }
  const rows: ThreadRow[] = []
  const walk = (parentId: number | null, depth: number) => {
    for (const c of byParent.get(parentId) ?? []) {
      rows.push({ comment: c, depth })
      walk(c.id, depth + 1)
    }
  }
  walk(null, 0)
  return rows
}
