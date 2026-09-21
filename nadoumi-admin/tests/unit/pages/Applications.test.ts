import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listApplications: vi.fn(),
  getApplication: vi.fn(),
  transitionApplication: vi.fn(),
  decideApplication: vi.fn(),
  completeApplicationTask: vi.fn(),
  skipApplicationTask: vi.fn(),
  assignApplication: vi.fn(),
  claimApplication: vi.fn(),
}))
vi.mock('@/api/application', () => api)

import Applications from '@/views/applications/index.vue'
import ApplicationDrawer from '@/views/applications/ApplicationDrawer.vue'
import { useUserStore } from '@/stores/user'
import type { Application, ApplicationDetail } from '@/api/application'

const APP: Application = {
  id: 7, applicantId: 3, applicationType: 'PROGRAM_WITH_SCHOLARSHIP', programId: 1, scholarshipId: 2, intakeId: 4,
  currentStageCode: 'ELIGIBILITY_REVIEW', currentStageName: 'Eligibility review', currentStatus: 'IN_REVIEW',
  assigneeUserId: null, submittedAt: '2026-01-15T10:00:00', version: 5, createdAt: '2026-01-10T09:00:00',
}
const page = { content: [APP], page: 0, size: 20, totalElements: 1, totalPages: 1 }

const detail = (over: Partial<ApplicationDetail> = {}): ApplicationDetail => ({
  application: APP,
  tasks: [
    { id: 11, title: 'Verify eligibility', roleRequired: null, mandatory: true, blocksExit: true, status: 'OPEN', assigneeUserId: null, dueAt: null, skipReason: null },
    { id: 12, title: 'Optional check', roleRequired: null, mandatory: false, blocksExit: false, status: 'OPEN', assigneeUserId: null, dueAt: null, skipReason: null },
  ],
  history: [{ id: 1, fromStageId: 1, toStageId: 2, transitionCode: 'submit', changedBy: 9, changedAt: '2026-01-15T10:00:00', reason: null }],
  events: [{ id: 1, eventType: 'APPLICATION_CREATED', actorUserId: 9, at: '2026-01-10T09:00:00', detailJson: null }],
  decisions: [],
  ...over,
})

function mountList() {
  return mount(Applications, mountOpts())
}
function mountDrawer() {
  return mount(ApplicationDrawer, { ...mountOpts(), props: { modelValue: true, applicationId: 7 }, attachTo: document.body })
}

beforeEach(() => {
  document.body.innerHTML = ''
  setActivePinia(createPinia())
  useUserStore().permissions = ['*:*:*']
  Object.values(api).forEach(fn => fn.mockReset())
  api.listApplications.mockResolvedValue(page)
  api.getApplication.mockResolvedValue(detail())
  api.transitionApplication.mockResolvedValue(APP)
  api.decideApplication.mockResolvedValue({})
  api.completeApplicationTask.mockResolvedValue(undefined)
  api.skipApplicationTask.mockResolvedValue(undefined)
  api.claimApplication.mockResolvedValue(undefined)
  api.assignApplication.mockResolvedValue(undefined)
})

