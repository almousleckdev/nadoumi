import type { ProgramDetail, ProgramMajor } from '~/types/catalog'

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
