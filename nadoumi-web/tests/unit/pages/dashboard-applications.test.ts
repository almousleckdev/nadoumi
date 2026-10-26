import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ApplicationsList from '~/pages/dashboard/applications/index.vue'
import ApplicationDetail from '~/pages/dashboard/applications/[id].vue'
import type { StudentApplicationDto } from '~/types/catalog'

const APP = (over: Partial<StudentApplicationDto> = {}): StudentApplicationDto => ({
  id: 7, reference: 'NAD20261023001', applicantId: 1, applicationType: 'PROGRAM_WITH_SCHOLARSHIP', programId: 1, scholarshipId: 2, intakeId: 3,
  currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW', submittedAt: '2026-01-15T10:00:00',
  timeline: ['APPLICATION_CREATED', 'STAGE_SUBMIT', 'CASE_ASSIGNED', 'STAGE_CLAIM'],
  title: 'MBBS', universityName: 'Fudan University', intake: 'Autumn', applicantName: 'Amina Diallo', paymentNeeded: false, actionNote: null, ...over,
})

const list = vi.fn()
const get = vi.fn()
const submit = vi.fn()
const checklist = vi.fn()
const templates = vi.fn()
const navigateTo = vi.hoisted(() => vi.fn())
mockNuxtImport('navigateTo', () => navigateTo)
mockNuxtImport('useRoute', () => () => ({ params: { id: '7' }, path: '/dashboard/applications/7' }))
// the fee is settled in these tests; the payment panel has its own suite
mockNuxtImport('useApplicationPayment', () => () => ({
  overview: async () => ({ state: 'CONFIRMED', required: true, settled: true, canStart: false, amount: 300, currency: 'CNY', payee: '', reference: 'NAD-TEST0001', expiresAt: null, secondsLeft: 0, rejectionReason: null }),
}))
mockNuxtImport('useApplicantSignature', () => () => ({
  status: async () => ({ signed: true, signedAt: '2026-10-22T10:00:00' }),
  image: async () => new Blob(['x'], { type: 'image/png' }),
}))
vi.mock('~/composables/useApplications', () => ({
  useApplications: () => ({ list, get, submit, checklist, formPreview: vi.fn(), templates, template: vi.fn() }),
}))

beforeEach(() => {
  navigateTo.mockReset()
  list.mockReset().mockResolvedValue([APP()])
  get.mockReset().mockResolvedValue(APP())
  submit.mockReset()
  checklist.mockReset().mockResolvedValue([])
  templates.mockReset().mockResolvedValue([])
})

async function mountList() {
  const w = await mountSuspended(ApplicationsList)
  await flushPromises()
  return w
}
async function mountDetail() {
  const w = await mountSuspended(ApplicationDetail)
  await flushPromises()
  return w
}

describe('dashboard applications list', () => {
  it('shows an honest empty state that points at scholarships, never fake applications', async () => {
    list.mockResolvedValueOnce([])
    const w = await mountList()
    expect(w.text()).toContain('You have not started an application yet')
    expect(w.findAll('a').some(a => a.attributes('href')?.includes('/scholarships'))).toBe(true)
  })

  it('lists submitted applications as a table with a translated status and a link to the detail page', async () => {
    const w = await mountList()
    expect(w.find('[data-test="applications-table"]').exists()).toBe(true)
    expect(w.text()).toContain('MBBS')
    expect(w.text()).toContain('Fudan University')
    expect(w.text()).toContain('Amina Diallo')
    expect(w.text()).toContain('Eligibility review')
    expect(w.text()).toContain('In review')
    expect(w.text()).toContain('NAD20261023001')
    expect(w.findAll('a').some(a => a.attributes('href')?.endsWith('/dashboard/applications/7'))).toBe(true)
  })

  it('never lists a withdrawn application, and keeps drafts apart from the submitted table', async () => {
    list.mockResolvedValueOnce([
      APP({ id: 8, reference: 'NAD20261023002', currentStatus: 'CLOSED_WITHDRAWN' }),
      APP({ id: 9, reference: 'NAD20261023003', currentStatus: 'DRAFT', submittedAt: null }),
    ])
    const w = await mountList()
    expect(w.text()).not.toContain('NAD20261023002')
    expect(w.find('[data-test="drafts"]').exists()).toBe(true)
    expect(w.find('[data-test="applications-table"]').exists()).toBe(false)
  })

  it('sends a draft to the step-by-step flow and invites the student to continue', async () => {
    list.mockResolvedValueOnce([APP({ currentStatus: 'DRAFT', submittedAt: null, currentStageName: null })])
    const w = await mountList()
    expect(w.findAll('a').some(a => a.attributes('href')?.endsWith('/apply/7'))).toBe(true)
    expect(w.text()).toContain('Continue')
  })

  it('shows an error with retry when loading fails', async () => {
    list.mockReset().mockRejectedValue(new Error('boom'))
    const w = await mountList()
    expect(w.text()).toContain('This section could not be loaded')
    list.mockResolvedValueOnce([APP()])
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('MBBS')
  })
})

describe('dashboard application detail', () => {
  it('shows only the student-safe timeline, dropping internal events', async () => {
    const w = await mountDetail()
    const timeline = w.find('[data-test="timeline"]').text()
    expect(timeline).toContain('Application started')
    expect(timeline).toContain('Application submitted')
    expect(timeline).toContain('Your application moved to the next stage')
    expect(timeline).not.toContain('CASE_ASSIGNED')
    expect(timeline).not.toContain('STAGE_CLAIM')
    expect(w.text()).toContain('Eligibility review')
    expect(w.text()).toContain('Fudan University')
  })

  it('shows a not-found message when the server refuses or does not know the application', async () => {
    get.mockReset().mockRejectedValue({ statusCode: 403 })
    const w = await mountDetail()
    expect(w.text()).toContain('We could not find this application')
  })

  it('shows a generic load error for a server failure', async () => {
    get.mockReset().mockRejectedValue({ statusCode: 500 })
    const w = await mountDetail()
    expect(w.text()).toContain('This section could not be loaded')
  })

  it('names the application by its reference, not by its database id', async () => {
    const w = await mountDetail()
    expect(w.find('[data-test="application-reference"]').text()).toBe('NAD20261023001')
    expect(w.text()).not.toContain('#7')
  })

  it('sends an unsubmitted application to the step-by-step flow instead of editing it here', async () => {
    get.mockResolvedValue(APP({ currentStatus: 'DRAFT', currentStageName: 'Draft', submittedAt: null, timeline: ['APPLICATION_CREATED'] }))
    await mountDetail()
    expect(navigateTo).toHaveBeenCalledWith('/apply/7', { replace: true })
  })

  it('offers no withdrawal: the application fee is not refundable', async () => {
    const w = await mountDetail()
    expect(w.find('[data-test="withdraw-start"]').exists()).toBe(false)
    expect(w.text()).not.toContain('Withdraw application')
    expect(w.text()).toContain('The application fee is not refundable.')
  })

  it('flags an application whose fee staff rejected, in the table and in a banner', async () => {
    list.mockResolvedValueOnce([APP({ paymentNeeded: true, actionNote: null })])
    const w = await mountList()
    expect(w.find('[data-test="payment-needed"]').text()).toBe('Payment needed')
    expect(w.find('[data-test="payment-banner"]').text()).toContain('has not received the fee')
  })
})
