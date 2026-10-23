import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({ listMajors: vi.fn() }))
vi.mock('@/api/major', async orig => ({ ...(await orig<typeof import('@/api/major')>()), ...api }))

import AddMajorsDialog, { type NewMajorRow } from '@/views/programs/form/AddMajorsDialog.vue'

function mountDialog() {
  api.listMajors.mockResolvedValue({ content: [], page: 0, size: 30, totalElements: 0, totalPages: 0 })
  return mount(AddMajorsDialog, {
    props: { modelValue: false, programLevels: ['BACHELOR', 'MASTER'], departments: [] },
    attachTo: document.body,
    ...mountOpts(),
  })
}

describe('AddMajorsDialog', () => {
  it('adds each pasted major once per ticked level, with the Chinese name when given', async () => {
    const w = mountDialog()
    await w.setProps({ modelValue: true })
    await flushPromises()

    const textarea = document.body.querySelector('textarea') as HTMLTextAreaElement
    textarea.value = 'Computer Science | 计算机科学\nLaw'
    textarea.dispatchEvent(new Event('input'))
    await flushPromises()
    const add = [...document.body.querySelectorAll('button')].find(b => b.textContent?.trim() === 'Add') as HTMLButtonElement
    add.click()
    await flushPromises()

    const rows = w.emitted('add')![0]![0] as NewMajorRow[]
    expect(rows.map(r => [r.name, r.nameCn, r.level])).toEqual([
      ['Computer Science', '计算机科学', 'BACHELOR'],
      ['Computer Science', '计算机科学', 'MASTER'],
      ['Law', null, 'BACHELOR'],
      ['Law', null, 'MASTER'],
    ])
    expect(rows.every(r => r.majorId === null)).toBe(true) // typed names are matched to the catalogue on save
    w.unmount()
  })
})
