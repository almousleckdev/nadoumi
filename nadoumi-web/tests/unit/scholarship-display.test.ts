import { describe, it, expect } from 'vitest'
import { deadlineSummary, dualMoney, eligibilityRows, moneyOf } from '~/utils/scholarshipDisplay'

const t = (key: string, params?: Record<string, unknown>) => (params ? `${key}:${JSON.stringify(params)}` : key)

describe('dualMoney', () => {
  it('shows both currencies grouped', () => {
    expect(dualMoney(12000, 1657)).toBe('¥12,000 · $1,657')
  })

  it('treats one missing side as zero and both missing as nothing', () => {
    expect(dualMoney(500, null)).toBe('¥500 · $0')
    expect(dualMoney(undefined, 70)).toBe('¥0 · $70')
    expect(dualMoney(null, undefined)).toBeNull()
  })
})

describe('moneyOf', () => {
  it('formats an amount pair and ignores a missing amount', () => {
    expect(moneyOf({ amountRmb: 800, amountUsd: 110 })).toBe('¥800 · $110')
    expect(moneyOf(null)).toBeNull()
    expect(moneyOf(undefined)).toBeNull()
  })
})

describe('eligibilityRows', () => {
  it('is empty when no criteria are published', () => {
    expect(eligibilityRows(undefined, t)).toEqual([])
    expect(eligibilityRows(null, t)).toEqual([])
    expect(eligibilityRows({}, t)).toEqual([])
  })

  it('lists each criterion that is set, in reading order', () => {
    const rows = eligibilityRows({ ageMin: 18, ageMax: 35, gpaMin: 3, ieltsMin: 6.5, toeflMin: 90, hskMin: 4, inChina: true, acceptedCountries: 'MA, DZ' }, t)
    expect(rows.map(r => r.label)).toEqual([
      'scholarships.elig.age', 'scholarships.elig.gpa', 'IELTS', 'TOEFL', 'HSK', 'scholarships.elig.inChina', 'scholarships.elig.nationality',
    ])
    expect(rows[0]!.value).toBe('18 to 35')
    expect(rows[1]!.value).toBe('≥ 3')
    expect(rows[5]!.value).toBe('common.yes')
    expect(rows[6]!.value).toBe('MA, DZ')
  })

  it('handles an open-ended age range and a negative in-China requirement', () => {
    const rows = eligibilityRows({ ageMax: 30, inChina: false }, t)
    expect(rows[0]!.value).toBe('to 30')
    expect(rows[1]!.value).toBe('common.no')
  })
})

describe('deadlineSummary', () => {
  const inDays = (n: number) => new Date(Date.now() + n * 86_400_000).toISOString().slice(0, 10)

  it('is rolling and calm when there is no deadline', () => {
    expect(deadlineSummary(null, t)).toEqual({ rolling: true, tone: 'ok', date: '', text: 'scholarships.deadlineRolling' })
    expect(deadlineSummary(undefined, t).rolling).toBe(true)
  })

  it('reads as passed for a date in the past', () => {
    const s = deadlineSummary(inDays(-3), t)
    expect(s).toMatchObject({ rolling: false, tone: 'passed', text: 'scholarships.deadlinePassed' })
  })

  it('counts days for an upcoming deadline and grades its urgency', () => {
    const soon = deadlineSummary(inDays(5), t)
    expect(soon.tone).toBe('urgent')
    expect(soon.text).toContain('scholarships.deadlineDays')
    expect(soon.date).toBe(inDays(5))
    expect(deadlineSummary(inDays(30), t).tone).toBe('soon')
    expect(deadlineSummary(inDays(200), t).tone).toBe('ok')
  })
})
