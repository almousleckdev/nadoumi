import { describe, it, expect } from 'vitest'
import { groupMajors, programTuition } from '~/utils/programDisplay'
import type { ProgramMajor } from '~/types/catalog'

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
