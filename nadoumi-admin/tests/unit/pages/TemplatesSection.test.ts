import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ listTemplates: vi.fn(), addTemplate: vi.fn(), removeTemplate: vi.fn() }))
vi.mock('@/api/documentTemplate', async orig => ({ ...(await orig<typeof import('@/api/documentTemplate')>()), ...api }))

const message = vi.hoisted(() => ({ warning: vi.fn(), success: vi.fn() }))
vi.mock('element-plus', async (orig) => {
  const actual = await orig<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: message.warning, success: message.success } }
})

import TemplatesSection from '@/views/shared/TemplatesSection.vue'

function mountSection(refId: number | null) {
  return mount(TemplatesSection, { props: { scope: 'programs', refId }, ...mountOpts() })
}

async function pick(w: ReturnType<typeof mountSection>, file: File) {
  const input = w.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', { value: [file], configurable: true })
  await input.trigger('change')
  await flushPromises()
}

describe('TemplatesSection', () => {
  beforeEach(() => {
    Object.values(api).forEach(fn => fn.mockReset())
    message.warning.mockReset()
    message.success.mockReset()
    api.listTemplates.mockResolvedValue([{ id: 1, title: 'Application form template', fileName: 'form.pdf' }])
  })

  it('lists the templates of an existing record', async () => {
    const w = mountSection(7)
    await flushPromises()

    expect(api.listTemplates).toHaveBeenCalledWith('programs', 7)
    expect(w.text()).toContain('Application form template')
    expect(w.text()).toContain('form.pdf')
  })

  it('asks to save the record first when it has no id yet', async () => {
    const w = mountSection(null)
    await flushPromises()

    expect(api.listTemplates).not.toHaveBeenCalled()
    expect(w.find('input[type="file"]').exists()).toBe(false)
    expect(w.text()).toContain('Save the record first')
  })

  it('refuses an upload without a title, and uploads with one', async () => {
    api.addTemplate.mockResolvedValue({ id: 2, title: 'T', fileName: 't.pdf' })
    const w = mountSection(7)
    await flushPromises()
    const file = new File(['%PDF'], 't.pdf', { type: 'application/pdf' })

    await pick(w, file)
    expect(api.addTemplate).not.toHaveBeenCalled()
    expect(message.warning).toHaveBeenCalled()

    await w.find('input.el-input__inner').setValue('Form')
    await pick(w, file)
    expect(api.addTemplate).toHaveBeenCalledWith('programs', 7, 'Form', file)
  })
})
