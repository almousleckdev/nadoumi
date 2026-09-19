import { describe, it, expect, vi, beforeEach } from 'vitest'
import { defineComponent, h, nextTick, ref } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { testI18n } from '../helpers'
import { useDrawerForm, type DrawerFormOptions } from '@/composables/useDrawerForm'

const message = vi.hoisted(() => ({ success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, success: message.success } }
})

interface Form { name: string }

/** Mounts a host component so the composable runs inside a real setup with i18n. */
function host(overrides: Partial<DrawerFormOptions<Form>> = {}) {
  const open = ref(false)
  const options = {
    isOpen: () => open.value,
    blank: (): Form => ({ name: '' }),
    submit: vi.fn().mockResolvedValue(undefined),
    onSaved: vi.fn(),
    ...overrides,
  }
  let api!: ReturnType<typeof useDrawerForm<Form>>
  const Host = defineComponent({
    setup() {
      api = useDrawerForm<Form>(options)
      return () => h('div')
    },
  })
  mount(Host, { global: { plugins: [testI18n()] } })
  return { open, api, options }
}

describe('useDrawerForm', () => {
  beforeEach(() => message.success.mockReset())

  it('resets the form and loads when the drawer opens, with loading on while it runs', async () => {
    let release!: () => void
    const load = vi.fn((form: Form) => new Promise<void>((resolve) => { form.name = 'loaded'; release = resolve }))
    const { open, api } = host({ load })

    open.value = true
    await nextTick()

    expect(load).toHaveBeenCalledOnce()
    expect(api.loading.value).toBe(true)
    release()
    await flushPromises()
    expect(api.loading.value).toBe(false)
    expect(api.form.name).toBe('loaded')
  })

  it('does not load when the drawer closes', async () => {
    const load = vi.fn().mockResolvedValue(undefined)
    const { open } = host({ load })
    open.value = true
    await nextTick()
    load.mockClear()

    open.value = false
    await nextTick()

    expect(load).not.toHaveBeenCalled()
  })

  it('turns loading off, without an unhandled rejection, when the load fails', async () => {
    const { open, api } = host({ load: vi.fn().mockRejectedValue(new Error('boom')) })

    open.value = true
    await flushPromises()

    expect(api.loading.value).toBe(false)
  })

  it('submits the form, confirms and reports the save', async () => {
    const { api, options } = host()
    api.form.name = 'Ada'

    await api.save()

    expect(options.submit).toHaveBeenCalledWith(expect.objectContaining({ name: 'Ada' }))
    expect(message.success).toHaveBeenCalledOnce()
    expect(options.onSaved).toHaveBeenCalledOnce()
    expect(api.saving.value).toBe(false)
  })

  it('reports nothing and stops saving when the request fails', async () => {
    const { api, options } = host({ submit: vi.fn().mockRejectedValue(new Error('nope')) })

    await expect(api.save()).rejects.toThrow('nope')

    expect(message.success).not.toHaveBeenCalled()
    expect(options.onSaved).not.toHaveBeenCalled()
    expect(api.saving.value).toBe(false)
  })

  it('reset restores the blank form and clears extra state', async () => {
    const onReset = vi.fn()
    const { api } = host({ onReset })
    api.form.name = 'dirty'

    api.reset()

    expect(api.form.name).toBe('')
    expect(onReset).toHaveBeenCalledOnce()
  })
})
