import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listApplications: vi.fn(),
  getApplication: vi.fn(),
  listNextSteps: vi.fn(),
  advanceApplication: vi.fn(),
  listAssignees: vi.fn(),
  fetchApplicationForm: vi.fn(),
  completeApplicationTask: vi.fn(),
  skipApplicationTask: vi.fn(),
  assignApplication: vi.fn(),
  claimApplication: vi.fn(),
}))
vi.mock('@/api/application', () => api)

const router = vi.hoisted(() => ({ push: vi.fn() }))
vi.mock('vue-router', async (importOriginal) => ({
  ...(await importOriginal<typeof import('vue-router')>()),
  useRoute: () => ({ params: { id: '7' } }),
  useRouter: () => router,
}))

import Applications from '@/views/applications/index.vue'
import ApplicationDetailPage from '@/views/applications/detail.vue'
import AssignDialog from '@/views/applications/detail/AssignDialog.vue'
import StatusDialog from '@/views/applications/detail/StatusDialog.vue'
import { useUserStore } from '@/stores/user'
import type { Application, ApplicationDetail, ApplicationRow } from '@/api/application'

const APP: Application = {
  id: 7, reference: 'NAD20261024001', applicantId: 3, applicationType: 'PROGRAM_WITH_SCHOLARSHIP', programId: 1, scholarshipId: 2,
  intakeId: 4, currentStageCode: 'ELIGIBILITY_REVIEW', currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW',
  assigneeUserId: null, submittedAt: '2026-01-15T10:00:00', version: 5, createdAt: '2026-01-10T09:00:00',
}
const ROW: ApplicationRow = {
  id: 7, reference: 'NAD20261024001', applicantId: 3, applicantName: 'Amina Diallo', applicationType: 'PROGRAM_WITH_SCHOLARSHIP',
  opportunityTitle: 'MBBS', universityName: 'Fudan University', intake: 'Autumn', currentStageName: 'Eligibility review',
  currentStatus: 'IN_REVIEW', assigneeUserId: null, assigneeName: null, submittedAt: '2026-01-15T10:00:00',
}
const page = { content: [ROW], page: 0, size: 20, totalElements: 1, totalPages: 1 }

const detail = (over: Partial<ApplicationDetail> = {}): ApplicationDetail => ({
  application: APP,
  tasks: [
    { id: 11, title: 'Verify eligibility', roleRequired: null, mandatory: true, blocksExit: true, status: 'OPEN', assigneeUserId: null, dueAt: null, skipReason: null },
    { id: 12, title: 'Optional check', roleRequired: null, mandatory: false, blocksExit: false, status: 'OPEN', assigneeUserId: null, dueAt: null, skipReason: null },
  ],
  history: [],
  events: [{ id: 1, eventType: 'APPLICATION_CREATED', actorUserId: 9, at: '2026-01-10T09:00:00', detailJson: null, actorName: 'Amina Diallo', detailText: null }],
  decisions: [],
  applicant: { id: 3, publicId: '11111111-1111-1111-1111-111111111111', name: 'Amina Diallo', email: 'amina@example.test', phone: '+224600000000', nationality: 'GN' },
  opportunity: { kind: 'PROGRAM', title: 'MBBS', universityName: 'Fudan University', intake: 'Autumn' },
  assigneeName: null,
  ...over,
})

const mountList = () => mount(Applications, mountOpts())
const mountDetail = () => mount(ApplicationDetailPage, { ...mountOpts(), attachTo: document.body })

beforeEach(() => {
  document.body.innerHTML = ''
  setActivePinia(createPinia())
  useUserStore().permissions = ['*:*:*']
  Object.values(api).forEach(fn => fn.mockReset())
  router.push.mockReset()
  api.listApplications.mockResolvedValue(page)
  api.getApplication.mockResolvedValue(detail())
  api.listAssignees.mockResolvedValue([{ userId: 9, name: 'Case Officer', userName: 'officer' }])
  api.listNextSteps.mockResolvedValue([
    { code: 'eligible', label: 'Document collection', status: 'IN_REVIEW', decision: 'Eligible', blockedReason: null },
    { code: 'package_ready', label: 'Package preparation', status: 'IN_REVIEW', decision: null, blockedReason: 'Finish the required tasks of this stage first' },
  ])
  api.advanceApplication.mockResolvedValue(APP)
  api.fetchApplicationForm.mockRejectedValue(new Error('no pdf in tests'))
  api.completeApplicationTask.mockResolvedValue(undefined)
  api.skipApplicationTask.mockResolvedValue(undefined)
  api.claimApplication.mockResolvedValue(undefined)
  api.assignApplication.mockResolvedValue(undefined)
})

