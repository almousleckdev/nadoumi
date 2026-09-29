import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'

const api = vi.hoisted(() => ({ myUnreadCount: vi.fn() }))
vi.mock('@/api/notification', () => api)

import { useUnreadCount, UNREAD_POLL_MS } from '@/composables/useUnreadCount'

function host() {
  let exposed!: ReturnType<typeof useUnreadCount>
  const w = mount(defineComponent({
    setup() {
      exposed = useUnreadCount()
      return () => null
    },
  }))
  return { w, api: exposed }
}

describe('useUnreadCount', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    api.myUnreadCount.mockReset()
    api.myUnreadCount.mockResolvedValue({ count: 3 })
  })
  afterEach(() => vi.useRealTimers())

  it('loads the count on mount', async () => {
    const { api: unread } = host()
    await flushPromises()
    expect(unread.unread.value).toBe(3)
  })

  it('polls on an interval and stops after unmount', async () => {
    const { w } = host()
    await flushPromises()
    api.myUnreadCount.mockResolvedValue({ count: 5 })
    await vi.advanceTimersByTimeAsync(UNREAD_POLL_MS)
    expect(api.myUnreadCount).toHaveBeenCalledTimes(2)
    w.unmount()
    await vi.advanceTimersByTimeAsync(UNREAD_POLL_MS * 2)
    expect(api.myUnreadCount).toHaveBeenCalledTimes(2)
  })

  it('keeps the last count when a poll fails', async () => {
    const { api: unread } = host()
    await flushPromises()
    api.myUnreadCount.mockRejectedValue(new Error('offline'))
    await vi.advanceTimersByTimeAsync(UNREAD_POLL_MS)
    expect(unread.unread.value).toBe(3)
  })

  it('clears the count on demand', async () => {
    const { api: unread } = host()
    await flushPromises()
    unread.clear()
    expect(unread.unread.value).toBe(0)
  })
})
