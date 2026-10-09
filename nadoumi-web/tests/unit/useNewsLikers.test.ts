import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useNewsLikers } from '~/composables/useNewsLikers'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

beforeEach(() => studentFetch.mockReset())

describe('useNewsLikers', () => {
  it('loads the most recent likers and the total', async () => {
    studentFetch.mockResolvedValueOnce({
      total: 31,
      likers: [{ displayName: 'Amina', avatarUrl: null }, { displayName: 'Jane Smith', avatarUrl: 'https://cdn.example/j.jpg' }],
    })
    const l = useNewsLikers('studying-in-chengdu')

    await l.load()

    expect(studentFetch).toHaveBeenCalledWith('news/studying-in-chengdu/likes', { query: { limit: 30 } })
    expect(l.total.value).toBe(31)
    expect(l.likers.value.map((x: { displayName: string }) => x.displayName)).toEqual(['Amina', 'Jane Smith'])
    expect(l.failed.value).toBe(false)
    expect(l.loaded.value).toBe(true)
  })

  it('shows a busy state while loading', async () => {
    let resolve!: (v: unknown) => void
    studentFetch.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const l = useNewsLikers('a')

    const pending = l.load()
    expect(l.loading.value).toBe(true)
    resolve({ total: 0, likers: [] })
    await pending

    expect(l.loading.value).toBe(false)
  })

  it('reports a failure and can be retried', async () => {
    studentFetch.mockRejectedValueOnce(new Error('offline'))
    const l = useNewsLikers('a')

    await l.load()
    expect(l.failed.value).toBe(true)
    expect(l.loaded.value).toBe(false)

    studentFetch.mockResolvedValueOnce({ total: 1, likers: [{ displayName: 'Luc', avatarUrl: null }] })
    await l.load()
    expect(l.failed.value).toBe(false)
    expect(l.likers.value).toHaveLength(1)
  })

  it('does not start a second request while one is running', async () => {
    let resolve!: (v: unknown) => void
    studentFetch.mockReturnValueOnce(new Promise(r => { resolve = r }))
    const l = useNewsLikers('a')

    const first = l.load()
    await l.load()
    resolve({ total: 0, likers: [] })
    await first

    expect(studentFetch).toHaveBeenCalledTimes(1)
  })
})
