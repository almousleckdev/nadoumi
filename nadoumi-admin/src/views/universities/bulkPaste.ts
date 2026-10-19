import type { DeptRow } from './universityForm'

export type RankingDraft = {
  source: string
  rankPosition: number | null
  rankYear: number | null
  note: string | null
}

export type ParseResult<T> = { rows: T[], skipped: number }

const MAX_SOURCE_LENGTH = 40
const MAX_NOTE_LENGTH = 200
const MIN_RANK_YEAR = 1900
const CELL_SEPARATOR = /\t|\||｜/
const LIST_MARKER = /^\s*(?:[-*•·]|\d+[.)])\s+/
// "School of Medicine 医学院": a Latin name directly followed by a Chinese one
const LATIN_THEN_CJK = /^(.*?[A-Za-z0-9.)])\s*([㐀-鿿].*)$/
// "QS World 245 2025 note": source, rank, optional year, optional note
const SPACED_RANKING = /^(\S.*?)\s+#?(\d+)(?:\s+(\d{4}))?(?:\s+(.+))?$/

function lines(text: string): string[] {
  return text.split(/\r?\n/).map(l => l.replace(LIST_MARKER, '').trim()).filter(Boolean)
}

export function parseDepartments(text: string, existing: DeptRow[] = []): ParseResult<DeptRow> {
  const seen = new Set(existing.map(d => d.name.trim().toLowerCase()))
  const rows: DeptRow[] = []
  let skipped = 0
  for (const line of lines(text)) {
    let [name, nameCn = ''] = line.split(CELL_SEPARATOR).map(c => c.trim())
    if (!nameCn) {
      const m = LATIN_THEN_CJK.exec(name)
      if (m && m[1].trim()) [name, nameCn] = [m[1].trim(), m[2].trim()]
    }
    const key = name.toLowerCase()
    if (!name || seen.has(key)) {
      skipped++
      continue
    }
    seen.add(key)
    rows.push({ id: undefined, name, nameCn })
  }
  return { rows, skipped }
}

function toRanking(source: string, rank: string, year: string, note: string, maxYear: number): RankingDraft | null {
  const position = Number(rank.replace(/^#/, ''))
  if (!source || source.length > MAX_SOURCE_LENGTH || !Number.isInteger(position) || position < 1) return null
  const parsedYear = year ? Number(year) : null
  const yearOk = parsedYear === null || (Number.isInteger(parsedYear) && parsedYear >= MIN_RANK_YEAR && parsedYear <= maxYear)
  if (!yearOk || note.length > MAX_NOTE_LENGTH) return null
  return { source, rankPosition: position, rankYear: parsedYear, note: note || null }
}

export function parseRankings(text: string, defaultYear: number): ParseResult<RankingDraft> {
  const rows: RankingDraft[] = []
  let skipped = 0
  for (const line of lines(text)) {
    const cells = line.includes('\t') || line.includes('|') || line.includes('｜')
      ? line.split(CELL_SEPARATOR)
      : line.includes(',') ? line.split(',') : null
    let draft: RankingDraft | null
    if (cells) {
      const [source = '', rank = '', year = '', ...note] = cells.map(c => c.trim())
      draft = toRanking(source, rank, year, note.join(', '), defaultYear + 1)
    }
    else {
      const m = SPACED_RANKING.exec(line)
      draft = m ? toRanking(m[1].trim(), m[2], m[3] ?? '', (m[4] ?? '').trim(), defaultYear + 1) : null
    }
    if (!draft) {
      skipped++
      continue
    }
    rows.push({ ...draft, rankYear: draft.rankYear ?? defaultYear })
  }
  return { rows, skipped }
}