describe('Applications list', () => {
  it('loads on mount and shows real rows with a translated status', async () => {
    const w = mountList()
    await flushPromises()
    expect(api.listApplications).toHaveBeenCalledWith({
      q: undefined, applicationType: undefined, status: undefined, assigneeUserId: undefined, page: 0, size: 20,
    })
    expect(w.text()).toContain('Eligibility review')
    expect(w.text()).toContain('In review')
    expect(w.text()).toContain('Programme with scholarship')
    expect(w.text()).toContain('Unassigned')
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

describe('Application drawer', () => {
  it('shows tasks, history, decisions and events from the real detail', async () => {
    const w = mountDrawer()
    await flushPromises()
    expect(api.getApplication).toHaveBeenCalledWith(7)
    expect(document.body.textContent).toContain('Verify eligibility')
    expect(document.body.textContent).toContain('Eligibility review')
    w.unmount()
  })

  it('hides every action the caller has no permission for', async () => {
    useUserStore().permissions = ['nad:application:view']
    const w = mountDrawer()
    await flushPromises()
    for (const t of ['claim', 'assign', 'transition', 'decide', 'task-complete']) {
      expect(document.querySelector(`[data-test="${t}"]`), t).toBeNull()
    }
    w.unmount()
  })

  it('only offers claim while the case is unassigned', async () => {
    api.getApplication.mockResolvedValue(detail({ application: { ...APP, assigneeUserId: 9 } }))
    const w = mountDrawer()
    await flushPromises()
    expect(document.querySelector('[data-test="claim"]')).toBeNull()
    expect(document.querySelector('[data-test="assign"]')).not.toBeNull()
    w.unmount()
  })

  it('claims the case and reloads', async () => {
    const w = mountDrawer()
    await flushPromises()
    ;(document.querySelector('[data-test="claim"]') as HTMLElement).click()
    await flushPromises()
    expect(api.claimApplication).toHaveBeenCalledWith(7)
    expect(api.getApplication).toHaveBeenCalledTimes(2)
    expect(w.emitted('changed')).toBeTruthy()
    w.unmount()
  })

  it('sends the transition code together with the version it read', async () => {
    const w = mountDrawer()
    await flushPromises()
    ;(document.querySelector('[data-test="transition"]') as HTMLElement).click()
    await flushPromises()
    const input = document.querySelector('[data-test="transition-code"]') as HTMLInputElement
    input.value = 'eligible'
    input.dispatchEvent(new Event('input'))
    await flushPromises()
    ;(document.querySelector('[data-test="transition-submit"]') as HTMLElement).click()
    await flushPromises()
    expect(api.transitionApplication).toHaveBeenCalledWith(7, 'eligible', { reason: undefined, version: 5 })
    w.unmount()
  })

  it('does not call the API for a transition without a code', async () => {
    const w = mountDrawer()
    await flushPromises()
    ;(document.querySelector('[data-test="transition"]') as HTMLElement).click()
    await flushPromises()
    ;(document.querySelector('[data-test="transition-submit"]') as HTMLElement).click()
    await flushPromises()
    await flushPromises()
    expect(api.transitionApplication).not.toHaveBeenCalled()
    expect(document.querySelector('.el-form-item.is-error')).not.toBeNull()
    w.unmount()
  })

  it('requires type, outcome and rationale before recording a decision', async () => {
    const w = mountDrawer()
    await flushPromises()
    ;(document.querySelector('[data-test="decide"]') as HTMLElement).click()
    await flushPromises()
    ;(document.querySelector('[data-test="decision-submit"]') as HTMLElement).click()
    await flushPromises()
    expect(api.decideApplication).not.toHaveBeenCalled()

    for (const [sel, value] of [['decision-outcome', 'ELIGIBLE'], ['decision-rationale', 'Meets the entry requirements']] as const) {
      const el = document.querySelector(`[data-test="${sel}"]`) as HTMLInputElement
      el.value = value
      el.dispatchEvent(new Event('input'))
    }
    await flushPromises()
    ;(document.querySelector('[data-test="decision-submit"]') as HTMLElement).click()
    await flushPromises()
    expect(api.decideApplication).toHaveBeenCalledWith(7, {
      decisionType: 'NADOUMI_INTERNAL', outcome: 'ELIGIBLE', rationale: 'Meets the entry requirements',
    })
    w.unmount()
  })

  it('completes a task, and only offers skip for non-mandatory ones', async () => {
    const w = mountDrawer()
    await flushPromises()
    expect(document.querySelectorAll('[data-test="task-skip"]').length).toBe(1)
    ;(document.querySelector('[data-test="task-complete"]') as HTMLElement).click()
    await flushPromises()
    expect(api.completeApplicationTask).toHaveBeenCalledWith(7, 11)
    w.unmount()
  })

  it('offers no stage or decision actions once the application is closed', async () => {
    api.getApplication.mockResolvedValue(detail({ application: { ...APP, currentStatus: 'CLOSED_WITHDRAWN' } }))
    const w = mountDrawer()
    await flushPromises()
    expect(document.querySelector('[data-test="transition"]')).toBeNull()
    expect(document.querySelector('[data-test="decide"]')).toBeNull()
    w.unmount()
  })

  it('shows an error with retry when the detail fails to load', async () => {
    api.getApplication.mockReset().mockRejectedValueOnce(new Error('no access'))
    const w = mountDrawer()
    await flushPromises()
    expect(document.body.textContent).toContain('no access')
    w.unmount()
  })
})
