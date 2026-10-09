import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useToast } from '~/composables/useToast'

beforeEach(() => {
  vi.useFakeTimers()
  useToast().toasts.value = []
})
afterEach(() => vi.useRealTimers())

describe('useToast', () => {
  it('shows a toast and dismisses it by itself', () => {
    const toast = useToast()
    toast.success('Saved')
    expect(toast.toasts.value).toHaveLength(1)
    vi.advanceTimersByTime(3600)
    expect(toast.toasts.value).toHaveLength(0)
  })

  it('keeps an error on screen longer than a success', () => {
    const toast = useToast()
    toast.error('Could not save')
    vi.advanceTimersByTime(4000)
    expect(toast.toasts.value).toHaveLength(1)
    vi.advanceTimersByTime(2100)
    expect(toast.toasts.value).toHaveLength(0)
  })

  it('treats the same message twice in a row as one event', () => {
    const toast = useToast()
    toast.success('Photo saved')
    toast.success('Photo saved')
    expect(toast.toasts.value).toHaveLength(1)
  })

  it('shows at most four at once, newest last', () => {
    const toast = useToast()
    for (const n of [1, 2, 3, 4, 5]) toast.info(`n${n}`)
    expect(toast.toasts.value.map(t => t.message)).toEqual(['n2', 'n3', 'n4', 'n5'])
  })

  it('dismisses on demand and ignores an empty message', () => {
    const toast = useToast()
    toast.success('')
    expect(toast.toasts.value).toHaveLength(0)
    toast.success('Done')
    toast.dismiss(toast.toasts.value[0]!.id)
    expect(toast.toasts.value).toHaveLength(0)
  })
})
