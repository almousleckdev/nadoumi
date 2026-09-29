import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useScholarshipFinder } from '~/composables/useScholarshipFinder'

const publicGet = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ publicGet, studentFetch: vi.fn() }),
}))

beforeEach(() => {
  publicGet.mockReset()
})

describe('useScholarshipFinder', () => {
  it('searches with a page size and only the filters that are set', async () => {
    publicGet.mockResolvedValue({ content: [{ id: 1 }] })
    const finder = useScholarshipFinder()
    finder.query.value = '  medicine '
    finder.funding.value = 'FULLY'
    await finder.search()
    expect(publicGet).toHaveBeenCalledWith('scholarships', { size: 5, q: 'medicine', funding: 'FULLY' })
    expect(finder.results.value).toEqual([{ id: 1 }])
    expect(finder.searched.value).toBe(true)
    expect(finder.failed.value).toBe(false)
  })

  it('omits blank filters from the request', async () => {
    publicGet.mockResolvedValue({ content: [] })
    const finder = useScholarshipFinder()
    finder.query.value = '   '
    await finder.search()
    expect(publicGet).toHaveBeenCalledWith('scholarships', { size: 5 })
  })

  it('reports a failure and clears the results', async () => {
    publicGet.mockResolvedValueOnce({ content: [{ id: 1 }] }).mockRejectedValueOnce(new Error('boom'))
    const finder = useScholarshipFinder()
    await finder.search()
    await finder.search()
    expect(finder.failed.value).toBe(true)
    expect(finder.results.value).toEqual([])
    expect(finder.finding.value).toBe(false)
  })

  it('ignores a second search while one is running', async () => {
    let resolve!: (v: unknown) => void
    publicGet.mockReturnValue(new Promise((r) => { resolve = r }))
    const finder = useScholarshipFinder()
    const first = finder.search()
    await finder.search()
    expect(publicGet).toHaveBeenCalledOnce()
    resolve({ content: [] })
    await first
  })

  it('builds the query string for the full results page', () => {
    const finder = useScholarshipFinder()
    expect(finder.resultsQuery.value).toBe('')
    finder.query.value = ' law '
    finder.funding.value = 'PARTIAL'
    expect(finder.resultsQuery.value).toBe('?q=law&funding=PARTIAL')
  })
})
