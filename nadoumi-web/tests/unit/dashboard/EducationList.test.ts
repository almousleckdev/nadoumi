import { describe, it, expect } from 'vitest'
import { mountSuspended } from '@nuxt/test-utils/runtime'
import EducationList from '~/components/dashboard/EducationList.vue'

const rows = [
  { id: 1, institution: 'MIT', level: 'BSc', field: 'CS', gpa: 3.9, gpaScale: 4, startDate: '2018-09-01', endDate: '2022-06-01' },
]

describe('EducationList', () => {
  it('renders existing rows', async () => {
    const w = await mountSuspended(EducationList, { props: { items: rows, busy: false } })
    expect(w.text()).toContain('MIT')
    expect(w.text()).toContain('CS')
  })

  it('emits add with the new-entry body', async () => {
    const w = await mountSuspended(EducationList, { props: { items: [], busy: false } })
    await w.find('[data-test="add"]').trigger('click')
    await w.find('#edu-institution').setValue('Oxford')
    await w.find('[data-test="save-new"]').trigger('click')
    expect(w.emitted('add')?.[0]?.[0]).toMatchObject({ institution: 'Oxford' })
  })

  it('emits remove after confirm', async () => {
    const w = await mountSuspended(EducationList, { props: { items: rows, busy: false } })
    await w.find('[data-test="remove-1"]').trigger('click')
    await w.vm.$nextTick()
    // NModal teleports its panel to <body>, so query the document, not the wrapper.
    const confirm = document.querySelector<HTMLElement>('[data-test="confirm-remove"]')
    expect(confirm).not.toBeNull()
    confirm!.click()
    await w.vm.$nextTick()
    expect(w.emitted('remove')?.[0]).toEqual([1])
    w.unmount()
  })
})
