import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import DocumentChecklistCard from '~/components/applicant/DocumentChecklistCard.vue'

const api = vi.hoisted(() => ({ checklist: vi.fn(), create: vi.fn(), replace: vi.fn() }))
mockNuxtImport('useApplications', () => () => ({ checklist: api.checklist }))
mockNuxtImport('useDocuments', () => () => ({ create: api.create, replace: api.replace }))

const row = (docType: string, over: Record<string, unknown> = {}) => ({
  docType, mandatory: true, note: null, status: 'MISSING', documentId: null, rejectionReason: null, ...over,
})

beforeEach(() => Object.values(api).forEach(fn => fn.mockReset()))

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
})
