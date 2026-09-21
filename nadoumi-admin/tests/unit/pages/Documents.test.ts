import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'
import type { StaffDocument } from '@/api/document'

const api = vi.hoisted(() => ({
  listStaffDocuments: vi.fn(),
  listDocumentTypes: vi.fn(),
  getStaffDocument: vi.fn(),
  verifyDocument: vi.fn(),
  rejectDocument: vi.fn(),
}))
vi.mock('@/api/document', async (orig) => ({ ...(await orig<typeof import('@/api/document')>()), ...api }))

const openDocumentFile = vi.hoisted(() => vi.fn())
vi.mock('@/utils/documentFile', () => ({ openDocumentFile }))

import Documents from '@/views/documents/index.vue'
import DocumentDrawer from '@/views/documents/DocumentDrawer.vue'
import { useUserStore } from '@/stores/user'

// el-drawer / el-dialog teleport; render their slots inline so the markup can be asserted.
const ElDrawerStub = { name: 'ElDrawer', props: ['modelValue', 'title'], template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>' }
const ElDialogStub = { name: 'ElDialog', props: ['modelValue', 'title'], template: '<div v-if="modelValue"><slot /><slot name="footer" /></div>' }

const doc = (over: Partial<StaffDocument> = {}): StaffDocument => ({
  id: 1, applicantId: 42, applicationId: null, docType: 'TRANSCRIPT', status: 'SUBMITTED', expiresOn: null,
  reviewerUserId: null, rejectionReason: null,
  versions: [{
    id: 11, versionNo: 1, contentType: 'application/pdf', sizeBytes: 2048,
    uploadedAt: '2026-02-01T09:00:00', verificationStatus: 'PENDING', verifiedAt: null,
  }],
  events: [{ id: 5, eventType: 'SUBMITTED', actorUserId: 42, at: '2026-02-01T09:00:00', detail: null }],
  ...over,
})

function opts() {
  const base = mountOpts()
  return { global: { ...base.global, stubs: { ElDrawer: ElDrawerStub, ElDialog: ElDialogStub } } }
}

beforeEach(() => {
  setActivePinia(createPinia())
  useUserStore().permissions = ['*:*:*']
  Object.values(api).forEach(fn => fn.mockReset())
  openDocumentFile.mockReset().mockResolvedValue(undefined)
  api.listStaffDocuments.mockResolvedValue([doc(), doc({ id: 2, docType: 'VISA', status: 'VERIFIED' })])
  api.listDocumentTypes.mockResolvedValue({ data: [{ dictLabel: 'Transcript', dictValue: 'TRANSCRIPT' }, { dictLabel: 'Visa', dictValue: 'VISA' }] })
})

describe('Documents review queue', () => {
  it('loads real documents and labels them from the type dictionary', async () => {
    const w = mount(Documents, opts())
    await flushPromises()

    expect(api.listStaffDocuments).toHaveBeenCalledWith({})
    expect(w.text()).toContain('Transcript')
    expect(w.text()).toContain('#42')
    expect(w.text()).toContain('Verified')
  })

  it('falls back to the raw type code when the dictionary cannot be loaded', async () => {
    api.listDocumentTypes.mockRejectedValue(new Error('403'))
    const w = mount(Documents, opts())
    await flushPromises()
    expect(w.text()).toContain('TRANSCRIPT')
  })

  it('filters by status and type over the fetched list, and asks the server only for the applicant', async () => {
    const w = mount(Documents, opts())
    await flushPromises()
    const vm = w.vm as unknown as { filters: { status: string, docType: string, applicantId: string }, reload: () => Promise<void> }

    vm.filters.status = 'VERIFIED'
    await vm.reload()
    await flushPromises()
    expect(w.findComponent({ name: 'ElTable' }).props('data')).toHaveLength(1)

    vm.filters.status = ''
    vm.filters.applicantId = '42'
    await vm.reload()
    await flushPromises()
    expect(api.listStaffDocuments).toHaveBeenLastCalledWith({ applicantId: 42 })
  })

  it('shows an honest empty state', async () => {
    api.listStaffDocuments.mockResolvedValue([])
    const w = mount(Documents, opts())
    await flushPromises()
    expect(w.text()).toContain('No documents match')
  })

  it('shows a retryable error when the queue cannot be loaded', async () => {
    api.listStaffDocuments.mockRejectedValueOnce(new Error('boom'))
    const w = mount(Documents, opts())
    await flushPromises()
    expect(w.text()).toContain('boom')

    await w.findAll('button').find(b => /retry|try again/i.test(b.text()))!.trigger('click')
    await flushPromises()
    expect(api.listStaffDocuments).toHaveBeenCalledTimes(2)
  })

  it('opens the detail drawer with the selected document', async () => {
    const w = mount(Documents, opts())
    await flushPromises()
    const vm = w.vm as unknown as { openDetail: (d: StaffDocument) => void }

    vm.openDetail(doc())
    await flushPromises()

    expect(w.findComponent(DocumentDrawer).props('doc')).toMatchObject({ id: 1 })
    expect(w.text()).toContain('v1')
  })
})

describe('DocumentDrawer', () => {
  function drawer(d: StaffDocument, perms: string[] = ['*:*:*']) {
    useUserStore().permissions = perms
    return mount(DocumentDrawer, { ...opts(), props: { modelValue: true, doc: d, typeLabel: 'Transcript' } })
  }
  const button = (w: ReturnType<typeof drawer>, test: string) => w.find(`[data-test="${test}"]`)

  it('shows the version history and the audit trail', async () => {
    const w = drawer(doc({ events: [
      { id: 1, eventType: 'SUBMITTED', actorUserId: 42, at: '2026-02-01T09:00:00', detail: null },
      { id: 2, eventType: 'REJECTED', actorUserId: 9, at: '2026-02-02T09:00:00', detail: 'Blurry scan' },
    ] }))
    expect(w.text()).toContain('v1')
    expect(w.text()).toContain('application/pdf')
    expect(w.text()).toContain('Blurry scan')
  })

  it('verifies a document and tells the list to refresh', async () => {
    api.verifyDocument.mockResolvedValue(undefined)
    const w = drawer(doc())
    await button(w, 'verify').trigger('click')
    await flushPromises()

    expect(api.verifyDocument).toHaveBeenCalledWith(1)
    expect(w.emitted('changed')).toHaveLength(1)
  })

  it('will not reject without a reason', async () => {
    const w = drawer(doc())
    await button(w, 'reject').trigger('click')
    await button(w, 'confirm-reject').trigger('click')
    await flushPromises()

    expect(api.rejectDocument).not.toHaveBeenCalled()
    expect(w.text()).toContain('This field is required')
  })

  it('rejects with the trimmed reason the applicant will see', async () => {
    api.rejectDocument.mockResolvedValue(undefined)
    const w = drawer(doc())
    await button(w, 'reject').trigger('click')
    await w.find('textarea[data-test="reason"]').setValue('  Scan is blurry  ')
    await button(w, 'confirm-reject').trigger('click')
    await flushPromises()

    expect(api.rejectDocument).toHaveBeenCalledWith(1, 'Scan is blurry')
    expect(w.emitted('changed')).toHaveLength(1)
  })

  it('hides verify and reject without the permissions, and download without nad:document:download', async () => {
    const w = drawer(doc(), ['nad:document:view'])
    expect(button(w, 'verify').exists()).toBe(false)
    expect(button(w, 'reject').exists()).toBe(false)
    expect(button(w, 'download').exists()).toBe(false)
  })

  it('offers no verdict on a document that has no file yet', async () => {
    const w = drawer(doc({ status: 'DRAFT', versions: [], events: [] }))
    expect(button(w, 'verify').exists()).toBe(false)
    expect(w.text()).toContain('No file has been uploaded yet.')
  })

  it('does not offer verify on an already verified document, but still allows reject', async () => {
    const w = drawer(doc({ status: 'VERIFIED' }))
    expect(button(w, 'verify').exists()).toBe(false)
    expect(button(w, 'reject').exists()).toBe(true)
  })

  it('downloads a version through the backend-chosen delivery', async () => {
    const w = drawer(doc())
    await button(w, 'download').trigger('click')
    await flushPromises()
    expect(openDocumentFile).toHaveBeenCalledWith(1, 1)
  })

  it('shows the reviewer reason on a rejected document', async () => {
    const w = drawer(doc({ status: 'REJECTED', rejectionReason: 'Expired scan' }))
    expect(w.text()).toContain('Expired scan')
  })
})
