import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listInterviews: vi.fn(),
  scheduleInterview: vi.fn(),
  cancelInterview: vi.fn(),
  listNextSteps: vi.fn(),
}))
vi.mock('@/api/application', () => api)
vi.mock('element-plus', async (importOriginal) => ({
  ...(await importOriginal<typeof import('element-plus')>()),
  ElMessage: { success: vi.fn(), error: vi.fn() },
}))

import InterviewPanel from '@/views/applications/detail/InterviewPanel.vue'
import StatusDialog from '@/views/applications/detail/StatusDialog.vue'
import type { Interview } from '@/api/application'

const SCHEDULED: Interview = {
  id: 5, startsAt: '2030-11-02T07:00:00Z', durationMinutes: 30, platform: 'Zoom', meetingUrl: 'https://meet.example/abc',
  instructions: 'Bring your passport.', status: 'SCHEDULED',
}

function mountPanel(canSchedule = true) {
  return mount(InterviewPanel, { ...mountOpts(), props: { applicationId: 7, canSchedule }, attachTo: document.body })
}

beforeEach(() => {
  Object.values(api).forEach(fn => fn.mockReset())
  api.listInterviews.mockResolvedValue([])
  api.scheduleInterview.mockResolvedValue(SCHEDULED)
  api.cancelInterview.mockResolvedValue(undefined)
})

describe('InterviewPanel', () => {
  it('shows the scheduled interview in Beijing time and cancels it', async () => {
    api.listInterviews.mockResolvedValue([SCHEDULED])
    const w = mountPanel()
    await flushPromises()
    const card = w.get('[data-test="interview-scheduled"]')
    expect(card.text()).toContain('15:00')
    expect(card.text()).toContain('Beijing time (UTC+8)')
    expect(card.text()).toContain('https://meet.example/abc')

    await w.get('[data-test="interview-cancel"]').trigger('click')
    await flushPromises()
    expect(api.cancelInterview).toHaveBeenCalledWith(7, 5)
    w.unmount()
  })

  it('refuses a link that is not https, and sends nothing', async () => {
    const w = mountPanel()
    await flushPromises()
    const vm = w.vm as unknown as { $: { setupState: { form: { wall: string, meetingUrl: string } } } }
    vm.$.setupState.form.wall = '2099-01-01T10:00'
    vm.$.setupState.form.meetingUrl = 'http://meet.example/abc'
    await w.get('form').trigger('submit')
    await flushPromises()
    expect(w.get('[data-test="interview-error"]').text()).toContain('https')
    expect(api.scheduleInterview).not.toHaveBeenCalled()
    w.unmount()
  })

  it('refuses a time that is already past in Beijing', async () => {
    const w = mountPanel()
    await flushPromises()
    const vm = w.vm as unknown as { $: { setupState: { form: { wall: string, meetingUrl: string } } } }
    vm.$.setupState.form.wall = '2020-01-01T10:00'
    vm.$.setupState.form.meetingUrl = 'https://meet.example/abc'
    await w.get('form').trigger('submit')
    await flushPromises()
    expect(w.get('[data-test="interview-error"]').text()).toContain('future')
    expect(api.scheduleInterview).not.toHaveBeenCalled()
    w.unmount()
  })

  it('sends the Beijing wall time with its +08:00 offset, whatever the browser zone is', async () => {
    const w = mountPanel()
    await flushPromises()
    const vm = w.vm as unknown as { $: { setupState: { form: { wall: string, meetingUrl: string, platform: string } } } }
    vm.$.setupState.form.wall = '2099-01-01T10:00'
    vm.$.setupState.form.meetingUrl = ' https://meet.example/abc '
    vm.$.setupState.form.platform = 'Zoom'
    await w.get('form').trigger('submit')
    await flushPromises()
    expect(api.scheduleInterview).toHaveBeenCalledWith(7, expect.objectContaining({
      startsAt: '2099-01-01T10:00:00+08:00', durationMinutes: 30, platform: 'Zoom', meetingUrl: 'https://meet.example/abc',
    }))
    w.unmount()
  })

  it('offers no form to someone who cannot change the case', async () => {
    const w = mountPanel(false)
    await flushPromises()
    expect(w.find('form').exists()).toBe(false)
    w.unmount()
  })
})

describe('StatusDialog note rule', () => {
  const STEPS = [
    { code: 'request_documents', label: 'Documents required', status: 'DOCUMENTS_REQUIRED', decision: 'Documents required', blockedReason: null },
    { code: 'mark_ready', label: 'Ready for university submission', status: 'READY', decision: 'Eligible', blockedReason: null },
  ]

  it('will not send "Documents required" without a note for the student, but sends other steps without one', async () => {
    api.listNextSteps.mockResolvedValue(STEPS)
    const w = mount(StatusDialog, { ...mountOpts(), props: { modelValue: false, saving: false, applicationId: 7 }, attachTo: document.body })
    await w.setProps({ modelValue: true })
    await flushPromises()

    const submit = () => document.querySelector('[data-test="status-submit"]') as HTMLButtonElement
    // the first available step is preselected: it needs a note
    expect(submit().disabled).toBe(true)

    const note = document.querySelector('[data-test="status-note"]') as HTMLTextAreaElement
    note.value = 'The passport scan is blurred'
    note.dispatchEvent(new Event('input'))
    await flushPromises()
    expect(submit().disabled).toBe(false)
    submit().click()
    expect(w.emitted('submit')?.[0]).toEqual([{ code: 'request_documents', note: 'The passport scan is blurred' }])

    ;(document.querySelector('[data-test="step-mark_ready"] input') as HTMLInputElement).click()
    note.value = ''
    note.dispatchEvent(new Event('input'))
    await flushPromises()
    expect(submit().disabled).toBe(false)
    w.unmount()
  })
})
