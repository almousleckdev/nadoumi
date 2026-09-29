import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h } from 'vue'
import { mount } from '@vue/test-utils'
import { testI18n } from '../helpers'
import { runDeferred, uploaderTask, useDeferredImages, type DeferredTask } from '@/composables/useDeferredImages'

const message = vi.hoisted(() => ({ warning: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: message.warning } }
})

function host() {
  let api!: ReturnType<typeof useDeferredImages>
  mount(
    defineComponent({
      setup() {
        api = useDeferredImages()
        return () => h('div')
      },
    }),
    { global: { plugins: [testI18n()] } },
  )
  return api
}

describe('runDeferred', () => {
  it('returns zero failures when every task succeeds', async () => {
    const tasks: DeferredTask[] = [async () => true, async () => true]
    expect(await runDeferred(tasks)).toBe(0)
  })

  it('counts each task that resolves false', async () => {
    const tasks: DeferredTask[] = [async () => true, async () => false, async () => false]
    expect(await runDeferred(tasks)).toBe(2)
  })

  it('counts a task that throws as a failure and still runs the rest', async () => {
    const last = vi.fn().mockResolvedValue(true)
    const tasks: DeferredTask[] = [
      async () => {
        throw new Error('network')
      },
      last,
    ]
    expect(await runDeferred(tasks)).toBe(1)
    expect(last).toHaveBeenCalledOnce()
  })

  it('runs tasks one after another in order', async () => {
    const order: number[] = []
    const tasks: DeferredTask[] = [1, 2, 3].map(n => async () => {
      await Promise.resolve()
      order.push(n)
      return true
    })
    await runDeferred(tasks)
    expect(order).toEqual([1, 2, 3])
  })
})

describe('uploaderTask', () => {
  it('succeeds when there is no uploader', async () => {
    expect(await uploaderTask(null, 9)()).toBe(true)
    expect(await uploaderTask(undefined, 9)()).toBe(true)
  })

  it('flushes the uploader with the record id and returns its result', async () => {
    const flush = vi.fn().mockResolvedValue(false)
    expect(await uploaderTask({ flush }, 9)()).toBe(false)
    expect(flush).toHaveBeenCalledWith(9)
  })
})

describe('useDeferredImages', () => {
  beforeEach(() => message.warning.mockClear())

  it('resolves true and stays quiet when everything uploads', async () => {
    const ok = await host().flush([async () => true])
    expect(ok).toBe(true)
    expect(message.warning).not.toHaveBeenCalled()
  })

  it('warns with the failure count when some uploads fail', async () => {
    const ok = await host().flush([async () => false, async () => true, async () => false])
    expect(ok).toBe(false)
    expect(message.warning).toHaveBeenCalledWith(expect.stringContaining('2'))
  })
})
