import { describe, it, expect } from 'vitest'
import {
  cheapest, durationRange, formatDuration, groupMajors, groupOfferings, offeringMoney, programTuition,
} from '~/utils/programDisplay'
import type { ProgramMajor, ProgramOffering } from '~/types/catalog'

describe('programTuition', () => {
  it('formats CNY with the stored currency and USD with a dollar sign', () => {
    expect(programTuition({ tuitionAmount: 32000, tuitionCurrency: 'CNY', tuitionAmountUsd: 4400 }))
      .toEqual({ cny: 'CNY 32,000', usd: '$4,400' })
  })

  it('falls back to the yen sign and omits missing amounts', () => {
    expect(programTuition({ tuitionAmount: 1000, tuitionCurrency: null, tuitionAmountUsd: null }))
      .toEqual({ cny: '¥ 1,000', usd: '' })
    expect(programTuition({ tuitionAmount: null, tuitionCurrency: null, tuitionAmountUsd: null }))
      .toEqual({ cny: '', usd: '' })
  })
})

describe('groupMajors', () => {
  const m = (id: number, name: string, departmentName: string | null) => ({ id, name, departmentName }) as ProgramMajor

  it('groups majors by department in first-seen order', () => {
    const groups = groupMajors([m(1, 'A', 'Eng'), m(2, 'B', 'Arts'), m(3, 'C', 'Eng')])
    expect(groups.map(g => g.dept)).toEqual(['Eng', 'Arts'])
    expect(groups[0]!.majors.map(x => x.id)).toEqual([1, 3])
  })

  it('puts majors without a department into an unnamed group', () => {
    expect(groupMajors([m(1, 'A', null)])).toEqual([{ dept: '', majors: [expect.objectContaining({ id: 1 })] }])
  })

  it('returns no groups for no majors', () => {
    expect(groupMajors([])).toEqual([])
  })
})

describe('offerings', () => {
  const off = (level: string | null, durationMonths: number, tuitionAmount: number | null = null, accommodationAmount: number | null = null) => ({
    id: durationMonths, level, durationMonths, tuitionAmount, tuitionCurrency: 'CNY', tuitionPeriod: 'PER_YEAR',
    tuitionAmountUsd: null, accommodationAmount, accommodationCurrency: 'CNY', accommodationPeriod: 'PER_MONTH',
    accommodationAmountUsd: null, note: null,
  }) as ProgramOffering

  it('groups offerings by level in DIPLOMA to PHD order and keeps level-less ones first', () => {
    const groups = groupOfferings([off('PHD', 36), off(null, 12), off('BACHELOR', 48), off('BACHELOR', 60)])
    expect(groups.map(g => [g.level, g.offerings.map(o => o.durationMonths)])).toEqual([
      [null, [12]], ['BACHELOR', [48, 60]], ['PHD', [36]],
    ])
  })

  it('summarises the duration range', () => {
    expect(durationRange([off('MASTER', 24), off('PHD', 48), off('BACHELOR', 48)])).toEqual({ min: 24, max: 48 })
    expect(durationRange([])).toBeNull()
  })

  it('finds the cheapest tuition and accommodation across offerings', () => {
    const o = [off('MASTER', 24, 30000, 1500), off('PHD', 36, 26000, null), off('BACHELOR', 48, null, 900)]
    expect(cheapest(o, 'tuitionAmount')?.tuitionAmount).toBe(26000)
    expect(cheapest(o, 'accommodationAmount')?.accommodationAmount).toBe(900)
    expect(cheapest([off('MASTER', 24)], 'tuitionAmount')).toBeNull()
  })

  it('formats a duration in years when it is a whole number of years', () => {
    const t = (key: string, p?: Record<string, unknown>) => `${key}:${JSON.stringify(p ?? {})}`
    expect(formatDuration(48, t)).toBe('program.durationYears:{"n":4}')
    expect(formatDuration(6, t)).toBe('program.duration:{"months":6}')
    expect(formatDuration(18, t)).toBe('program.duration:{"months":18}')
  })
})

describe('offeringMoney', () => {
  it('shows RMB and USD together, and nothing when there is no amount', () => {
    expect(offeringMoney(26000, 3600, 'CNY')).toBe('¥26,000 · $3,600')
    expect(offeringMoney(26000, null, 'CNY')).toBe('¥26,000')
    expect(offeringMoney(null, null, null)).toBe('')
  })

  it('keeps a non-CNY currency code', () => {
    expect(offeringMoney(5000, null, 'USD')).toBe('USD 5,000')
  })
})
