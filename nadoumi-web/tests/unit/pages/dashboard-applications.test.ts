import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mockNuxtImport, mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ApplicationsList from '~/pages/dashboard/applications/index.vue'
import ApplicationDetail from '~/pages/dashboard/applications/[id].vue'
import type { StudentApplicationDto } from '~/types/catalog'

const APP = (over: Partial<StudentApplicationDto> = {}): StudentApplicationDto => ({
  id: 7, applicationType: 'PROGRAM_WITH_SCHOLARSHIP', programId: 1, scholarshipId: 2, intakeId: 3,
  currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW', submittedAt: '2026-01-15T10:00:00',
  timeline: ['APPLICATION_CREATED', 'STAGE_SUBMIT', 'CASE_ASSIGNED', 'STAGE_CLAIM'], ...over,
})

const list = vi.fn()
const get = vi.fn()
const submit = vi.fn()
const withdraw = vi.fn()
mockNuxtImport('useRoute', () => () => ({ params: { id: '7' }, path: '/dashboard/applications/7' }))
vi.mock('~/composables/useApplications', () => ({
  useApplications: () => ({ list, get, submit, withdraw }),
}))

beforeEach(() => {
  list.mockReset().mockResolvedValue([APP()])
  get.mockReset().mockResolvedValue(APP())
  submit.mockReset()
  withdraw.mockReset()
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

  it('lists real applications with a translated status and a link to the detail page', async () => {
    const w = await mountList()
    expect(w.text()).toContain('Programme with scholarship')
    expect(w.text()).toContain('Eligibility review')
    expect(w.text()).toContain('In review')
    expect(w.findAll('a').some(a => a.attributes('href')?.endsWith('/dashboard/applications/7'))).toBe(true)
  })

  it('shows an error with retry when loading fails', async () => {
    list.mockReset().mockRejectedValue(new Error('boom'))
    const w = await mountList()
    expect(w.text()).toContain('This section could not be loaded')
    list.mockResolvedValueOnce([APP()])
    await w.findAll('button').find(b => b.text() === 'Try again')!.trigger('click')
    await flushPromises()
    expect(w.text()).toContain('Programme with scholarship')
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
    expect(w.text()).toContain('Current stage: Eligibility review')
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

  it('submits a draft and shows the returned application', async () => {
    get.mockResolvedValue(APP({ currentStatus: 'DRAFT', currentStageName: 'Draft', submittedAt: null, timeline: ['APPLICATION_CREATED'] }))
    submit.mockResolvedValue(APP())
    const w = await mountDetail()
    await w.find('[data-test="submit"]').trigger('click')
    await flushPromises()
    expect(submit).toHaveBeenCalledWith(7)
    expect(w.text()).toContain('Your application has been submitted')
    expect(w.text()).toContain('In review')
  })

  it('requires a reason before withdrawing, then withdraws', async () => {
    withdraw.mockResolvedValue(APP({ currentStatus: 'CLOSED_WITHDRAWN', currentStageName: 'Withdrawn' }))
    const w = await mountDetail()
    await w.find('[data-test="withdraw-start"]').trigger('click')

    await w.find('[data-test="withdraw-confirm"]').trigger('click')
    await flushPromises()
    expect(withdraw).not.toHaveBeenCalled()

    await w.find('#withdraw-reason').setValue('Changed plans')
    await w.find('[data-test="withdraw-confirm"]').trigger('click')
    await flushPromises()
    expect(withdraw).toHaveBeenCalledWith(7, 'Changed plans')
    expect(w.text()).toContain('Your application has been withdrawn')
    expect(w.find('[data-test="withdraw-start"]').exists()).toBe(false)
  })

  it('shows the server error when an action is refused', async () => {
    get.mockResolvedValue(APP({ currentStatus: 'DRAFT', submittedAt: null }))
    submit.mockRejectedValue({ statusCode: 400, data: { detail: 'program_id is required to submit' } })
    const w = await mountDetail()
    await w.find('[data-test="submit"]').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('program_id is required to submit')
  })
})
