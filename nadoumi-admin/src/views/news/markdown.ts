import MarkdownIt from 'markdown-it'
import type { ArticleComment } from '@/api/news'

// Raw HTML is escaped, so the preview string is safe for v-html.
const md = new MarkdownIt({ html: false, linkify: true, typographer: true })

export const renderPreview = (source: string): string => md.render(source)

/** Result of splicing text into a textarea value: the new value and where the caret belongs. */
export interface Splice {
  value: string
  caret: number
}

/** Inserts `snippet` at the selection (replacing it), on its own paragraph. */
export function insertAtSelection(value: string, start: number, end: number, snippet: string): Splice {
  const before = value.slice(0, start)
  const after = value.slice(end)
  const lead = before === '' || before.endsWith('\n\n') ? '' : before.endsWith('\n') ? '\n' : '\n\n'
  const trail = after === '' || after.startsWith('\n\n') ? '' : after.startsWith('\n') ? '\n' : '\n\n'
  const inserted = `${lead}${snippet}${trail}`
  return { value: before + inserted + after, caret: before.length + inserted.length }
}

export const imageMarkdown = (url: string, alt = ''): string => `![${alt}](${url})`

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
