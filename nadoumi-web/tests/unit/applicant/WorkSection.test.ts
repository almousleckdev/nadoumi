import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import WorkSection from '~/components/applicant/WorkSection.vue'
import { pickCombobox } from '../helpers/combobox'

const { api } = vi.hoisted(() => ({
  api: {
    listWork: vi.fn(), addWork: vi.fn(), updateWork: vi.fn(), deleteWork: vi.fn(),
  },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const row = {
  id: 1, employer: 'Acme Corp', jobTitle: 'Engineer', employmentType: 'FULL_TIME', country: 'US', city: 'Austin',
  startDate: '2021-01-01', endDate: null, current: true, description: null, workVisaType: null, workVisaExpiry: null,
}

async function mountSection() {
  const w = await mountSuspended(WorkSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.listWork.mockReset().mockResolvedValue([row])
  api.addWork.mockReset().mockResolvedValue(row)
  api.updateWork.mockReset().mockResolvedValue(row)
  api.deleteWork.mockReset().mockResolvedValue(undefined)
})

describe('WorkSection', () => {
  it('renders existing work experience', async () => {
    const w = await mountSection()
    expect(w.text()).toContain('Acme Corp')
  })

  it('adds a new record and reports the change', async () => {
    api.listWork.mockResolvedValueOnce([])
    const w = await mountSection()

    await w.find('[data-test="add"]').trigger('click')
    await w.find('#work-employer').setValue('Globex')
    await w.find('#work-title').setValue('Analyst')
    await pickCombobox(w, '#work-country', 'United Kingdom')
    await w.find('#work-start').setValue('2020-01-01')
    await w.find('#work-current').setValue(true)
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.addWork).toHaveBeenCalledWith(1, expect.objectContaining({ employer: 'Globex' }))
    expect(w.emitted('changed')).toBeTruthy()
  })

  it('removes a record after confirm', async () => {
    const w = await mountSection()

    await w.find('[data-test="remove-1"]').trigger('click')
    await w.vm.$nextTick()
    const confirm = document.querySelector<HTMLElement>('[data-test="confirm-remove"]')
    expect(confirm).not.toBeNull()
    confirm!.click()
    await flushPromises()

    expect(api.deleteWork).toHaveBeenCalledWith(1, 1)
    expect(w.emitted('changed')).toBeTruthy()
    w.unmount()
  })
})
