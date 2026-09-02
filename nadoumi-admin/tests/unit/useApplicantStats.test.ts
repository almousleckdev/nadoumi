import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { ApplicantRow, Page } from '@/api/applicant'
import { listApplicantsPage } from '@/api/dashboard'
import { useApplicantStats } from '@/composables/useApplicantStats'

vi.mock('@/api/dashboard', () => ({ listApplicantsPage: vi.fn() }))

const mockList = vi.mocked(listApplicantsPage)

type Params = { status?: string; createdAfter?: string; size?: number }

function row(over: Partial<ApplicantRow>): ApplicantRow {
  return {
    id: 1, givenName: 'A', familyName: 'B', status: 'ACTIVE',
    dob: '2000-01-01', nationality: 'MR', passportNo: 'P1', email: 'a@b.c',
    phone: '+100', createdAt: '2026-09-01 10:00:00', ...over,
  }
}

function page(content: ApplicantRow[], totalElements: number): Page<ApplicantRow> {
  return { content, page: 0, size: content.length || 1, totalElements, totalPages: 1 }
}

describe('useApplicantStats', () => {
  beforeEach(() => mockList.mockReset())

  it('aggregates counts from the per-segment pages and derives incomplete profiles', async () => {
    const sample = [
      row({ id: 1 }),
      row({ id: 2 }),
      row({ id: 3, phone: null }), // incomplete
      row({ id: 4, nationality: null }), // incomplete
      row({ id: 5 }),
    ]
    mockList.mockImplementation((p: Params = {}) => {
      if (p.createdAfter) return Promise.resolve(page([], 12))
      if (p.status === 'ACTIVE') return Promise.resolve(page([], 40))
      if (p.status === 'DRAFT') return Promise.resolve(page([], 8))
      if (p.size === 200) return Promise.resolve(page(sample, 5))
      return Promise.resolve(page([], 123)) // total
    })

    const { data, loading, error, refresh } = useApplicantStats()
    await refresh()

    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
    expect(data.value).toMatchObject({
      total: 123, new30d: 12, active: 40, draft: 8, incomplete: 2, sampled: false,
    })
    expect(data.value?.recent).toHaveLength(5)
  })

  it('flags a truncated sample when the total exceeds the sample size', async () => {
    mockList.mockImplementation((p: Params = {}) =>
      Promise.resolve(p.size === 200 ? page([row({ id: 1 })], 4200) : page([], 1)),
    )
    const { data, refresh } = useApplicantStats()
    await refresh()
    expect(data.value?.sampled).toBe(true)
  })

  it('requests the new-applicants segment with an ISO createdAfter ~30 days back', async () => {
    mockList.mockResolvedValue(page([], 0))
    const { refresh } = useApplicantStats()
    await refresh()
    const call = mockList.mock.calls.find(([p]) => (p as Params)?.createdAfter)
    const createdAfter = (call?.[0] as Params).createdAfter as string
    expect(createdAfter).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/)
    const days = (Date.now() - new Date(createdAfter).getTime()) / 86_400_000
    expect(days).toBeGreaterThan(29)
    expect(days).toBeLessThan(31)
  })

  it('surfaces a message and clears loading when the endpoint fails', async () => {
    // One segment fails; the others resolve — mirrors a real partial outage and
    // keeps Promise.all's rejection the only one in flight.
    mockList.mockImplementation((p: Params = {}) =>
      p.createdAfter
        ? Promise.reject(new Error('Request failed with status code 500'))
        : Promise.resolve(page([], 0)),
    )
    const { data, loading, error, refresh } = useApplicantStats()
    await refresh()
    expect(loading.value).toBe(false)
    expect(data.value).toBeNull()
    expect(error.value).toBe('Request failed with status code 500')
  })
})
