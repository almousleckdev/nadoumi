import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { useAutosave } from '@/views/news/editor/useAutosave'

const DELAY = 1500

function setup(save: () => Promise<void>) {
  const scope = effectScope()
  const autosave = scope.run(() => useAutosave({ save, delayMs: DELAY }))!
  return { autosave, stop: () => scope.stop() }
}

beforeEach(() => vi.useFakeTimers())
afterEach(() => vi.useRealTimers())

describe('useAutosave', () => {
  it('saves once after the quiet period, however many edits came in', async () => {
    const save = vi.fn().mockResolvedValue(undefined)
    const { autosave } = setup(save)

    autosave.markDirty()
    await vi.advanceTimersByTimeAsync(DELAY - 100)
    autosave.markDirty()
    await vi.advanceTimersByTimeAsync(DELAY - 100)
    expect(save).not.toHaveBeenCalled()
    expect(autosave.state.value).toBe('dirty')

    await vi.advanceTimersByTimeAsync(200)
    expect(save).toHaveBeenCalledTimes(1)
    expect(autosave.state.value).toBe('saved')
  })

  it('never runs two saves at once and saves again for edits made meanwhile', async () => {
    let release: () => void = () => {}
    const save = vi.fn().mockImplementationOnce(() => new Promise<void>((r) => { release = r }))
      .mockResolvedValue(undefined)
    const { autosave } = setup(save)

    autosave.markDirty()
    await vi.advanceTimersByTimeAsync(DELAY)
    expect(autosave.state.value).toBe('saving')

    autosave.markDirty() // typed while the first save is in flight
    await vi.advanceTimersByTimeAsync(DELAY * 2)
    expect(save).toHaveBeenCalledTimes(1)

    release()
    await vi.advanceTimersByTimeAsync(DELAY)
    expect(save).toHaveBeenCalledTimes(2)
    expect(autosave.state.value).toBe('saved')
  })

  it('reports an error and retries on the next edit', async () => {
    const save = vi.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(undefined)
    const { autosave } = setup(save)

    autosave.markDirty()
    await vi.advanceTimersByTimeAsync(DELAY)
    expect(autosave.state.value).toBe('error')

    autosave.markDirty()
    await vi.advanceTimersByTimeAsync(DELAY)
    expect(save).toHaveBeenCalledTimes(2)
    expect(autosave.state.value).toBe('saved')
  })

  it('flush saves immediately and resolves only once everything is persisted', async () => {
    const save = vi.fn().mockResolvedValue(undefined)
    const { autosave } = setup(save)

    autosave.markDirty()
    await autosave.flush()

    expect(save).toHaveBeenCalledTimes(1)
    expect(autosave.state.value).toBe('saved')
    await vi.advanceTimersByTimeAsync(DELAY * 2)
    expect(save).toHaveBeenCalledTimes(1) // the pending timer was cancelled
  })

  it('flush rethrows a failed save so the caller can stop (e.g. before publishing)', async () => {
    const save = vi.fn().mockRejectedValue(new Error('boom'))
    const { autosave } = setup(save)

    autosave.markDirty()
    await expect(autosave.flush()).rejects.toThrow('boom')
    expect(autosave.state.value).toBe('error')
  })

  it('flush does nothing when nothing changed', async () => {
    const save = vi.fn().mockResolvedValue(undefined)
    const { autosave } = setup(save)

    await autosave.flush()
    expect(save).not.toHaveBeenCalled()
  })

  it('drops a pending save when its scope is disposed', async () => {
    const save = vi.fn().mockResolvedValue(undefined)
    const { autosave, stop } = setup(save)

    autosave.markDirty()
    stop()
    await vi.advanceTimersByTimeAsync(DELAY * 2)
    expect(save).not.toHaveBeenCalled()
  })
})