describe('Applications list', () => {
  it('shows names, the programme with its university and a translated status, not ids', async () => {
    const w = mountList()
    await flushPromises()
    expect(api.listApplications).toHaveBeenCalledWith({
      q: undefined, applicationType: undefined, status: undefined, assigneeUserId: undefined, page: 0, size: 20,
    })
    expect(w.text()).toContain('Amina Diallo')
    expect(w.text()).toContain('MBBS')
    expect(w.text()).toContain('Fudan University')
    expect(w.text()).toContain('Eligibility review')
    expect(w.text()).toContain('In review')
    expect(w.text()).toContain('Unassigned')
  })

  it('never offers draft or withdrawn as a filter: staff only receive submitted applications', async () => {
    const w = mountList()
    await flushPromises()
    expect(w.html()).not.toContain('Withdrawn')
    expect(w.html()).not.toContain('>Draft<')
  })

  it('re-queries from the first page with the chosen filters', async () => {
    const w = mountList()
    await flushPromises()
    const vm = w.vm as unknown as { filters: { status: string }, page: number, reload: () => void }
    vm.filters.status = 'DECISION'
    vm.page = 3
    vm.reload()
    await flushPromises()
    expect(vm.page).toBe(0)
    expect(api.listApplications).toHaveBeenLastCalledWith(expect.objectContaining({ status: 'DECISION', page: 0 }))
  })

  it('shows an error state with retry when the list fails', async () => {
    api.listApplications.mockReset().mockRejectedValue(new Error('boom'))
    const w = mountList()
    await flushPromises()
    expect(w.text()).toContain('boom')
  })
})

describe('Application page', () => {
  it('shows who applied, what for and who handles it, with names instead of ids', async () => {
    api.getApplication.mockResolvedValue(detail({ assigneeName: 'Case Officer', application: { ...APP, assigneeUserId: 9 } }))
    const w = mountDetail()
    await flushPromises()
    expect(api.getApplication).toHaveBeenCalledWith(7)
    expect(document.body.textContent).toContain('NAD20261024001')
    expect(document.body.textContent).toContain('Amina Diallo')
    expect(document.body.textContent).toContain('Fudan University')
    expect(document.body.textContent).toContain('Case Officer')
    w.unmount()
  })

  it('hides every action the caller has no permission for', async () => {
    useUserStore().permissions = ['nad:application:view']
    const w = mountDetail()
    await flushPromises()
    for (const t of ['claim', 'assign', 'change-status']) {
      expect(document.querySelector(`[data-test="${t}"]`), t).toBeNull()
    }
    w.unmount()
  })

  it('only offers claim while the case is unassigned', async () => {
    api.getApplication.mockResolvedValue(detail({ application: { ...APP, assigneeUserId: 9 }, assigneeName: 'Case Officer' }))
    const w = mountDetail()
    await flushPromises()
    expect(document.querySelector('[data-test="claim"]')).toBeNull()
    expect(document.querySelector('[data-test="assign"]')).not.toBeNull()
    w.unmount()
  })

  it('claims the case and reloads', async () => {
    const w = mountDetail()
    await flushPromises()
    ;(document.querySelector('[data-test="claim"]') as HTMLElement).click()
    await flushPromises()
    expect(api.claimApplication).toHaveBeenCalledWith(7)
    expect(api.getApplication).toHaveBeenCalledTimes(2)
    w.unmount()
  })

  it('assigns to a staff member picked from the staff list, never a typed id', async () => {
    const w = mountDetail()
    await flushPromises()
    ;(document.querySelector('[data-test="assign"]') as HTMLElement).click()
    await flushPromises()
    expect(api.listAssignees).toHaveBeenCalled()
    w.findComponent(AssignDialog).vm.$emit('submit', 9)
    await flushPromises()
    expect(api.assignApplication).toHaveBeenCalledWith(7, 9)
    w.unmount()
  })

  it('changes the status from the offered steps, sending the version it read', async () => {
    const w = mountDetail()
    await flushPromises()
    ;(document.querySelector('[data-test="change-status"]') as HTMLElement).click()
    await flushPromises()
    expect(api.listNextSteps).toHaveBeenCalledWith(7)
    expect(document.body.textContent).toContain('Document collection')
    expect(document.body.textContent).toContain('Records the decision: Eligible')
    expect(document.body.textContent).toContain('Finish the required tasks of this stage first')
    w.findComponent(StatusDialog).vm.$emit('submit', { code: 'eligible', note: 'Looks complete' })
    await flushPromises()
    expect(api.advanceApplication).toHaveBeenCalledWith(7, { code: 'eligible', note: 'Looks complete', version: 5 })
    w.unmount()
  })

  it('offers no status change once the application is closed', async () => {
    api.getApplication.mockResolvedValue(detail({ application: { ...APP, currentStatus: 'CLOSED_SUCCESS' } }))
    const w = mountDetail()
    await flushPromises()
    expect(document.querySelector('[data-test="change-status"]')).toBeNull()
    w.unmount()
  })

  it('completes a stage task, and only offers skip for non-mandatory ones', async () => {
    const w = mountDetail()
    await flushPromises()
    const tab = [...document.querySelectorAll('[role="tab"]')].find(el => el.textContent?.includes('Stage checklist')) as HTMLElement
    tab.click()
    await flushPromises()
    expect(document.querySelectorAll('[data-test="task-skip"]').length).toBe(1)
    ;(document.querySelector('[data-test="task-complete"]') as HTMLElement).click()
    await flushPromises()
    expect(api.completeApplicationTask).toHaveBeenCalledWith(7, 11)
    w.unmount()
  })

  it('shows an error with retry when the detail fails to load', async () => {
    api.getApplication.mockReset().mockRejectedValueOnce(new Error('no access'))
    const w = mountDetail()
    await flushPromises()
    expect(document.body.textContent).toContain('no access')
    w.unmount()
  })
})
