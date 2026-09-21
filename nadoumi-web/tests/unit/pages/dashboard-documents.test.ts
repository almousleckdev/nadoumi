import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import Documents from '~/pages/dashboard/documents.vue'
import AddDocumentForm from '~/components/documents/AddDocumentForm.vue'
import type { StudentDocumentDto } from '~/types/documents'
import { fakeFile, pickFile } from '../helpers/files'

const applicant = {
  id: 1, givenName: 'ADA', familyName: 'LOVELACE', dob: '2000-01-01', email: 'a@b.co', emailVerified: true,
}
const EMPTY_PASSPORT = {
  passportNo: null, givenName: null, familyName: null, dob: null, issueDate: null, expiryDate: null,
  readMethod: null, edited: false, scanUploaded: false, validForAdmission: false, matchesProfile: false, mismatches: [],
}

const doc = (over: Partial<StudentDocumentDto> = {}): StudentDocumentDto => ({
  id: 10, applicationId: null, docType: 'TRANSCRIPT', status: 'SUBMITTED', expiresOn: null, rejectionReason: null,
  currentVersion: {
    id: 100, versionNo: 2, contentType: 'application/pdf', sizeBytes: 2 * 1024 * 1024,
    uploadedAt: '2026-02-03T10:00:00', verificationStatus: 'PENDING', verifiedAt: null,
  },
  ...over,
})

const api = vi.hoisted(() => ({
  list: vi.fn(), create: vi.fn(), replace: vi.fn(), remove: vi.fn(), fileAccess: vi.fn(), types: vi.fn(),
}))
const listMine = vi.fn()
const get = vi.fn()

vi.mock('~/composables/useSession', () => ({
  useSession: () => ({ activeApplicantId: ref(1) }),
}))
vi.mock('~/composables/useApplicant', () => ({
  useApplicant: () => ({
    listMine, get, photoUrl: vi.fn().mockRejectedValue({ statusCode: 404 }),
    passportStatus: vi.fn().mockResolvedValue(EMPTY_PASSPORT),
  }),
}))
vi.mock('~/composables/useDocuments', () => ({ useDocuments: () => api }))

async function mountPage() {
  const w = await mountSuspended(Documents)
  await flushPromises()
  return w
}

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  listMine.mockReset().mockResolvedValue([applicant])
  get.mockReset().mockResolvedValue(applicant)
  api.list.mockResolvedValue([doc()])
  api.types.mockResolvedValue([{ value: 'TRANSCRIPT', label: 'Transcript' }, { value: 'VISA', label: 'Visa' }])
})

