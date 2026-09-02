import { describe, it, expect, vi, beforeEach } from 'vitest'

const api = vi.hoisted(() => ({
  getApplicant: vi.fn(),
  listEducation: vi.fn(),
  listTestScores: vi.fn(),
  listContacts: vi.fn(),
}))
vi.mock('@/api/applicant', () => api)

import { useApplicant } from '@/composables/useApplicant'

describe('useApplicant', () => {
  beforeEach(() => {
    Object.values(api).forEach(fn => fn.mockReset())
  })

  it('loads the applicant profile and clears loading', async () => {
    api.getApplicant.mockResolvedValue({ id: 7, givenName: 'A', familyName: 'B', status: 'ACTIVE' })
    const { applicant, loading, error, load } = useApplicant(7)
    await load()
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
    expect(applicant.value?.id).toBe(7)
    expect(api.getApplicant).toHaveBeenCalledWith(7)
  })

  it('surfaces a load error without throwing', async () => {
    api.getApplicant.mockRejectedValue(new Error('nope'))
    const { error, applicant, load } = useApplicant(1)
    await load()
    expect(error.value).toBe('nope')
    expect(applicant.value).toBeNull()
  })

  it('lazy-loads each sub-resource exactly once unless forced', async () => {
    api.listEducation.mockResolvedValue([{ id: 1, institution: 'MIT' }])
    const { education, loadEducation } = useApplicant(1)

    await loadEducation()
    await loadEducation()
    expect(api.listEducation).toHaveBeenCalledTimes(1)
    expect(education.value).toHaveLength(1)

    await loadEducation(true)
    expect(api.listEducation).toHaveBeenCalledTimes(2)
  })
})
