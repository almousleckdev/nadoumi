import { describe, expect, it } from 'vitest'
import type { ApplyTarget } from '~/types/apply'
import type { StudentApplicationDto } from '~/types/catalog'
import { applyRoute, findDraft, parseApplyQuery, startBody } from '~/utils/applyView'

const programme: ApplyTarget = {
  kind: 'programme', id: 5, slug: 'mbbs-fudan', title: 'MBBS', universityName: 'Fudan University', intakes: [{ id: 9, term: 'AUTUMN_SEPTEMBER' }],
}
const scholarship: ApplyTarget = { kind: 'scholarship', id: 7, slug: 'csc-master', title: 'CSC Master' }

const app = (over: Partial<StudentApplicationDto>): StudentApplicationDto => ({
  id: 1, reference: 'NAD20261023001', applicantId: 3, applicationType: 'PROGRAM_ONLY', programId: 5, scholarshipId: null, intakeId: 9,
  currentStageName: 'Draft', currentStatus: 'DRAFT', submittedAt: null, timeline: [],
  title: 'MBBS', universityName: 'Fudan University', intake: 'Autumn', applicantName: 'Amina Diallo', ...over,
})

describe('parseApplyQuery', () => {
  it('reads a programme or a scholarship slug', () => {
    expect(parseApplyQuery({ programme: 'mbbs-fudan' })).toEqual({ kind: 'programme', slug: 'mbbs-fudan' })
    expect(parseApplyQuery({ scholarship: 'csc-master' })).toEqual({ kind: 'scholarship', slug: 'csc-master' })
  })

  it('refuses both together, neither, and anything that is not a plain slug', () => {
    expect(parseApplyQuery({ programme: 'a', scholarship: 'b' })).toBeNull()
    expect(parseApplyQuery({})).toBeNull()
    expect(parseApplyQuery({ programme: '../etc/passwd' })).toBeNull()
    expect(parseApplyQuery({ scholarship: 'x y' })).toBeNull()
  })
})

describe('startBody', () => {
  it('builds a programme application with its intake and no scholarship', () => {
    expect(startBody(programme, 3, 9)).toEqual({ applicantId: 3, applicationType: 'PROGRAM_ONLY', programId: 5, intakeId: 9 })
  })

  it('builds a scholarship application with no programme and no intake', () => {
    expect(startBody(scholarship, 3, null)).toEqual({ applicantId: 3, applicationType: 'SCHOLARSHIP_ONLY', scholarshipId: 7 })
  })
})

describe('findDraft', () => {
  it('finds this student\'s unsubmitted application for the same opportunity', () => {
    expect(findDraft([app({})], programme, 3)?.id).toBe(1)
    expect(findDraft([app({ programId: null, scholarshipId: 7 })], scholarship, 3)?.id).toBe(1)
  })

  it('ignores another student, another opportunity and submitted applications', () => {
    expect(findDraft([app({ applicantId: 4 })], programme, 3)).toBeUndefined()
    expect(findDraft([app({ programId: 6 })], programme, 3)).toBeUndefined()
    expect(findDraft([app({ currentStatus: 'IN_REVIEW' })], programme, 3)).toBeUndefined()
    expect(findDraft([app({ programId: null, scholarshipId: 8 })], scholarship, 3)).toBeUndefined()
  })
})

describe('applyRoute', () => {
  it('addresses the apply page by kind and slug', () => {
    expect(applyRoute('scholarship', 'csc-master')).toEqual({ path: '/apply', query: { scholarship: 'csc-master' } })
  })
})
