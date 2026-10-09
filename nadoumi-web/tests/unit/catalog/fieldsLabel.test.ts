import { describe, it, expect } from 'vitest'
import { fieldsLabel } from '~/utils/scholarshipDisplay'

const t = (key: string) => ({
  'scholarships.allLevels': 'All levels',
  'scholarships.level.BACHELOR': "Bachelor's",
  'scholarships.level.MASTER': "Master's",
  'scholarships.level.PHD': 'PhD',
}[key] ?? key)

describe('fieldsLabel', () => {
  it('is empty when no field is listed', () => {
    expect(fieldsLabel([], t)).toBe('')
    expect(fieldsLabel(undefined, t)).toBe('')
  })

  it('lists the fields plainly when every level shares them', () => {
    expect(fieldsLabel([{ level: null, name: 'Engineering' }, { level: null, name: 'Medicine' }], t)).toBe('Engineering, Medicine')
  })

  it('groups by level, in degree order, when the levels differ', () => {
    expect(fieldsLabel([
      { level: 'PHD', name: 'Medicine' },
      { level: null, name: 'Engineering' },
      { level: 'BACHELOR', name: 'Architecture' },
      { level: 'BACHELOR', name: 'Law' },
    ], t)).toBe("All levels: Engineering · Bachelor's: Architecture, Law · PhD: Medicine")
  })

  it('omits the all-levels part when only level-specific fields exist', () => {
    expect(fieldsLabel([{ level: 'MASTER', name: 'Medicine' }], t)).toBe("Master's: Medicine")
  })
})
