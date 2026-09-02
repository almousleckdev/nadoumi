import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useApplicant } from '~/composables/useApplicant'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
  problemMessage: (_e: unknown, f: string) => f,
}))

beforeEach(() => studentFetch.mockReset())

describe('useApplicant', () => {
  it('GET applicants', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useApplicant().listMine()
    expect(studentFetch).toHaveBeenCalledWith('applicants', undefined)
    expect(rows).toEqual([{ id: 1 }])
  })

  it('POST applicants strips empty strings from the body', async () => {
    studentFetch.mockResolvedValueOnce({ id: 2 })
    await useApplicant().create({ givenName: 'A', familyName: 'B', dob: '', nationality: '', passportNo: '', email: '', phone: '' })
    expect(studentFetch).toHaveBeenCalledWith('applicants', { method: 'POST', body: { givenName: 'A', familyName: 'B' } })
  })

  it('PUT applicants/{id}/education/{eduId}', async () => {
    studentFetch.mockResolvedValueOnce({ id: 9 })
    await useApplicant().updateEducation(3, 9, { institution: 'X' })
    expect(studentFetch).toHaveBeenCalledWith('applicants/3/education/9', { method: 'PUT', body: { institution: 'X' } })
  })

  it('DELETE applicants/{id}/contacts/{cid}', async () => {
    studentFetch.mockResolvedValueOnce(undefined)
    await useApplicant().deleteContact(3, 4)
    expect(studentFetch).toHaveBeenCalledWith('applicants/3/contacts/4', { method: 'DELETE' })
  })
})
