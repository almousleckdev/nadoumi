import { describe, it, expect } from 'vitest'
import { mockNuxtImport } from '@nuxt/test-utils/runtime'
import { useAsyncAction } from '~/composables/useAsyncAction'

// useI18n needs a component setup context; the key itself is enough to assert on.
mockNuxtImport('useI18n', () => () => ({ t: (key: string) => key }))

describe('useAsyncAction', () => {
  it('returns the action result and shows the success notice', async () => {
    const { run, notice, error, busy } = useAsyncAction()

    const result = await run(async () => 42, 'saved')

    expect(result).toBe(42)
    expect(notice.value).toBe('saved')
    expect(error.value).toBe('')
    expect(busy.value).toBe(false)
  })

  it('sets no notice when no success message is given', async () => {
    const { run, notice } = useAsyncAction()

    await run(async () => undefined)

    expect(notice.value).toBe('')
  })

  it('maps a failure to a message, returns undefined, and clears busy', async () => {
    const { run, notice, error, busy } = useAsyncAction()

    const result = await run(async () => { throw Object.assign(new Error('boom'), { statusCode: 500 }) }, 'saved')

    expect(result).toBeUndefined()
    expect(notice.value).toBe('')
    expect(error.value).not.toBe('')
    expect(error.value).not.toContain('boom')
    expect(busy.value).toBe(false)
  })

  it('is busy while the action is in flight', async () => {
    const { run, busy } = useAsyncAction()
    let release!: () => void
    const gate = new Promise<void>((resolve) => { release = resolve })

    const pending = run(() => gate)
    expect(busy.value).toBe(true)
    release()
    await pending

    expect(busy.value).toBe(false)
  })

  it('clears the previous notice and error on the next run', async () => {
    const { run, notice, error } = useAsyncAction()
    await run(async () => { throw new Error('first') })
    expect(error.value).not.toBe('')

    await run(async () => undefined, 'ok')

    expect(error.value).toBe('')
    expect(notice.value).toBe('ok')
  })
})
