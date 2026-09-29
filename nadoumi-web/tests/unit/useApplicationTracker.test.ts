import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useApplicationTracker } from '~/composables/useApplicationTracker'

const studentFetch = vi.fn()
vi.mock('~/composables/useApi', () => ({
  useApi: () => ({ studentFetch, publicGet: vi.fn() }),
}))

beforeEach(() => {
  studentFetch.mockReset()
})

describe('useApplicationTracker', () => {
  it('does nothing for a blank reference', async () => {
    const tracker = useApplicationTracker()
    tracker.appId.value = '   '
    await tracker.track()
    expect(studentFetch).not.toHaveBeenCalled()
    expect(tracker.state.value).toBe('idle')
  })

  it('loads the application, encoding the reference', async () => {
    studentFetch.mockResolvedValue({ id: 7, status: 'IN_REVIEW' })
    const tracker = useApplicationTracker()
    tracker.appId.value = ' 7/../x '
    await tracker.track()
    expect(studentFetch).toHaveBeenCalledWith('applications/7%2F..%2Fx')
    expect(tracker.state.value).toBe('ok')
    expect(tracker.application.value).toEqual({ id: 7, status: 'IN_REVIEW' })
  })

  it('reports not found for a real 404', async () => {
    studentFetch.mockRejectedValue({ statusCode: 404 })
    const tracker = useApplicationTracker()
    tracker.appId.value = '9'
    await tracker.track()
    expect(tracker.state.value).toBe('notfound')
    expect(tracker.application.value).toBeNull()
  })

  it('also understands a plain status field', async () => {
    studentFetch.mockRejectedValue({ status: 404 })
    const tracker = useApplicationTracker()
    tracker.appId.value = '9'
    await tracker.track()
    expect(tracker.state.value).toBe('notfound')
  })

  it('falls back to unavailable for any other failure', async () => {
    studentFetch.mockRejectedValue({ statusCode: 502 })
    const tracker = useApplicationTracker()
    tracker.appId.value = '9'
    await tracker.track()
    expect(tracker.state.value).toBe('unavailable')
    expect(tracker.tracking.value).toBe(false)
  })

  it('ignores a second lookup while one is running', async () => {
    let resolve!: (v: unknown) => void
    studentFetch.mockReturnValue(new Promise((r) => { resolve = r }))
    const tracker = useApplicationTracker()
    tracker.appId.value = '9'
    const first = tracker.track()
    await tracker.track()
    expect(studentFetch).toHaveBeenCalledOnce()
    resolve({ id: 9 })
    await first
  })
})
