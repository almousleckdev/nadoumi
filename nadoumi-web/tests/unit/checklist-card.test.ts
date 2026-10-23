import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import DocumentChecklistCard from '~/components/applicant/DocumentChecklistCard.vue'

const api = vi.hoisted(() => ({
  checklist: vi.fn(), templates: vi.fn(), template: vi.fn(), list: vi.fn(), create: vi.fn(), replace: vi.fn(), fileAccess: vi.fn(),
}))
mockNuxtImport('useApplications', () => () => ({ checklist: api.checklist, templates: api.templates, template: api.template }))
mockNuxtImport('useDocuments', () => () => ({ list: api.list, create: api.create, replace: api.replace, fileAccess: api.fileAccess }))

const row = (docType: string, over: Record<string, unknown> = {}) => ({
  docType, mandatory: true, note: null, status: 'MISSING', documentId: null, rejectionReason: null, ...over,
})

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  api.templates.mockResolvedValue([])
  api.list.mockResolvedValue([])
})

async function mountCard() {
  const w = await mountSuspended(DocumentChecklistCard, { props: { applicationId: 42, applicantId: 9 } })
  await flushPromises()
  return w
}

describe('DocumentChecklistCard', () => {
  it('shows nothing, and holds nothing back, when no documents are required', async () => {
    api.checklist.mockResolvedValue([])
    const w = await mountCard()

    expect(w.find('[data-test="checklist"]').exists()).toBe(false)
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
  })

  it('lists each document and says submission is not ready while a mandatory one is missing', async () => {
    api.checklist.mockResolvedValue([row('PASSPORT'), row('CV', { mandatory: false })])
    const w = await mountCard()

    expect(w.findAll('[data-test="checklist-row"]')).toHaveLength(2)
    expect(w.text()).toContain('Passport')
    expect(w.text()).toContain('Not uploaded')
    expect(w.emitted('ready')?.at(-1)).toEqual([false])
  })

  it('creates a document for a missing type, scoped to the application, then reloads', async () => {
    api.checklist.mockResolvedValueOnce([row('PASSPORT')]).mockResolvedValueOnce([row('PASSPORT', { status: 'SUBMITTED', documentId: 5 })])
    api.create.mockResolvedValue({})
    const w = await mountCard()

    const file = new File(['%PDF'], 'passport.pdf', { type: 'application/pdf' })
    const input = w.find('[data-test="upload-PASSPORT"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(api.create).toHaveBeenCalledWith(9, 'PASSPORT', file, 42)
    expect(w.emitted('ready')?.at(-1)).toEqual([true])
  })

  it('uploads a new version over a rejected document and shows the reason', async () => {
    api.checklist.mockResolvedValue([row('TRANSCRIPT', { status: 'REJECTED', documentId: 7, rejectionReason: 'Blurry scan' })])
    api.replace.mockResolvedValue({})
    const w = await mountCard()

    expect(w.text()).toContain('Blurry scan')
    const file = new File(['%PDF'], 'transcript.pdf', { type: 'application/pdf' })
    const input = w.find('[data-test="upload-TRANSCRIPT"]')
    Object.defineProperty(input.element, 'files', { value: [file] })
    await input.trigger('change')
    await flushPromises()

    expect(api.replace).toHaveBeenCalledWith(7, file)
  })

  it('lets the student view an uploaded picture or PDF without downloading it', async () => {
    const create = vi.fn(() => 'blob:doc-preview')
    vi.stubGlobal('URL', Object.assign(URL, { createObjectURL: create, revokeObjectURL: vi.fn() }))
    api.checklist.mockResolvedValue([row('PASSPORT', { status: 'SUBMITTED', documentId: 5 })])
    api.list.mockResolvedValue([{ id: 5, docType: 'PASSPORT', status: 'SUBMITTED', currentVersion: { versionNo: 2, contentType: 'image/png' } }])
    api.fileAccess.mockResolvedValue({ kind: 'blob', blob: new Blob(['x'], { type: 'image/png' }), filename: 'p.png' })
    const w = await mountCard()

    await w.find('[data-test="checklist-view"]').trigger('click')
    await flushPromises()

    expect(api.fileAccess).toHaveBeenCalledWith(5, 2)
    expect(document.body.querySelector('[data-test="preview-image"]')?.getAttribute('src')).toBe('blob:doc-preview')
    vi.unstubAllGlobals()
  })

  it('offers no view button for a document whose type cannot be shown, or that is not uploaded', async () => {
    api.checklist.mockResolvedValue([row('PASSPORT'), row('CV', { status: 'SUBMITTED', documentId: 6 })])
    api.list.mockResolvedValue([{ id: 6, docType: 'CV', status: 'SUBMITTED', currentVersion: { versionNo: 1, contentType: 'application/msword' } }])
    const w = await mountCard()

    expect(w.find('[data-test="checklist-view"]').exists()).toBe(false)
  })

  it('puts the form to download right on the required application form row', async () => {
    api.checklist.mockResolvedValue([row('APPLICATION_FORM'), row('PASSPORT')])
    api.templates.mockResolvedValue([{ id: 3, title: 'Application form', fileName: 'form.doc' }])
    const w = await mountCard()

    const downloads = w.find('[data-test="form-downloads"]')
    expect(downloads.exists()).toBe(true)
    expect(downloads.text()).toContain('Application form')
    expect(w.findAll('[data-test="form-downloads"]')).toHaveLength(1) // only the form row, not the passport row
    expect(w.text()).toContain('It is required')
  })
})
