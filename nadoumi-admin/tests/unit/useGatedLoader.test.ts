import { describe, it, expect, vi } from 'vitest'
import { useGatedLoader } from '@/views/dashboard/useGatedLoader'

describe('useGatedLoader', () => {
  it('keeps the initial value and never fetches while disabled', async () => {
    const fetch = vi.fn().mockResolvedValue(['x'])
    const { data, loading, load } = useGatedLoader<string[]>([], () => false, fetch)
    await load()
    expect(fetch).not.toHaveBeenCalled()
    expect(data.value).toEqual([])
    expect(loading.value).toBe(false)
  })

  it('stores the fetched value and toggles loading around the request', async () => {
    let resolve!: (v: string[]) => void
    const fetch = vi.fn().mockReturnValue(new Promise<string[]>((r) => { resolve = r }))
    const { data, loading, load } = useGatedLoader<string[]>([], () => true, fetch)
    const pending = load()
    expect(loading.value).toBe(true)
    resolve(['a', 'b'])
    await pending
    expect(loading.value).toBe(false)
    expect(data.value).toEqual(['a', 'b'])
  })

  it('falls back to the initial value when the request fails', async () => {
    const fetch = vi.fn().mockRejectedValue(new Error('boom'))
    const { data, loading, load } = useGatedLoader<string[]>(['seed'], () => true, fetch)
    await load()
    expect(data.value).toEqual(['seed'])
    expect(loading.value).toBe(false)
  })

  it('re-evaluates the gate on every load', async () => {
    let allowed = false
    const fetch = vi.fn().mockResolvedValue(1)
    const { load } = useGatedLoader<number>(0, () => allowed, fetch)
    await load()
    allowed = true
    await load()
    expect(fetch).toHaveBeenCalledOnce()
  })
})
