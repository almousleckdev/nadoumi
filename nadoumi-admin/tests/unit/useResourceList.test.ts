import { describe, it, expect, vi } from 'vitest'
import { useResourceList } from '@/composables/useResourceList'

describe('useResourceList', () => {
  it('loads once and skips a second load unless forced', async () => {
    const fetcher = vi.fn().mockResolvedValue([{ id: 1 }, { id: 2 }])
    const list = useResourceList<{ id: number }>(fetcher)

    await list.load()
    await list.load()
    expect(fetcher).toHaveBeenCalledTimes(1)
    expect(list.items.value).toHaveLength(2)
    expect(list.loaded.value).toBe(true)

    await list.reload()
    expect(fetcher).toHaveBeenCalledTimes(2)
  })

  it('captures a fetch error and clears loading', async () => {
    const list = useResourceList(() => Promise.reject(new Error('boom')))
    await list.load()
    expect(list.error.value).toBe('boom')
    expect(list.loading.value).toBe(false)
    expect(list.loaded.value).toBe(false)
  })
})
