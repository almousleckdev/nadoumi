import { daysUntilDeadline, deadlineTone, type DeadlineTone } from '~/utils/deadline'

type Translate = (key: string, params?: Record<string, unknown>) => string

export interface EligibilityCriteria {
  ageMin?: number | null
  ageMax?: number | null
  gpaMin?: number | null
  ieltsMin?: number | null
  toeflMin?: number | null
  hskMin?: number | null
  inChina?: boolean | null
  acceptedCountries?: string | null
}

export interface DeadlineSummary {
  rolling: boolean
  tone: DeadlineTone
  date: string
  text: string
}

export function dualMoney(rmb?: number | null, usd?: number | null): string | null {
  if (rmb == null && usd == null) return null
  return `¥${(rmb ?? 0).toLocaleString('en')} · $${(usd ?? 0).toLocaleString('en')}`
}

export function moneyOf(amount?: { amountRmb: number, amountUsd: number } | null): string | null {
  return amount ? dualMoney(amount.amountRmb, amount.amountUsd) : null
}

export function eligibilityRows(e: EligibilityCriteria | null | undefined, t: Translate): { label: string, value: string }[] {
  if (!e) return []
  const rows: { label: string, value: string }[] = []
  if (e.ageMin != null || e.ageMax != null) rows.push({ label: t('scholarships.elig.age'), value: `${e.ageMin ?? ''} to ${e.ageMax ?? ''}`.trim() })
  if (e.gpaMin != null) rows.push({ label: t('scholarships.elig.gpa'), value: `≥ ${e.gpaMin}` })
  if (e.ieltsMin != null) rows.push({ label: 'IELTS', value: `≥ ${e.ieltsMin}` })
  if (e.toeflMin != null) rows.push({ label: 'TOEFL', value: `≥ ${e.toeflMin}` })
  if (e.hskMin != null) rows.push({ label: 'HSK', value: `≥ ${e.hskMin}` })
  if (e.inChina != null) rows.push({ label: t('scholarships.elig.inChina'), value: e.inChina ? t('common.yes') : t('common.no') })
  if (e.acceptedCountries) rows.push({ label: t('scholarships.elig.nationality'), value: e.acceptedCountries })
  return rows
}

export function deadlineSummary(raw: string | null | undefined, t: Translate): DeadlineSummary {
  const days = daysUntilDeadline(raw)
  if (!raw || days == null) return { rolling: true, tone: 'ok', date: '', text: t('scholarships.deadlineRolling') }
  const text = days < 0
    ? t('scholarships.deadlinePassed')
    : days === 0 ? t('scholarships.deadlineToday') : t('scholarships.deadlineDays', { n: days })
  return { rolling: false, tone: deadlineTone(days), date: raw, text }
}
