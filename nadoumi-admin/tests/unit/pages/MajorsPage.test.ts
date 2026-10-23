import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { mountOpts } from '../../helpers'

const api = vi.hoisted(() => ({
  listMajors: vi.fn(), createMajor: vi.fn(), updateMajor: vi.fn(), deleteMajor: vi.fn(), mergeMajor: vi.fn(), bulkMajors: vi.fn(),
}))
vi.mock('@/api/major', async orig => ({ ...(await orig<typeof import('@/api/major')>()), ...api }))
vi.mock('@/stores/user', () => ({ useUserStore: () => ({ hasPerm: () => true }) }))

import MajorsPage from '@/views/majors/index.vue'

const page = (content: object[]) => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1 })

describe('Majors screen', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    Object.values(api).forEach(fn => fn.mockReset())
    api.listMajors.mockResolvedValue(page([
      { id: 1, name: 'Computer Science', nameCn: '计算机科学', discipline: 'ENGINEERING', programCount: 3 },
      { id: 2, name: 'Law', nameCn: null, discipline: null, programCount: 0 },
    ]))
  })

  it('lists the catalogue with how many programmes use each major', async () => {
    const w = mount(MajorsPage, mountOpts())
    await flushPromises()

    expect(w.text()).toContain('Computer Science')
    expect(w.text()).toContain('计算机科学')
    expect(w.text()).toContain('Engineering')
    expect(w.text()).toContain('Law')
  })

  it('blocks deleting a major that programmes use, and offers merge instead', async () => {
    const w = mount(MajorsPage, mountOpts())
    await flushPromises()

    const deletes = w.findAll('button').filter(b => b.text() === 'Delete')
    expect(deletes[0]!.attributes('disabled')).toBeDefined() // Computer Science: 3 programmes
    expect(deletes[1]!.attributes('disabled')).toBeUndefined() // Law: unused
    expect(w.findAll('button').some(b => b.text() === 'Merge')).toBe(true)
  })
})
