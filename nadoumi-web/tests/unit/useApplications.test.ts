import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useApplications } from '~/composables/useApplications'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

beforeEach(() => studentFetch.mockReset())

describe('useApplications', () => {
  it('lists the caller\'s applications from the student API', async () => {
    studentFetch.mockResolvedValueOnce([{ id: 1 }])
    const rows = await useApplications().list()
    expect(studentFetch).toHaveBeenCalledWith('applications')
    expect(rows).toEqual([{ id: 1 }])
  })

  it('reads one application', async () => {
    studentFetch.mockResolvedValueOnce({ id: 4 })
    await useApplications().get(4)
    expect(studentFetch).toHaveBeenCalledWith('applications/4')
  })

  it('submits a draft with POST', async () => {
    studentFetch.mockResolvedValueOnce({ id: 4 })
    await useApplications().submit(4)
    expect(studentFetch).toHaveBeenCalledWith('applications/4/submit', { method: 'POST' })
  })

  it('withdraws through the withdraw transition, sending the reason', async () => {
    studentFetch.mockResolvedValueOnce({ id: 4 })
    await useApplications().withdraw(4, 'Changed plans')
    expect(studentFetch).toHaveBeenCalledWith('applications/4/transitions/withdraw', {
      method: 'POST', body: { reason: 'Changed plans' },
    })
  })
})
