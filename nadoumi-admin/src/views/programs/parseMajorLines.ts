export interface MajorLine {
  name: string
  nameCn: string | null
}

const LIST_MARKER = /^\s*(?:[-*•·]|\d+[.)])\s+/
const CELL_SEPARATOR = /\t|\||｜/
// "Computer Science 计算机科学": a Latin name directly followed by a Chinese one
const LATIN_THEN_CJK = /^(.*?[A-Za-z0-9.)])\s*([㐀-鿿].*)$/

/** Pasted majors, one per line; a Chinese name may follow after a tab, a pipe, or straight after the Latin name. */
export function parseMajorLines(text: string): MajorLine[] {
  const seen = new Set<string>()
  const lines: MajorLine[] = []
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.replace(LIST_MARKER, '').trim()
    if (!line) continue
    const cells = line.split(CELL_SEPARATOR).map(c => c.trim())
    const split = cells.length === 1 ? LATIN_THEN_CJK.exec(line) : null
    const name = cells.length > 1 ? cells[0]! : split ? split[1]!.trim() : line
    const nameCn = cells.length > 1 ? (cells[1] || null) : split ? split[2]!.trim() : null
    if (!name || seen.has(name.toLowerCase())) continue
    seen.add(name.toLowerCase())
    lines.push({ name, nameCn })
  }
  return lines
}
