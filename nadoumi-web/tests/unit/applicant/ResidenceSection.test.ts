import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import { flushPromises } from '@vue/test-utils'
import ResidenceSection from '~/components/applicant/ResidenceSection.vue'
import { pickCombobox } from '../helpers/combobox'

const { api } = vi.hoisted(() => ({
  api: { getResidence: vi.fn(), saveResidence: vi.fn() },
}))
vi.mock('~/composables/useApplicant', () => ({ useApplicant: () => api }))

const record = {
  inChina: true, country: 'CN', city: 'Beijing', address: null,
  chinaEducationLevel: 'BACHELOR', chinaSchool: 'Tsinghua', visaType: 'X1', visaExpiryDate: '2027-01-01',
}

async function mountSection() {
  const w = await mountSuspended(ResidenceSection, { props: { applicantId: 1 } })
  await flushPromises()
  return w
}

beforeEach(() => {
  api.getResidence.mockReset().mockResolvedValue(null)
  api.saveResidence.mockReset().mockResolvedValue(record)
})

describe('ResidenceSection', () => {
  it('starts unanswered when nothing was saved yet', async () => {
    const w = await mountSection()
    expect(w.find('#loc-city').exists()).toBe(false)
  })

  it('prefills from the saved record', async () => {
    api.getResidence.mockResolvedValue(record)
    const w = await mountSection()
    expect((w.find('#loc-city').element as HTMLInputElement).value).toBe('Beijing')
  })

  it('saves "not in China" and reports the change', async () => {
    const w = await mountSection()

    const no = w.findAll('input[name="loc-in-china"]')[1]!
    ;(no.element as HTMLInputElement).checked = true
    await no.trigger('change')
    await pickCombobox(w, '#loc-country', 'United Kingdom')
    await w.find('#loc-city').setValue('London')
    await w.find('form').trigger('submit')
    await flushPromises()

    expect(api.saveResidence).toHaveBeenCalledWith(1, expect.objectContaining({ inChina: false, city: 'London' }))
    expect(w.emitted('changed')).toBeTruthy()
  })
})
