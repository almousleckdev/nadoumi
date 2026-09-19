import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import EducationSection from '~/components/applicant/EducationSection.vue'
import { pickCombobox } from '../helpers/combobox'

const { api } = vi.hoisted(() => ({
  api: {
    listEducation: vi.fn(), addEducation: vi.fn(), updateEducation: vi.fn(), deleteEducation: vi.fn(),
  },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const row = {
  id: 1, institution: 'MIT', country: 'US', city: null, level: 'BACHELOR', qualification: null, field: 'CS',
  gpa: 3.9, gpaScale: 4, startDate: '2018-09-01', endDate: '2022-06-01', current: false,
}

async function mountSection() {
  const w = await mountSuspended(EducationSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.listEducation.mockReset().mockResolvedValue([row])
  api.addEducation.mockReset().mockResolvedValue(row)
  api.updateEducation.mockReset().mockResolvedValue(row)
  api.deleteEducation.mockReset().mockResolvedValue(undefined)
})

describe('EducationSection', () => {
  it('renders existing rows', async () => {
    const w = await mountSection()
    expect(w.text()).toContain('MIT')
  })

  it('adds a new record and reports the change', async () => {
    api.listEducation.mockResolvedValueOnce([])
    const w = await mountSection()

    await w.find('[data-test="add"]').trigger('click')
    await w.find('#edu-institution').setValue('Oxford')
    await w.find('#edu-level').setValue('BACHELOR')
    await pickCombobox(w, '#edu-country', 'United Kingdom')
    await w.find('#edu-start').setValue('2020-01-01')
    await w.find('#edu-end').setValue('2023-01-01')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.addEducation).toHaveBeenCalledWith(1, expect.objectContaining({ institution: 'Oxford' }))
    expect(w.emitted('changed')).toBeTruthy()
  })

  it('removes a record after confirm', async () => {
    const w = await mountSection()

    await w.find('[data-test="remove-1"]').trigger('click')
    await w.vm.$nextTick()
    // NModal teleports its panel to <body>, so query the document, not the wrapper.
    const confirm = document.querySelector<HTMLElement>('[data-test="confirm-remove"]')
    expect(confirm).not.toBeNull()
    confirm!.click()
    await flushPromises()

    expect(api.deleteEducation).toHaveBeenCalledWith(1, 1)
    expect(w.emitted('changed')).toBeTruthy()
    w.unmount()
  })
})
