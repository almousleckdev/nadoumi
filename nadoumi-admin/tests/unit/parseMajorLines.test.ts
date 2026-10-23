import { describe, expect, it } from 'vitest'
import { parseMajorLines } from '@/views/programs/parseMajorLines'

describe('parseMajorLines', () => {
  it('reads one major per line with an optional Chinese name', () => {
    expect(parseMajorLines('Computer Science\n1. Civil Engineering | 土木工程\n- Clinical Medicine 临床医学')).toEqual([
      { name: 'Computer Science', nameCn: null },
      { name: 'Civil Engineering', nameCn: '土木工程' },
      { name: 'Clinical Medicine', nameCn: '临床医学' },
    ])
  })

  it('skips blank lines and repeats regardless of case', () => {
    expect(parseMajorLines('Law\n\n law \nLAW')).toEqual([{ name: 'Law', nameCn: null }])
  })
})