describe('dashboard documents page', () => {
  it('shows shimmer placeholders while the documents load', async () => {
    api.list.mockReturnValue(new Promise(() => {}))
    const w = await mountSuspended(Documents)
    await flushPromises()

    expect(w.findAll('.n-skeleton').length).toBeGreaterThan(0)
    expect(w.find('[data-test="document-row"]').exists()).toBe(false)
  })

  it('shows an honest empty state, never a fake document', async () => {
    api.list.mockResolvedValue([])
    const w = await mountPage()

    expect(w.find('[data-test="docs-empty"]').text()).toContain('not uploaded any documents')
    expect(w.find('[data-test="document-row"]').exists()).toBe(false)
  })

  it('lists real documents with the dictionary label, status and version facts', async () => {
    const w = await mountPage()
    const row = w.find('[data-test="document-row"]')

    expect(row.text()).toContain('Transcript')
    expect(row.text()).toContain('Uploaded')
    expect(row.text()).toContain('Version 2')
    expect(row.text()).toContain('2.0 MB')
  })

  it('falls back to the raw type code when the dictionary does not know it', async () => {
    api.list.mockResolvedValue([doc({ docType: 'MYSTERY_TYPE' })])
    const w = await mountPage()
    expect(w.find('[data-test="document-row"]').text()).toContain('MYSTERY_TYPE')
  })

  it('shows the reviewer reason on a rejected document', async () => {
    api.list.mockResolvedValue([doc({ status: 'REJECTED', rejectionReason: 'Scan is blurry' })])
    const w = await mountPage()

    expect(w.text()).toContain('Rejected')
    expect(w.text()).toContain('Reason: Scan is blurry')
  })

  it('only offers delete on a draft', async () => {
    api.list.mockResolvedValue([doc({ id: 1, status: 'DRAFT', currentVersion: null }), doc({ id: 2, status: 'VERIFIED' })])
    const w = await mountPage()

    expect(w.findAll('[data-test="doc-delete"]')).toHaveLength(1)
    expect(w.findAll('[data-test="doc-download"]')).toHaveLength(1)
  })

  it('shows a retryable error when the list cannot be loaded', async () => {
    api.list.mockRejectedValueOnce({ statusCode: 500 })
    const w = await mountPage()
    expect(w.text()).toContain('could not be loaded')

    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(api.list).toHaveBeenCalledTimes(2)
    expect(w.find('[data-test="document-row"]').exists()).toBe(true)
  })

  it('uploads a new document with the chosen type, then refreshes the list', async () => {
    api.create.mockResolvedValue(doc())
    const w = await mountPage()
    const form = w.findComponent(AddDocumentForm)
    const file = fakeFile('visa.pdf', 'application/pdf')

    await form.find('#docType').setValue('VISA')
    await pickFile(form, file)
    await flushPromises()

    expect(api.create).toHaveBeenCalledWith(1, 'VISA', file)
    expect(api.list).toHaveBeenCalledTimes(2)
    expect(w.text()).toContain('Document uploaded.')
  })

  it('refuses a file until a document type is chosen', async () => {
    const w = await mountPage()
    await pickFile(w.findComponent(AddDocumentForm), fakeFile('visa.pdf', 'application/pdf'))
    await flushPromises()

    expect(api.create).not.toHaveBeenCalled()
    expect(w.text()).toContain('This field is required.')
  })

  it('rejects an unsupported file type before it reaches the server', async () => {
    const w = await mountPage()
    const form = w.findComponent(AddDocumentForm)
    await form.find('#docType').setValue('VISA')
    await pickFile(form, fakeFile('notes.txt', 'text/plain'))
    await flushPromises()

    expect(api.create).not.toHaveBeenCalled()
    expect(w.text()).toContain('Only PDF, JPEG or PNG')
  })

  it('does not offer new uploads when the type dictionary cannot be loaded', async () => {
    api.types.mockRejectedValue({ statusCode: 500 })
    const w = await mountPage()

    expect(w.find('[data-test="types-unavailable"]').exists()).toBe(true)
    expect(w.findComponent(AddDocumentForm).exists()).toBe(false)
  })

  it('opens a signed URL for a download', async () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    api.fileAccess.mockResolvedValue({ kind: 'url', url: 'https://signed.example/f' })
    const w = await mountPage()

    await w.find('[data-test="doc-download"]').trigger('click')
    await flushPromises()

    expect(api.fileAccess).toHaveBeenCalledWith(10, 2)
    expect(open).toHaveBeenCalledWith('https://signed.example/f', '_blank', 'noopener')
    open.mockRestore()
  })

  it('uploads a replacement as a new version of the same document', async () => {
    api.replace.mockResolvedValue(doc())
    const w = await mountPage()
    const file = fakeFile('v3.pdf', 'application/pdf')

    await pickFile(w, file, '[data-test="doc-replace"]')
    await flushPromises()

    expect(api.replace).toHaveBeenCalledWith(10, file)
    expect(w.text()).toContain('New version uploaded.')
  })

  it('shows the identity cards (photo, passport) as part of Your documents, not a separate section', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Your documents')
    expect(w.text()).toContain('Passport')
    expect(w.text()).not.toContain('Identity documents')
  })

  it('shows Add a document as its own left/right section, separate from the list', async () => {
    const w = await mountPage()
    expect(w.text()).toContain('Add a document')
  })
})
