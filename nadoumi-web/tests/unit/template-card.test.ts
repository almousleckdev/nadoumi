import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import TemplateDownloadCard from '~/components/applicant/TemplateDownloadCard.vue'

const api = vi.hoisted(() => ({ templates: vi.fn(), template: vi.fn() }))
mockNuxtImport('useApplications', () => () => api)

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  URL.createObjectURL = vi.fn(() => 'blob:tpl')
  URL.revokeObjectURL = vi.fn()
})

describe('TemplateDownloadCard', () => {
  it('shows nothing when the opportunity has no templates', async () => {
    api.templates.mockResolvedValue([])
    const w = await mountSuspended(TemplateDownloadCard, { props: { applicationId: 42 } })
    await flushPromises()

    expect(w.find('[data-test="templates"]').exists()).toBe(false)
  })

  it('lists the templates and downloads one as a file', async () => {
    api.templates.mockResolvedValue([{ id: 3, title: 'Application form template', fileName: 'form.doc' }])
    api.template.mockResolvedValue(new Blob(['x']))
    const w = await mountSuspended(TemplateDownloadCard, { props: { applicationId: 42 } })
    await flushPromises()

    expect(w.text()).toContain('Application form template')
    await w.find('[data-test="template-3"]').trigger('click')
    await flushPromises()
    expect(api.template).toHaveBeenCalledWith(42, 3)
  })
})
