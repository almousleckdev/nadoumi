import type { DegreeLevel, ProgramDetail, ProgramMajor, ProgramOffering } from '~/types/catalog'

type Translate = (key: string, params?: Record<string, unknown>, plural?: number) => string

const LEVEL_ORDER: DegreeLevel[] = ['DIPLOMA', 'BACHELOR', 'MASTER', 'PHD']

export interface MajorGroup {
  dept: string
  majors: ProgramMajor[]
}

export function programTuition(p: Pick<ProgramDetail, 'tuitionAmount' | 'tuitionCurrency' | 'tuitionAmountUsd'>) {
  return {
    cny: p.tuitionAmount != null ? `${p.tuitionCurrency ?? '¥'} ${p.tuitionAmount.toLocaleString('en')}`.trim() : '',
    usd: p.tuitionAmountUsd != null ? `$${p.tuitionAmountUsd.toLocaleString('en')}` : '',
  }
}

export function groupMajors(majors: ProgramMajor[]): MajorGroup[] {
  const byDept = new Map<string, ProgramMajor[]>()
  for (const m of majors) {
    const key = m.departmentName || ''
    if (!byDept.has(key)) byDept.set(key, [])
    byDept.get(key)!.push(m)
  }
  return [...byDept.entries()].map(([dept, list]) => ({ dept, majors: list }))
}

export interface OfferingGroup {
  /** Null for the level-less offerings of language / non-degree programmes. */
  level: DegreeLevel | null
  offerings: ProgramOffering[]
}

/** Offerings grouped by level, level-less first, then DIPLOMA to PHD; each group shortest duration first. */
export function groupOfferings(offerings: ProgramOffering[]): OfferingGroup[] {
  const rank = (l: DegreeLevel | null) => (l == null ? -1 : LEVEL_ORDER.indexOf(l))
  const byLevel = new Map<DegreeLevel | null, ProgramOffering[]>()
  for (const o of offerings) {
    const list = byLevel.get(o.level) ?? []
    list.push(o)
    byLevel.set(o.level, list)
  }
  return [...byLevel.entries()]
    .sort(([a], [b]) => rank(a) - rank(b))
    .map(([level, list]) => ({ level, offerings: [...list].sort((a, b) => a.durationMonths - b.durationMonths) }))
}

export function durationRange(offerings: ProgramOffering[]): { min: number, max: number } | null {
  if (!offerings.length) return null
  const months = offerings.map(o => o.durationMonths)
  return { min: Math.min(...months), max: Math.max(...months) }
}

/** The offering with the lowest positive amount in `field`, or null when none has one. */
export function cheapest(
  offerings: ProgramOffering[],
  field: 'tuitionAmount' | 'accommodationAmount',
): ProgramOffering | null {
  return offerings
    .filter(o => o[field] != null)
    .reduce<ProgramOffering | null>((best, o) => (best == null || (o[field] as number) < (best[field] as number) ? o : best), null)
}

export function formatDuration(months: number, t: Translate): string {
  return months % 12 === 0
    ? t('program.durationYears', { n: months / 12 }, months / 12)
    : t('program.duration', { months })
}

/** "¥26,000 · $3,600" for CNY; "USD 5,000" for any other currency; empty without an amount. */
export function offeringMoney(amount: number | null | undefined, usd: number | null | undefined, currency: string | null | undefined): string {
  if (amount == null) return ''
  const isCny = !currency || currency.toUpperCase() === 'CNY' || currency.toUpperCase() === 'RMB'
  if (!isCny) return `${currency!.toUpperCase()} ${amount.toLocaleString('en')}`
  const rmb = `¥${amount.toLocaleString('en')}`
  return usd != null ? `${rmb} · $${usd.toLocaleString('en')}` : rmb
}
