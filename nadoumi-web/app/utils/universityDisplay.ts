import type { DegreeLevel, ProgramCard } from '~/types/catalog'

const LEVEL_ORDER: DegreeLevel[] = ['DIPLOMA', 'BACHELOR', 'MASTER', 'PHD']
const PERCENT = 100

/** International students as a whole percentage of all students, or empty when it cannot be computed. */
export function internationalShare(total: number | null | undefined, international: number | null | undefined): string {
  if (!total || international == null) return ''
  return `${Math.min(PERCENT, Math.round((international / total) * PERCENT))}%`
}

/** Every degree level at least one of the programmes is offered at, DIPLOMA to PHD. */
export function levelsOffered(programs: ProgramCard[]): DegreeLevel[] {
  const present = new Set(programs.flatMap(p => p.levels ?? []))
  return LEVEL_ORDER.filter(l => present.has(l))
}

/** The programme with the lowest tuition; programmes without a tuition are ignored. */
export function cheapestProgram(programs: ProgramCard[]): ProgramCard | null {
  return programs
    .filter(p => p.tuitionAmount != null)
    .reduce<ProgramCard | null>((best, p) => (best == null || (p.tuitionAmount as number) < (best.tuitionAmount as number) ? p : best), null)
}

export function mapUrl(address: string): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`
}

/** The value as a link target when it is an http(s) URL; anything else (an account id, a script URL) is not linkable. */
export function linkHref(value: string): string | null {
  return /^https?:\/\/\S+$/i.test(value.trim()) ? value.trim() : null
}
